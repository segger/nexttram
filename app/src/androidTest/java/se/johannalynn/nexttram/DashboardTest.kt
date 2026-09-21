package se.johannalynn.nexttram

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import se.johannalynn.nexttram.ui.theme.NextTramTheme
import java.io.File
import java.time.Instant

class DashboardTest {
    @get:Rule val compose = createComposeRule()
    private val now = Instant.parse("2026-09-20T06:00:00Z").toEpochMilli()
    private val weather = WeatherUiState(
        periods = WeatherPeriod.entries.map {
            val gust = if (it == WeatherPeriod.EVENING) null else 10.0
            PeriodForecast(it, listOf(
                WeatherHour(now, 10.0, "partlycloudy_day", 0.2, 3.0, gust?.minus(2.0)),
                WeatherHour(now + 3_600_000, 12.0, "partlycloudy_day", 0.3, 5.0, gust),
            ))
        },
        updatedAtMillis = now,
    )

    @Test
    fun tabletKeepsSectionsVisibleWhileListsScrollAndItemsCanBeUnchecked() {
        var items by mutableStateOf(List(40) { RememberItem("$it", "Sak $it") })
        compose.setContent {
            NextTramTheme {
                Box(Modifier.size(600.dp, 600.dp)) {
                    DashboardScreen(weather, items, { id, checked ->
                        items = items.map { if (it.id == id) it.copy(checked = checked) else it }
                    }, {}) { modifier -> TestDepartures(modifier) }
                }
            }
        }
        compose.onNodeWithText("Period").assertIsDisplayed()
        compose.onNodeWithText("Temp\n(°C)").assertIsDisplayed()
        compose.onNodeWithText("Nederbörd\n(mm)").assertIsDisplayed()
        compose.onNodeWithText("Vind\n(m/s)").assertIsDisplayed()
        compose.onAllNodesWithText("10–12").assertCountEquals(3)
        compose.onAllNodesWithText("3–5 (10)").assertCountEquals(2)
        compose.onNodeWithText("3–5 (—)").assertIsDisplayed()
        compose.onNodeWithText("Kom ihåg").assertIsDisplayed()
        compose.onNodeWithText("Axel Dahlströms Torg").assertIsDisplayed()
        compose.onNodeWithText("Sak 0").performClick().assertIsOn()
        compose.onNodeWithText("Sak 0").performClick().assertIsOff()
        screenshot("dashboard-tablet.png")
        compose.onNodeWithTag("remember-list")
            .performScrollToNode(hasText("Sak 39"))
        compose.onNodeWithText("Sak 39").assertIsDisplayed()
        compose.onNodeWithText("Period").assertIsDisplayed()
        compose.onNodeWithText("Axel Dahlströms Torg").assertIsDisplayed()
        compose.onNodeWithTag("departures-list")
            .performScrollToNode(hasText("Destination 49"))
        compose.onNodeWithText("Period").assertIsDisplayed()
        compose.onNodeWithText("Kom ihåg").assertIsDisplayed()
    }

    @Test
    fun failuresDoNotHideCachedWeatherOrRememberList() {
        compose.setContent {
            NextTramTheme {
                DashboardScreen(weather.copy(refreshFailed = true), listOf(RememberItem("1", "Matlåda")), { _, _ -> }, {}) {
                    TestDepartures(it, TimetableUiState.Error)
                }
            }
        }
        compose.onNodeWithText("Kunde inte uppdatera vädret").assertIsDisplayed()
        compose.onNodeWithText("Morgon").assertIsDisplayed()
        compose.onNodeWithText("Matlåda").assertIsDisplayed()
        compose.onNodeWithText("Försök igen").assertIsDisplayed()
    }

    @Test
    fun phonePageScrollsToRememberList() {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { smallestScreenWidthDp = 360 }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                NextTramTheme {
                    Box(Modifier.size(360.dp, 600.dp)) {
                        DashboardScreen(weather, listOf(RememberItem("1", "Matlåda")), { _, _ -> }, {}) {
                            TestDepartures(it)
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("dashboard").performTouchInput { swipeUp() }
        compose.onNodeWithText("Kom ihåg").assertIsDisplayed()
        compose.onNodeWithText("Matlåda").assertIsDisplayed()
        screenshot("dashboard-phone.png")
    }

    @Test
    fun settingsCanAddRenameAndDeleteItemsAndRejectBlankNames() {
        var items by mutableStateOf(emptyList<RememberItem>())
        compose.setContent {
            NextTramTheme {
                SettingsScreen(
                    darkModeEnabled = false, onDarkModeChanged = {},
                    autoUpdateSettings = AutoUpdateSettings(false, 360, 540),
                    onAutoUpdateEnabledChanged = {}, onAutoUpdateStartChanged = {}, onAutoUpdateEndChanged = {},
                    selectedStationName = "Axel Dahlströms Torg", stationQuery = "",
                    stationSearchState = StationSearchUiState.Idle,
                    onStationQueryChanged = {}, onStationSelected = {},
                    rememberItems = items,
                    onSaveRememberItem = { id, name ->
                        items = if (id == null) items + RememberItem("1", name)
                        else items.map { if (it.id == id) it.copy(name = name) else it }
                    },
                    onDeleteRememberItem = { id -> items = items.filterNot { it.id == id } },
                )
            }
        }
        compose.onNodeWithText("Lägg till").performScrollTo().performClick()
        compose.onNodeWithText("Spara").assertIsNotEnabled()
        compose.onNodeWithText("Sak att komma ihåg").performTextInput("Matlåda")
        compose.onNodeWithText("Spara").performClick()
        compose.onNodeWithText("Matlåda").performScrollTo().performClick()
        compose.onNodeWithText("Sak att komma ihåg").performTextReplacement("Nycklar")
        compose.onNodeWithText("Spara").performClick()
        compose.onNodeWithText("Nycklar").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Ta bort Nycklar").performClick()
        compose.onNodeWithText("Nycklar").assertDoesNotExist()
        assertTrue(items.isEmpty())
    }

    @androidx.compose.runtime.Composable
    private fun TestDepartures(modifier: Modifier, state: TimetableUiState = TimetableUiState.Success(
        List(50) { Departure("1", "Destination $it", it + 1, "A", "#FFFFFF", "#000000", "#000000") },
        listOf("A"), now,
    )) {
        TimetableScreenWrapper(state, "Axel Dahlströms Torg", "A", {}, {}, modifier)
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), name)
        file.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("cp ${file.absolutePath} /data/local/tmp/$name")
        ).use { it.readBytes() }
    }
}
