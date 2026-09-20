package se.johannalynn.nexttram

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class AutoUpdateScheduleTest {
    private val settings = AutoUpdateSettings(enabled = true, startMinutes = 6 * 60, endMinutes = 9 * 60)

    @Test
    fun checksEveryMinuteInsideWindowAndEveryHourOutside() {
        assertNext("06:00:00", "06:01:00")
        assertNext("08:58:30", "08:59:30")
        assertNext("09:00:00", "10:00:00")
        assertNext("12:15:30", "13:15:30")
    }

    @Test
    fun hourlyWaitEndsExactlyAtWindowStart() {
        assertNext("05:30:45", "06:00:00")
        assertNext("05:59:59", "06:00:00")
    }

    @Test
    fun minuteChecksStopAtWindowEnd() {
        assertNext("08:59:00", "09:59:00")
        assertNext("08:59:30", "09:59:30")
    }

    @Test
    fun disabledUpdatesCheckHourlyEvenInsideWindowOrAcrossItsStart() {
        assertNext("06:30:00", "07:30:00", settings.copy(enabled = false))
        assertNext("05:30:00", "06:30:00", settings.copy(enabled = false))
    }

    @Test
    fun settingsChangesRecalculateNextUpdateFromLastRequest() {
        val lastRequest = time("08:00:00")
        assertEquals(time("08:01:00"), settings.nextUpdateAt(lastRequest))
        assertEquals(time("09:00:00"), settings.copy(enabled = false).nextUpdateAt(lastRequest))
        assertEquals(time("08:30:00"), settings.copy(startMinutes = 8 * 60 + 30).nextUpdateAt(lastRequest))
    }

    @Test
    fun overnightWindowUsesMinuteChecksAcrossMidnight() {
        val overnight = settings.copy(startMinutes = 22 * 60, endMinutes = 2 * 60)
        assertNext("21:30:00", "22:00:00", overnight)
        assertNext("01:00:00", "01:01:00", overnight)
        assertNext("01:59:00", "02:59:00", overnight)
        assertNext("12:00:00", "13:00:00", overnight)
        val now = time("23:59:30")
        assertEquals(now.plusMinutes(1), overnight.nextUpdateAt(now))
    }

    @Test
    fun hourlyWaitCanEndAtNextDaysWindowStart() {
        val midnightStart = settings.copy(startMinutes = 0)
        val now = time("23:30:00")
        assertEquals(now.plusDays(1).withHour(0).withMinute(0), midnightStart.nextUpdateAt(now))
    }

    @Test
    fun equalStartAndEndCheckEveryMinuteAllDay() {
        val allDay = settings.copy(endMinutes = settings.startMinutes)
        assertNext("05:59:30", "06:00:30", allDay)
        assertNext("12:00:00", "12:01:00", allDay)
    }

    private fun assertNext(from: String, to: String, schedule: AutoUpdateSettings = settings) {
        assertEquals(time(to), schedule.nextUpdateAt(time(from)))
    }

    private fun time(value: String) = ZonedDateTime.parse("2026-09-20T$value+02:00[Europe/Stockholm]")
}
