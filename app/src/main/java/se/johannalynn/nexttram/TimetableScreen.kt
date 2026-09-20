package se.johannalynn.nexttram

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimetableScreenWrapper(
    uiState: TimetableUiState,
    stationName: String,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is TimetableUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is TimetableUiState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Försök igen")
                }
            }
        }

        is TimetableUiState.Success -> {
            TimetableScreen(
                departures = uiState.departures,
                platforms = uiState.platforms,
                stationName = stationName,
                selectedPlatform = selectedPlatform,
                onPlatformSelected = onPlatformSelected,
                onRefresh = onRefresh,
                lastUpdated = formatLastUpdated(uiState.lastUpdatedMillis),
                isRefreshing = uiState.isRefreshing,
                refreshError = uiState.refreshError,
                modifier = modifier
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    departures: List<Departure>,
    platforms: List<String>,
    stationName: String,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    lastUpdated: String,
    isRefreshing: Boolean,
    refreshError: String?,
    modifier: Modifier = Modifier
) {
    var platformMenuExpanded by remember { mutableStateOf(false) }
    val visibleDepartures = departures.filter { departure -> departure.platform == selectedPlatform }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            // Screen Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        Text(
                            text = selectedPlatform,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { platformMenuExpanded = true }
                        )
                        DropdownMenu(
                            expanded = platformMenuExpanded,
                            onDismissRequest = { platformMenuExpanded = false }
                        ) {
                            platforms.forEach { platform ->
                                DropdownMenuItem(
                                    text = { Text(platform) },
                                    onClick = {
                                        onPlatformSelected(platform)
                                        platformMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Text(
                    text = lastUpdated,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                )
            }

            refreshError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (departures.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Inga kommande avgångar")
                }
            } else {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Linje",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = "Destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = "Nästa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.2f)
                    )
                }
                HorizontalDivider()

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleDepartures) { departure ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.weight(0.2f)) {
                                LineBadge(departure = departure)
                            }
                            Text(
                                text = departure.destination,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = departure.next,
                                modifier = Modifier.weight(0.2f)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LineBadge(departure: Departure, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .widthIn(min = 36.dp)
            .background(parseHexColor(departure.backgroundColor, Color.White), shape)
            .border(1.dp, parseHexColor(departure.borderColor, Color.Black), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = departure.line,
            color = parseHexColor(departure.foregroundColor, Color.Black),
            fontWeight = FontWeight.Bold
        )
    }
}

private fun parseHexColor(value: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)

private fun formatLastUpdated(timestampMillis: Long): String = LAST_UPDATED_FORMATTER
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(timestampMillis))

private val LAST_UPDATED_FORMATTER = DateTimeFormatter.ofPattern(
    "d MMMM HH:mm",
    Locale.forLanguageTag("sv-SE"),
)

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun TimetableScreenPreview() {
    NextTramTheme {
        TimetableScreen(
            departures = listOf(
                Departure("2", "Biskopsgården", "Nu", "C", "#ffdd00", "#006c93","#ffdd00"),
                Departure("7", "Tynnered", "3 min", "C", "#00435c", "#ffffff", "#00435c"),
                Departure("6", "Länsmansgården", "8 min", "B","#f89828", "#00435c", "#f89828"),
                Departure("1", "Östra sjukhuset", "12 min", "C", "#ffffff", "#006c93","#006c93")
            ),
            platforms = listOf("B", "C"),
            stationName = "Axel Dahlströms Torg, Göteborg",
            selectedPlatform = "C",
            onPlatformSelected = {},
            onRefresh = {},
            lastUpdated = "6 april 15:30",
            isRefreshing = false,
            refreshError = null,
        )
    }
}
