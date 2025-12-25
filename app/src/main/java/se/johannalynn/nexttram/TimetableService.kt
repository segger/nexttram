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

@Serializable
data class Departure(
    val line: String,
    val destination: String,
    val next: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val scope: String? = null,
    val token_type: String? = null,
    val expires_in: Int? = null
)

class TimetableService {

    private val BASE_URL = "https://ext-api.vasttrafik.se/pr/v4"
    private val TOKEN_URL = "https://ext-api.vasttrafik.se/token"
    private val gid = 9021014001200000
    private val CLIENT_ID = BuildConfig.CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.CLIENT_SECRET


    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    // TODO store
                    null
                }
                refreshTokens {
                    val tokenClient = HttpClient(CIO) {
                        install(ContentNegotiation) {
                            json(Json { ignoreUnknownKeys = true })
                        }
                    }
                    val response: TokenResponse = tokenClient.submitForm (
                        url = TOKEN_URL,
                        formParameters = parameters {
                            append("grant_type", "client_credentials")
                        }
                        ) {
                        basicAuth(CLIENT_ID, CLIENT_SECRET)
                    }.body()

                    BearerTokens(
                        accessToken = response.access_token,
                        refreshToken = null
                    )
                }
            }
        }
    }

    suspend fun getDepartures(): List<Departure> {
        println("getDeparatures");
        val departures = client.get("$BASE_URL/stop-areas/${gid}/departures").body<String>()
        println(departures)

        return listOf(
            Departure("11", "Out of here", "Nu"),
            Departure("7", "Tynnered", "3 min"),
            Departure("11", "Saltholmen", "5 min"),
            Departure("6", "Länsmansgården", "8 min"),
            Departure("7", "Angered", "12 min")
        )
    }
}
