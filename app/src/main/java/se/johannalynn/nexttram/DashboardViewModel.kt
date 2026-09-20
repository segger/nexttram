package se.johannalynn.nexttram

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class WeatherUiState(
    val periods: List<PeriodForecast> = emptyList(),
    val updatedAtMillis: Long? = null,
    val isLoading: Boolean = false,
    val refreshFailed: Boolean = false,
)

class DashboardViewModel internal constructor(
    application: Application,
    private val preferences: SharedPreferences,
    private val service: WeatherService,
    private val clock: Clock,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        application.getSharedPreferences("dashboard", Context.MODE_PRIVATE),
        WeatherService(),
        Clock.systemDefaultZone(),
    )
    private var cachedWeather = readSaved<WeatherCache>("weather")
    private val _weather = MutableStateFlow(WeatherUiState())
    val weather = _weather.asStateFlow()
    private val _rememberList = MutableStateFlow(
        (readSaved<RememberList>("remember_list") ?: RememberList(LocalDate.now(clock).toString()))
            .forDate(LocalDate.now(clock))
    )
    val rememberList = _rememberList.asStateFlow()
    private var ticker: Job? = null
    private var weatherJob: Job? = null
    private var lastWeatherCheck = ZonedDateTime.now(clock)

    fun start(settings: AutoUpdateSettings) {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            refreshWeather()
            while (true) {
                updateDateAndPeriods()
                val now = ZonedDateTime.now(clock)
                if (!now.isBefore(settings.nextUpdateAt(lastWeatherCheck))) {
                    refreshWeather()
                }
                val nextWeatherCheck = settings.nextUpdateAt(lastWeatherCheck).toInstant().toEpochMilli()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay(minOf(
                    Duration.between(now, midnight).toMillis(),
                    nextWeatherCheck - clock.millis(),
                    60_000L,
                ).coerceAtLeast(1L))
            }
        }
    }

    fun stop() {
        ticker?.cancel()
        ticker = null
        weatherJob?.cancel()
        weatherJob = null
        _weather.value = _weather.value.copy(isLoading = false)
    }

    fun refreshWeather() {
        lastWeatherCheck = ZonedDateTime.now(clock)
        if (weatherJob?.isActive == true) return
        if (cachedWeather?.expiresAtMillis?.let { it > clock.millis() } == true) return
        weatherJob = viewModelScope.launch {
            _weather.value = _weather.value.copy(isLoading = true)
            try {
                val result = service.getForecast(cachedWeather, clock.instant())
                cachedWeather = result
                preferences.edit { putString("weather", Json.encodeToString(result)) }
                _weather.value = WeatherUiState()
                updateDateAndPeriods()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                lastWeatherCheck = ZonedDateTime.now(clock)
                _weather.value = _weather.value.copy(isLoading = false, refreshFailed = true)
                updateDateAndPeriods()
            }
        }
    }

    fun saveRememberItem(id: String?, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateRememberList { list ->
            list.copy(items = if (id == null) {
                list.items + RememberItem(UUID.randomUUID().toString(), trimmed)
            } else {
                list.items.map { if (it.id == id) it.copy(name = trimmed) else it }
            })
        }
    }

    fun deleteRememberItem(id: String) {
        updateRememberList { it.copy(items = it.items.filterNot { item -> item.id == id }) }
    }

    fun checkRememberItem(id: String, checked: Boolean) {
        updateRememberList { it.copy(items = it.items.map { item ->
            if (item.id == id) item.copy(checked = checked) else item
        }) }
    }

    private fun updateRememberList(transform: (RememberList) -> RememberList) {
        val updated = transform(_rememberList.value.forDate(LocalDate.now(clock)))
        if (updated == _rememberList.value) return
        _rememberList.value = updated
        preferences.edit { putString("remember_list", Json.encodeToString(updated)) }
    }

    private fun updateDateAndPeriods() {
        updateRememberList { it }
        val periods = cachedWeather?.periodsAt(clock.instant()).orEmpty()
        _weather.value = _weather.value.copy(
            periods = periods,
            updatedAtMillis = cachedWeather?.updatedAtMillis?.takeIf { periods.isNotEmpty() },
        )
    }

    private inline fun <reified T> readSaved(key: String): T? = try {
        preferences.getString(key, null)?.let { Json.decodeFromString<T>(it) }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    override fun onCleared() {
        service.close()
    }
}
