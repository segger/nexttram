package se.johannalynn.nexttram

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RememberListTest {
    private val today = LocalDate.of(2026, 9, 20)
    private val list = RememberList(today.toString(), listOf(
        RememberItem("1", "Matlåda", true), RememberItem("2", "Nycklar", false),
    ))

    @Test
    fun sameDayCompletionSurvivesSerialization() {
        val restored = Json.decodeFromString<RememberList>(Json.encodeToString(list))
        assertEquals(list, restored.forDate(today))
        assertTrue(restored.items.first().checked)
    }

    @Test
    fun newDayClearsChecksAndPreservesItemsAndOrder() {
        val reset = list.forDate(today.plusDays(1))
        assertEquals(list.items.map { it.id to it.name }, reset.items.map { it.id to it.name })
        assertTrue(reset.items.none { it.checked })
        assertEquals("2026-09-21", reset.date)
    }

    @Test
    fun sameDayDoesNotResetAndSkippingSeveralDaysStillResets() {
        assertSame(list, list.forDate(today))
        assertFalse(list.forDate(today.plusDays(5)).items.first().checked)
    }
}
