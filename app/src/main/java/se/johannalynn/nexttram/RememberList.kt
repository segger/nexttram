package se.johannalynn.nexttram

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class RememberItem(val id: String, val name: String, val checked: Boolean = false)

@Serializable
data class RememberList(val date: String, val items: List<RememberItem> = emptyList()) {
    fun forDate(today: LocalDate): RememberList = if (date == today.toString()) this else {
        copy(date = today.toString(), items = items.map { it.copy(checked = false) })
    }
}
