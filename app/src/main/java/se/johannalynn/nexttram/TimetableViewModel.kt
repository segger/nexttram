package se.johannalynn.nexttram

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TimetableUiState {
    data object Loading : TimetableUiState
    data class Error(val message: String) : TimetableUiState
    data class Success(val departures: List<Departure>, val platforms: List<String>) : TimetableUiState
}

sealed interface StationSearchUiState {
    data object Idle : StationSearchUiState
    data object Loading : StationSearchUiState
    data class Success(val stations: List<Station>) : StationSearchUiState
    data class Error(val message: String) : StationSearchUiState
}

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    private val service = TimetableService()
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _selectedStation = MutableStateFlow(
        Station(
            gid = preferences.getString(STATION_GID_KEY, null) ?: DEFAULT_STATION_GID,
            name = preferences.getString(STATION_NAME_KEY, null) ?: DEFAULT_STATION_NAME,
        )
    )
    val selectedStation: StateFlow<Station> = _selectedStation.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(
        preferences.getString(PLATFORM_KEY, "").orEmpty()
    )
    val selectedPlatform: StateFlow<String> = _selectedPlatform.asStateFlow()

    private val _stationQuery = MutableStateFlow("")
    val stationQuery: StateFlow<String> = _stationQuery.asStateFlow()

    private val _stationSearchState = MutableStateFlow<StationSearchUiState>(StationSearchUiState.Idle)
    val stationSearchState: StateFlow<StationSearchUiState> = _stationSearchState.asStateFlow()

    private var started = false
    private var departuresJob: Job? = null
    private var searchJob: Job? = null

    fun start() {
        if (started) return
        started = true
        fetchDepartures()
    }

    fun fetchDepartures() {
        departuresJob?.cancel()
        departuresJob = viewModelScope.launch {
            _uiState.value = TimetableUiState.Loading
            try {
                val result = service.getDepartures(_selectedStation.value.gid)
                if (result.platforms.isNotEmpty()) {
                    val platform = _selectedPlatform.value
                        .takeIf { it in result.platforms }
                        ?: result.platforms.first()
                    savePlatform(platform)
                }
                _uiState.value = TimetableUiState.Success(result.departures, result.platforms)
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                _uiState.value = TimetableUiState.Error("Failed to load: ${e.message}")
            }
        }
    }

    fun onStationQueryChanged(query: String) {
        _stationQuery.value = query
        searchJob?.cancel()

        val searchTerm = query.trim()
        if (searchTerm.length < MINIMUM_SEARCH_LENGTH) {
            _stationSearchState.value = StationSearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            _stationSearchState.value = StationSearchUiState.Loading
            try {
                _stationSearchState.value = StationSearchUiState.Success(
                    service.searchStations(searchTerm)
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _stationSearchState.value = StationSearchUiState.Error(
                    "Kunde inte söka: ${error.message}"
                )
            }
        }
    }

    fun selectStation(station: Station) {
        _selectedStation.value = station
        preferences.edit()
            .putString(STATION_GID_KEY, station.gid)
            .putString(STATION_NAME_KEY, station.name)
            .apply()
        _stationQuery.value = ""
        _stationSearchState.value = StationSearchUiState.Idle
        fetchDepartures()
    }

    fun selectPlatform(platform: String) {
        savePlatform(platform)
    }

    private fun savePlatform(platform: String) {
        _selectedPlatform.value = platform
        preferences.edit().putString(PLATFORM_KEY, platform).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "next_tram_preferences"
        const val STATION_GID_KEY = "station_gid"
        const val STATION_NAME_KEY = "station_name"
        const val PLATFORM_KEY = "platform"
        const val DEFAULT_STATION_GID = "9021014001200000"
        const val DEFAULT_STATION_NAME = "Axel Dahlströms Torg, Göteborg"
        const val MINIMUM_SEARCH_LENGTH = 3
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
