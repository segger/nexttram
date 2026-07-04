package se.johannalynn.nexttram

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

@Serializable
data class Departure(
    val line: String,
    val destination: String,
    val next: String,
    val backgroundColor: String,
    val foregroundColor: String,
    val borderColor: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val scope: String? = null,
    val token_type: String? = null,
    val expires_in: Int? = null
)

@Serializable
data class DeparturesResponse(val results: List<ApiDeparture> = emptyList())

@Serializable
data class ApiDeparture(val serviceJourney: ServiceJourney, val stopPoint: StopPoint, val estimatedOtherwisePlannedTime: String)

@Serializable
data class StopPoint(val platform: String)

@Serializable
data class ServiceJourney(val direction: String, val line: Line)

@Serializable
data class Line(val shortName: String, val backgroundColor: String, val foregroundColor: String, val borderColor: String)

class TimetableService {

    private val BASE_URL = "https://ext-api.vasttrafik.se/pr/v4"
    private val TOKEN_URL = "https://ext-api.vasttrafik.se/token"
    private val gid = 9021014001200000
    private val CLIENT_ID = BuildConfig.CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.CLIENT_SECRET

    private var cachedTokens: BearerTokens? = null
    private var cachedTokensExpiresAt: Instant = Instant.EPOCH

    private val tokenClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    cachedTokens.takeIf { Instant.now().isBefore(cachedTokensExpiresAt) }
                }
                refreshTokens {
                    val response: TokenResponse = tokenClient.submitForm (
                        url = TOKEN_URL,
                        formParameters = parameters {
                            append("grant_type", "client_credentials")
                        }
                        ) {
                        basicAuth(CLIENT_ID, CLIENT_SECRET)
                    }.body()

                    val tokens = BearerTokens(
                        accessToken = response.access_token,
                        refreshToken = null
                    )
                    cachedTokens = tokens
                    cachedTokensExpiresAt = tokenExpiry(response)
                    tokens
                }
            }
        }
    }

    suspend fun getDepartures(): List<Departure> {
        val departures = client.get("$BASE_URL/stop-areas/${gid}/departures").body<DeparturesResponse>()
        return departures.results.filter { departure -> departure.stopPoint.platform == "C" }
            .map { apiDeparture ->
                Departure(apiDeparture.serviceJourney.line.shortName,
                    apiDeparture.serviceJourney.direction,
                    departureTime(apiDeparture.estimatedOtherwisePlannedTime),
                    apiDeparture.serviceJourney.line.backgroundColor,
                    apiDeparture.serviceJourney.line.foregroundColor,
                    apiDeparture.serviceJourney.line.borderColor)
        }
    }

    private fun departureTime(departureTimestamp: String): String {
        val departureTime = ZonedDateTime.parse(departureTimestamp)
        val now = ZonedDateTime.now(departureTime.zone)

        val duration = Duration.between(now, departureTime)
        val minutes = duration.toMinutes()

        return when {
            minutes < 1 -> "Nu"
            else -> "$minutes min"
        }
    }

    private fun tokenExpiry(response: TokenResponse): Instant {
        val expiresIn = response.expires_in?.toLong() ?: 0L
        val safetyBufferSeconds = 60L
        return Instant.now().plusSeconds(maxOf(0L, expiresIn - safetyBufferSeconds))
    }
}
