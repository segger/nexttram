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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TimetableScreenWrapper(
    uiState: TimetableUiState,
    defaultPlatform: String,
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
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is TimetableUiState.Success -> {
            TimetableScreen(
                departures = uiState.departures,
                platforms = uiState.platforms,
                defaultPlatform = defaultPlatform,
                onRefresh = onRefresh,
                lastUpdated = SimpleDateFormat("d MMMM HH:mm", Locale("sv", "SE")).format(Calendar.getInstance().time),
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
    defaultPlatform: String,
    onRefresh: () -> Unit,
    lastUpdated: String,
    modifier: Modifier = Modifier
) {
    var selectedPlatform by remember(defaultPlatform, platforms) {
        mutableStateOf(defaultPlatform.takeIf { it in platforms } ?: platforms.firstOrNull() ?: "")
    }
    var platformMenuExpanded by remember { mutableStateOf(false) }
    val visibleDepartures = departures.filter { departure -> departure.platform == selectedPlatform }

    PullToRefreshBox(
        isRefreshing = false,
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
                        text = "Axel Dahlströms torg",
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
                                        selectedPlatform = platform
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
            defaultPlatform = "C",
            onRefresh = {},
            lastUpdated = "6 april 15:30"
        )
    }
}
