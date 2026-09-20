package se.johannalynn.nexttram

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

class DashboardPersistenceTest {
    @get:Rule val compose = createComposeRule()
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val preferences = application.getSharedPreferences("dashboard-test", Context.MODE_PRIVATE)
    private val weatherRequests = AtomicInteger()
    private val service = WeatherService(HttpClient(MockEngine {
        weatherRequests.incrementAndGet()
        respond("Offline", HttpStatusCode.ServiceUnavailable)
    }))
    private val clock = MutableClock(Instant.parse("2026-09-20T10:30:00Z"))
    private val models = mutableListOf<DashboardViewModel>()
    private val settings = AutoUpdateSettings(enabled = true, startMinutes = 6 * 60, endMinutes = 9 * 60)

    @After
    fun cleanUp() {
        compose.runOnIdle { models.forEach { it.stop() } }
        service.close()
        preferences.edit().clear().commit()
    }

    @Test
    fun itemsAndChecksSurviveRecreationAndEditing() {
        compose.runOnIdle {
            preferences.edit().clear().commit()
            val model = model()
            model.saveRememberItem(null, "  Matlåda  ")
            model.saveRememberItem(null, "  ")
            val id = model.rememberList.value.items.single().id
            model.checkRememberItem(id, true)
            val restored = model()
            assertEquals("Matlåda", restored.rememberList.value.items.single().name)
            assertTrue(restored.rememberList.value.items.single().checked)
            restored.saveRememberItem(id, "Nycklar")
            assertTrue(restored.rememberList.value.items.single().checked)
            assertEquals("Nycklar", model().rememberList.value.items.single().name)
            restored.deleteRememberItem(id)
            assertTrue(model().rememberList.value.items.isEmpty())
        }
    }

    @Test
    fun openDashboardResetsAtMidnightWithoutLosingItems() {
        lateinit var model: DashboardViewModel
        compose.runOnIdle {
            preferences.edit().clear().commit()
            clock.current = Instant.parse("2026-09-20T21:59:59.800Z")
            // A fresh cache keeps this focused on the date timer, without a network request.
            preferences.edit().putString("weather", Json.encodeToString(
                WeatherCache(emptyList(), clock.millis(), clock.millis() + 86_400_000)
            )).commit()
            model = model()
            model.saveRememberItem(null, "Matlåda")
            model.checkRememberItem(model.rememberList.value.items.single().id, true)
            model.start(settings)
            clock.current = clock.current.plusSeconds(1)
        }
        compose.waitUntil(5_000) { !model.rememberList.value.items.single().checked }
        assertEquals("2026-09-21", model.rememberList.value.date)
        assertEquals("Matlåda", model.rememberList.value.items.single().name)
    }

    @Test
    fun reopeningAfterMidnightClearsChecks() {
        compose.runOnIdle {
            preferences.edit().clear().commit()
            val model = model()
            model.saveRememberItem(null, "Matlåda")
            model.checkRememberItem(model.rememberList.value.items.single().id, true)
            clock.current = clock.current.plusSeconds(86_400)
            assertFalse(model().rememberList.value.items.single().checked)
        }
    }

    @Test
    fun failureKeepsTodaysCachedForecastAndRememberList() {
        lateinit var model: DashboardViewModel
        compose.runOnIdle {
            preferences.edit().clear().putString("weather", Json.encodeToString(
                WeatherCache(listOf(WeatherHour(clock.millis(), 12.0, "rain", 0.5, 5.0, 10.0)), clock.millis(), 0)
            )).commit()
            model = model()
            model.saveRememberItem(null, "Matlåda")
            model.start(settings)
        }
        compose.waitUntil(5_000) { model.weather.value.refreshFailed }
        assertEquals(1, model.weather.value.periods.size)
        assertEquals("Matlåda", model.rememberList.value.items.single().name)
    }

    @Test
    fun failedHourlyChecksAllowImmediateManualAndResumeChecks() {
        lateinit var model: DashboardViewModel
        val disabled = settings.copy(enabled = false)
        compose.runOnIdle {
            preferences.edit().clear().commit()
            model = model()
            model.start(disabled)
        }
        compose.waitUntil(5_000) {
            weatherRequests.get() == 1 && model.weather.value.refreshFailed && !model.weather.value.isLoading
        }
        compose.runOnIdle { model.refreshWeather() }
        compose.waitUntil(5_000) {
            weatherRequests.get() == 2 && !model.weather.value.isLoading
        }
        compose.runOnIdle {
            model.stop()
            model.start(disabled)
        }
        compose.waitUntil(5_000) {
            weatherRequests.get() == 3 && !model.weather.value.isLoading
        }
    }

    private fun model() = DashboardViewModel(application, preferences, service, clock).also { models += it }

    private class MutableClock(var current: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("Europe/Stockholm")
        override fun withZone(zone: ZoneId): Clock = fixed(current, zone)
        override fun instant(): Instant = current
    }
}
