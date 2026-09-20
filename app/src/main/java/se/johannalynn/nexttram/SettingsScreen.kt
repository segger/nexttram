package se.johannalynn.nexttram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme

@Composable
fun SettingsScreen(
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    selectedStationName: String,
    stationQuery: String,
    stationSearchState: StationSearchUiState,
    onStationQueryChanged: (String) -> Unit,
    onStationSelected: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            Text(
                text = "Inställningar",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(modifier = Modifier.padding(top = 16.dp)) {
            Text("Dark mode", modifier = Modifier.weight(1f))
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = onDarkModeChanged
            )
        }
        Text(
            text = "Hållplats",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = selectedStationName,
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedTextField(
            value = stationQuery,
            onValueChange = onStationQueryChanged,
            label = { Text("Sök hållplats") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        when (stationSearchState) {
            StationSearchUiState.Idle -> Unit
            StationSearchUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(24.dp)
            )
            is StationSearchUiState.Error -> Text(
                text = stationSearchState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
            is StationSearchUiState.Success -> {
                if (stationSearchState.stations.isEmpty()) {
                    Text(
                        text = "Inga hållplatser hittades",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(
                            items = stationSearchState.stations,
                            key = Station::gid,
                        ) { station ->
                            ListItem(
                                headlineContent = { Text(station.name) },
                                modifier = Modifier.clickable { onStationSelected(station) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NextTramTheme {
        SettingsScreen(
            darkModeEnabled = false,
            onDarkModeChanged = {},
            selectedStationName = "Axel Dahlströms Torg, Göteborg",
            stationQuery = "Central",
            stationSearchState = StationSearchUiState.Success(
                listOf(
                    Station("9021014001950000", "Centralstationen, Göteborg"),
                    Station("9021014017340000", "Centralstationen, Trollhättan"),
                )
            ),
            onStationQueryChanged = {},
            onStationSelected = {},
        )
    }
}
