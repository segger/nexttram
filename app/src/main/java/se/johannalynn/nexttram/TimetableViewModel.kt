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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.min

sealed interface TimetableUiState {
    data object Loading : TimetableUiState
    data class Error(val message: String) : TimetableUiState
    data class Success(
        val departures: List<Departure>,
        val platforms: List<String>,
        val lastUpdatedMillis: Long,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : TimetableUiState
}

data class AutoUpdateSettings(
    val enabled: Boolean,
    val startMinutes: Int,
    val endMinutes: Int,
)

internal fun AutoUpdateSettings.isActiveAt(time: LocalTime): Boolean {
    val currentMinutes = time.hour * MINUTES_PER_HOUR + time.minute
    return when {
        startMinutes == endMinutes -> true
        startMinutes < endMinutes -> currentMinutes in startMinutes..<endMinutes
        else -> currentMinutes >= startMinutes || currentMinutes < endMinutes
    }
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

    private val _autoUpdateSettings = MutableStateFlow(
        AutoUpdateSettings(
            enabled = preferences.getBoolean(AUTO_UPDATE_ENABLED_KEY, true),
            startMinutes = preferences.getInt(AUTO_UPDATE_START_KEY, DEFAULT_AUTO_UPDATE_START),
            endMinutes = preferences.getInt(AUTO_UPDATE_END_KEY, DEFAULT_AUTO_UPDATE_END),
        )
    )
    val autoUpdateSettings: StateFlow<AutoUpdateSettings> = _autoUpdateSettings.asStateFlow()

    private var started = false
    private var departuresJob: Job? = null
    private var searchJob: Job? = null
    private var autoUpdateJob: Job? = null
    private var nextAutoUpdateAtMillis: Long? = null

    fun start() {
        if (started) return
        started = true
        fetchDepartures()
    }

    fun fetchDepartures() {
        if (departuresJob?.isActive == true) return

        val requestStartedAtMillis = System.currentTimeMillis()
        nextAutoUpdateAtMillis = requestStartedAtMillis + AUTO_UPDATE_INTERVAL_MILLIS
        departuresJob = viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = when (currentState) {
                is TimetableUiState.Success -> currentState.copy(
                    isRefreshing = true,
                    refreshError = null,
                )
                is TimetableUiState.Error -> currentState
                TimetableUiState.Loading -> TimetableUiState.Loading
            }
            try {
                val result = service.getDepartures(_selectedStation.value.gid)
                if (result.platforms.isNotEmpty()) {
                    val platform = _selectedPlatform.value
                        .takeIf { it in result.platforms }
                        ?: result.platforms.first()
                    savePlatform(platform)
                }
                _uiState.value = TimetableUiState.Success(
                    departures = result.departures,
                    platforms = result.platforms,
                    lastUpdatedMillis = System.currentTimeMillis(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                val message = "Kunde inte uppdatera: ${e.message}"
                _uiState.value = when (val state = _uiState.value) {
                    is TimetableUiState.Success -> state.copy(
                        isRefreshing = false,
                        refreshError = message,
                    )
                    else -> TimetableUiState.Error(message)
                }
            }
        }
    }

    fun startAutoUpdate() {
        if (autoUpdateJob?.isActive == true) return
        autoUpdateJob = viewModelScope.launch {
            _autoUpdateSettings.collectLatest { settings ->
                if (!settings.enabled) return@collectLatest

                while (true) {
                    val now = ZonedDateTime.now()
                    if (!settings.isActiveAt(now.toLocalTime())) {
                        delay(settings.millisUntilNextStart(now).nextCheckDelay())
                        continue
                    }

                    val nowMillis = System.currentTimeMillis()
                    val nextUpdateAtMillis = nextAutoUpdateAtMillis
                    if (nextUpdateAtMillis == null || nextUpdateAtMillis <= nowMillis) {
                        if (departuresJob?.isActive == true) {
                            nextAutoUpdateAtMillis = nextAutoUpdateAtMillis
                                ?.nextIntervalAfter(nowMillis)
                                ?: nowMillis + AUTO_UPDATE_INTERVAL_MILLIS
                        } else {
                            fetchDepartures()
                        }
                        continue
                    }

                    val untilNextUpdate = nextUpdateAtMillis - nowMillis
                    val untilEnd = settings.millisUntilEnd(now)
                    delay(min(untilNextUpdate, untilEnd).nextCheckDelay())
                }
            }
        }
    }

    fun stopAutoUpdate() {
        autoUpdateJob?.cancel()
        autoUpdateJob = null
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        _autoUpdateSettings.value = _autoUpdateSettings.value.copy(enabled = enabled)
        preferences.edit().putBoolean(AUTO_UPDATE_ENABLED_KEY, enabled).apply()
    }

    fun setAutoUpdateStart(minutes: Int) {
        require(minutes in 0..<MINUTES_PER_DAY)
        _autoUpdateSettings.value = _autoUpdateSettings.value.copy(startMinutes = minutes)
        preferences.edit().putInt(AUTO_UPDATE_START_KEY, minutes).apply()
    }

    fun setAutoUpdateEnd(minutes: Int) {
        require(minutes in 0..<MINUTES_PER_DAY)
        _autoUpdateSettings.value = _autoUpdateSettings.value.copy(endMinutes = minutes)
        preferences.edit().putInt(AUTO_UPDATE_END_KEY, minutes).apply()
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
        departuresJob?.cancel()
        departuresJob = null
        _uiState.value = TimetableUiState.Loading
        fetchDepartures()
    }

    fun selectPlatform(platform: String) {
        savePlatform(platform)
    }

    private fun savePlatform(platform: String) {
        _selectedPlatform.value = platform
        preferences.edit().putString(PLATFORM_KEY, platform).apply()
    }

    private fun AutoUpdateSettings.millisUntilNextStart(now: ZonedDateTime): Long {
        val start = LocalTime.of(startMinutes / MINUTES_PER_HOUR, startMinutes % MINUTES_PER_HOUR)
        var nextStart = now.with(start).withSecond(0).withNano(0)
        if (!nextStart.isAfter(now)) nextStart = nextStart.plusDays(1)
        return Duration.between(now, nextStart).toMillis().coerceAtLeast(1L)
    }

    private fun AutoUpdateSettings.millisUntilEnd(now: ZonedDateTime): Long {
        if (startMinutes == endMinutes) return AUTO_UPDATE_INTERVAL_MILLIS

        val end = LocalTime.of(endMinutes / MINUTES_PER_HOUR, endMinutes % MINUTES_PER_HOUR)
        var nextEnd = now.with(end).withSecond(0).withNano(0)
        if (!nextEnd.isAfter(now)) nextEnd = nextEnd.plusDays(1)
        return Duration.between(now, nextEnd).toMillis().coerceAtLeast(1L)
    }

    private fun Long.nextIntervalAfter(nowMillis: Long): Long {
        val intervals = ((nowMillis - this) / AUTO_UPDATE_INTERVAL_MILLIS) + 1
        return this + intervals * AUTO_UPDATE_INTERVAL_MILLIS
    }

    private fun Long.nextCheckDelay(): Long = coerceIn(1L, AUTO_UPDATE_INTERVAL_MILLIS)

    private companion object {
        const val PREFERENCES_NAME = "next_tram_preferences"
        const val STATION_GID_KEY = "station_gid"
        const val STATION_NAME_KEY = "station_name"
        const val PLATFORM_KEY = "platform"
        const val AUTO_UPDATE_ENABLED_KEY = "auto_update_enabled"
        const val AUTO_UPDATE_START_KEY = "auto_update_start"
        const val AUTO_UPDATE_END_KEY = "auto_update_end"
        const val DEFAULT_STATION_GID = "9021014001200000"
        const val DEFAULT_STATION_NAME = "Axel Dahlströms Torg, Göteborg"
        const val DEFAULT_AUTO_UPDATE_START = 6 * 60
        const val DEFAULT_AUTO_UPDATE_END = 9 * 60
        const val AUTO_UPDATE_INTERVAL_MILLIS = 60_000L
        const val MINIMUM_SEARCH_LENGTH = 3
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
