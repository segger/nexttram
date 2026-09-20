package se.johannalynn.nexttram

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class AutoUpdateSettingsTest {
    @Test
    fun daytimePeriodIncludesStartAndExcludesEnd() {
        val settings = AutoUpdateSettings(enabled = true, startMinutes = 6 * 60, endMinutes = 9 * 60)

        assertFalse(settings.isActiveAt(LocalTime.of(5, 59)))
        assertTrue(settings.isActiveAt(LocalTime.of(6, 0)))
        assertTrue(settings.isActiveAt(LocalTime.of(8, 59)))
        assertFalse(settings.isActiveAt(LocalTime.of(9, 0)))
    }

    @Test
    fun overnightPeriodCrossesMidnight() {
        val settings = AutoUpdateSettings(enabled = true, startMinutes = 22 * 60, endMinutes = 2 * 60)

        assertTrue(settings.isActiveAt(LocalTime.of(23, 0)))
        assertTrue(settings.isActiveAt(LocalTime.of(1, 59)))
        assertFalse(settings.isActiveAt(LocalTime.of(2, 0)))
        assertFalse(settings.isActiveAt(LocalTime.of(12, 0)))
    }

    @Test
    fun equalTimesAreActiveAllDay() {
        val settings = AutoUpdateSettings(enabled = true, startMinutes = 6 * 60, endMinutes = 6 * 60)

        assertTrue(settings.isActiveAt(LocalTime.MIDNIGHT))
        assertTrue(settings.isActiveAt(LocalTime.of(23, 59)))
    }
}
