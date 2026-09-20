package se.johannalynn.nexttram

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal val weatherZone: ZoneId = ZoneId.of("Europe/Stockholm")

enum class WeatherPeriod(val endHour: Int) { MORNING(12), AFTERNOON(18), EVENING(24) }

@Serializable
data class WeatherHour(
    val timeMillis: Long,
    val temperature: Double,
    val symbol: String?,
    val precipitation: Double?,
    val wind: Double,
    val gust: Double?,
)

@Serializable
data class WeatherCache(
    val hours: List<WeatherHour>,
    val updatedAtMillis: Long,
    val expiresAtMillis: Long,
    val lastModified: String? = null,
)

data class PeriodForecast(val period: WeatherPeriod, val hours: List<WeatherHour>) {
    val precipitation: Double? = if (hours.all { it.precipitation != null }) {
        hours.sumOf { it.precipitation!! }
    } else null
    val symbol: String? = hours.maxBy { it.precipitation ?: 0.0 }.symbol
}

internal fun WeatherCache.periodsAt(now: Instant): List<PeriodForecast> {
    val today = now.atZone(weatherZone).toLocalDate()
    val currentHour = now.truncatedTo(ChronoUnit.HOURS).toEpochMilli()
    return hours.filter {
        it.timeMillis >= currentHour &&
            Instant.ofEpochMilli(it.timeMillis).atZone(weatherZone).toLocalDate() == today
    }.groupBy {
        val hour = Instant.ofEpochMilli(it.timeMillis).atZone(weatherZone).hour
        WeatherPeriod.entries.first { period -> hour < period.endHour }
    }.map { (period, hours) -> PeriodForecast(period, hours) }
}

class WeatherService(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentEncoding) { gzip() }
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
    },
) : AutoCloseable {
    suspend fun getForecast(cached: WeatherCache?, now: Instant = Instant.now()): WeatherCache {
        if (cached != null && now.toEpochMilli() < cached.expiresAtMillis) return cached
        val response = client.get("https://api.met.no/weatherapi/locationforecast/2.0/complete") {
            parameter("lat", "57.7089")
            parameter("lon", "11.9746")
            header(HttpHeaders.UserAgent, "NextTram/${BuildConfig.VERSION_NAME} github.com/segger/nexttram")
            cached?.lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
        }
        val expires = response.headers[HttpHeaders.Expires]?.let(::parseHttpDate)
            ?: now.plusSeconds(30 * 60).toEpochMilli()
        if (response.status == HttpStatusCode.NotModified && cached != null) {
            return cached.copy(expiresAtMillis = expires)
        }
        check(response.status.value in 200..299) { "Weather request failed: ${response.status.value}" }
        return decodeForecast(response.bodyAsText()).copy(
            expiresAtMillis = expires,
            lastModified = response.headers[HttpHeaders.LastModified],
        )
    }

    override fun close() = client.close()
}

internal fun decodeForecast(body: String): WeatherCache {
    val properties = Json.parseToJsonElement(body).jsonObject.getValue("properties").jsonObject
    val updatedAt = properties.getValue("meta").jsonObject.getValue("updated_at").jsonPrimitive.content
    val hours = properties.getValue("timeseries").jsonArray.take(48).map { element ->
        val entry = element.jsonObject
        val data = entry.getValue("data").jsonObject
        val instant = data.getValue("instant").jsonObject.getValue("details").jsonObject
        val nextHour = data["next_1_hours"]?.jsonObject
        WeatherHour(
            timeMillis = Instant.parse(entry.getValue("time").jsonPrimitive.content).toEpochMilli(),
            temperature = instant.getValue("air_temperature").jsonPrimitive.content.toDouble(),
            symbol = nextHour?.get("summary")?.jsonObject?.get("symbol_code")?.jsonPrimitive?.content,
            precipitation = nextHour?.get("details")?.jsonObject?.get("precipitation_amount")?.jsonPrimitive?.doubleOrNull,
            wind = instant.getValue("wind_speed").jsonPrimitive.content.toDouble(),
            gust = instant["wind_speed_of_gust"]?.jsonPrimitive?.doubleOrNull,
        )
    }
    require(hours.isNotEmpty()) { "Empty forecast" }
    return WeatherCache(hours, Instant.parse(updatedAt).toEpochMilli(), expiresAtMillis = 0)
}

private fun parseHttpDate(value: String): Long? = try {
    ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
} catch (_: java.time.format.DateTimeParseException) {
    null
}
