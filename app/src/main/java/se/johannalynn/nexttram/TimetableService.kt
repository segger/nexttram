package se.johannalynn.nexttram

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Todo(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean
)

class TimetableService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getDepartures(): List<Departure> {
        val todo: Todo = client.get("https://jsonplaceholder.typicode.com/todos/1").body()

        return listOf(
            Departure("11", todo.title, "Nu"),
            Departure("7", "Tynnered", "3 min"),
            Departure("11", "Saltholmen", "5 min"),
            Departure("6", "Länsmansgården", "8 min"),
            Departure("7", "Angered", "12 min")
        )
    }
}
