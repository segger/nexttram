package se.johannalynn.nexttram

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRefreshTimingTest {
    @Test
    fun manualFailureRestartsHourlyWaitAndReopeningChecksImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        val preferences = application.getSharedPreferences("weather-timing-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        var requests = 0
        val service = WeatherService(HttpClient(MockEngine) {
            engine {
                dispatcher = testDispatcher
                addHandler {
                    requests++
                    respond("Offline", HttpStatusCode.ServiceUnavailable)
                }
            }
        })
        val clock = object : Clock() {
            override fun getZone(): ZoneId = ZoneId.of("Europe/Stockholm")
            override fun withZone(zone: ZoneId): Clock = fixed(instant(), zone)
            override fun instant(): Instant = Instant.parse("2026-09-20T10:00:00Z")
                .plusMillis(testScheduler.currentTime)
        }
        val model = DashboardViewModel(application, preferences, service, clock)
        val settings = AutoUpdateSettings(enabled = false, startMinutes = 6 * 60, endMinutes = 9 * 60)
        try {
            model.start(settings)
            runCurrent()
            assertEquals(1, requests)
            assertTrue(model.weather.value.refreshFailed)
            advanceTimeBy(59 * 60_000L)
            runCurrent()
            assertEquals(1, requests)

            model.refreshWeather()
            runCurrent()
            assertEquals(2, requests)
            advanceTimeBy(60_000L)
            runCurrent()
            assertEquals(2, requests)
            advanceTimeBy(59 * 60_000L)
            runCurrent()
            assertEquals(3, requests)

            model.stop()
            advanceTimeBy(60 * 60_000L)
            runCurrent()
            assertEquals(3, requests)
            model.start(settings)
            runCurrent()
            assertEquals(4, requests)
        } finally {
            model.stop()
            service.close()
            preferences.edit().clear().commit()
            Dispatchers.resetMain()
        }
    }
}
