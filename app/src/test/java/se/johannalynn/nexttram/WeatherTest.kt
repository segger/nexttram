package se.johannalynn.nexttram

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeatherTest {
    private val noon = Instant.parse("2026-09-20T10:00:00Z") // Noon in Göteborg.

    @Test
    fun completedPeriodsAndOtherDatesAreExcluded() {
        val cache = cache(listOf(
            hour("2026-09-20T09:00:00Z"),
            hour("2026-09-20T10:00:00Z"),
            hour("2026-09-20T16:00:00Z"),
            hour("2026-09-20T22:00:00Z"),
        ))
        assertEquals(listOf(WeatherPeriod.AFTERNOON, WeatherPeriod.EVENING), cache.periodsAt(noon).map { it.period })
        assertEquals(listOf(WeatherPeriod.EVENING), cache.periodsAt(noon.plusSeconds(6 * 3600)).map { it.period })
        assertEquals(listOf(WeatherPeriod.MORNING), cache.periodsAt(noon.plusSeconds(12 * 3600)).map { it.period })
    }

    @Test
    fun currentHourRemainsVisibleAndRainIsNotDoubleCounted() {
        val cache = cache(listOf(hour("2026-09-20T10:00:00Z"), hour("2026-09-20T11:00:00Z")))
        val period = cache.periodsAt(noon.plusSeconds(1800)).single()
        assertEquals(2, period.hours.size)
        assertEquals(1.0, period.precipitation!!, 0.001)
    }

    @Test
    fun localDateUsesStockholmAcrossWinterAndSummerTime() {
        val cache = cache(listOf(hour("2026-10-25T23:00:00Z")))
        val period = cache.periodsAt(Instant.parse("2026-10-25T23:30:00Z")).single()
        assertEquals(WeatherPeriod.MORNING, period.period)
    }

    @Test
    fun missingPrecipitationIsNotShownAsZero() {
        val period = PeriodForecast(WeatherPeriod.AFTERNOON, listOf(hour(noon.toString()).copy(precipitation = null)))
        assertNull(period.precipitation)
    }

    @Test
    fun decoderUsesHourlyRainAndAllowsMissingGusts() {
        val forecast = decodeForecast(fixture)
        val hour = forecast.hours.single()
        assertEquals(0.5, hour.precipitation!!, 0.001)
        assertEquals("rain", hour.symbol)
        assertNull(hour.gust)
        assertEquals(12.0, hour.temperature, 0.001)
    }

    @Test
    fun freshCacheAvoidsRequests() = runTest {
        val engine = MockEngine { error("Fresh cache must not contact the server") }
        WeatherService(HttpClient(engine)).use { service ->
            val cached = cache(listOf(hour(noon.toString()))).copy(expiresAtMillis = noon.plusSeconds(600).toEpochMilli())
            assertSame(cached, service.getForecast(cached, noon))
        }
    }

    @Test
    fun notModifiedReusesForecastAndHonorsExpiryAndConditionalHeaders() = runTest {
        val modified = "Sun, 20 Sep 2026 09:00:00 GMT"
        val engine = MockEngine { request ->
            assertEquals(modified, request.headers[HttpHeaders.IfModifiedSince])
            assertTrue(request.headers[HttpHeaders.UserAgent]!!.startsWith("NextTram/"))
            assertEquals("57.7089", request.url.parameters["lat"])
            assertEquals("11.9746", request.url.parameters["lon"])
            respond("", HttpStatusCode.NotModified, headersOf(HttpHeaders.Expires, "Sun, 20 Sep 2026 11:00:00 GMT"))
        }
        WeatherService(HttpClient(engine)).use { service ->
            val cached = cache(listOf(hour(noon.toString()))).copy(lastModified = modified)
            val refreshed = service.getForecast(cached, noon)
            assertEquals(cached.hours, refreshed.hours)
            assertEquals(noon.plusSeconds(3600).toEpochMilli(), refreshed.expiresAtMillis)
        }
    }

    @Test
    fun successfulRequestDecodesAndStoresServerMetadata() = runTest {
        val engine = MockEngine {
            respond(fixture, HttpStatusCode.OK, headersOf(HttpHeaders.LastModified, "Sun, 20 Sep 2026 09:00:00 GMT"))
        }
        WeatherService(HttpClient(engine)).use { service ->
            val result = service.getForecast(null, noon)
            assertEquals(1, result.hours.size)
            assertNotNull(result.lastModified)
            assertTrue(result.expiresAtMillis > noon.toEpochMilli())
        }
    }

    private fun hour(time: String) = WeatherHour(Instant.parse(time).toEpochMilli(), 12.0, "rain", 0.5, 5.0, 10.0)
    private fun cache(hours: List<WeatherHour>) = WeatherCache(hours, noon.toEpochMilli(), 0)

    private val fixture = """
        {"properties":{"meta":{"updated_at":"2026-09-20T09:00:00Z"},"timeseries":[{
          "time":"2026-09-20T10:00:00Z","data":{
            "instant":{"details":{"air_temperature":12.0,"wind_speed":5.0}},
            "next_1_hours":{"summary":{"symbol_code":"rain"},"details":{"precipitation_amount":0.5}},
            "next_6_hours":{"details":{"precipitation_amount":8.0}}
          }}]}}
    """.trimIndent()
}
