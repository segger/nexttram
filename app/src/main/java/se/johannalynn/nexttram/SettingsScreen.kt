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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme
import java.util.Locale

@Composable
fun SettingsScreen(
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    autoUpdateSettings: AutoUpdateSettings,
    onAutoUpdateEnabledChanged: (Boolean) -> Unit,
    onAutoUpdateStartChanged: (Int) -> Unit,
    onAutoUpdateEndChanged: (Int) -> Unit,
    selectedStationName: String,
    stationQuery: String,
    stationSearchState: StationSearchUiState,
    onStationQueryChanged: (String) -> Unit,
    onStationSelected: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    var editedTime by remember { mutableStateOf<ScheduleTime?>(null) }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark mode", modifier = Modifier.weight(1f))
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = onDarkModeChanged
            )
        }
        ListItem(
            headlineContent = { Text("Automatisk uppdatering") },
            trailingContent = {
                Switch(
                    checked = autoUpdateSettings.enabled,
                    onCheckedChange = onAutoUpdateEnabledChanged,
                )
            },
            modifier = Modifier.padding(top = 8.dp),
        )
        if (autoUpdateSettings.enabled) {
            ListItem(
                headlineContent = { Text("Från") },
                trailingContent = { Text(formatTime(autoUpdateSettings.startMinutes)) },
                modifier = Modifier.clickable { editedTime = ScheduleTime.START },
            )
            ListItem(
                headlineContent = { Text("Till") },
                trailingContent = { Text(formatTime(autoUpdateSettings.endMinutes)) },
                modifier = Modifier.clickable { editedTime = ScheduleTime.END },
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

    when (editedTime) {
        ScheduleTime.START -> ScheduleTimePickerDialog(
            title = "Starttid",
            initialMinutes = autoUpdateSettings.startMinutes,
            onConfirm = {
                onAutoUpdateStartChanged(it)
                editedTime = null
            },
            onDismiss = { editedTime = null },
        )
        ScheduleTime.END -> ScheduleTimePickerDialog(
            title = "Sluttid",
            initialMinutes = autoUpdateSettings.endMinutes,
            onConfirm = {
                onAutoUpdateEndChanged(it)
                editedTime = null
            },
            onDismiss = { editedTime = null },
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text("Spara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        },
    )
}

private fun formatTime(minutes: Int): String = String.format(
    Locale.ROOT,
    "%02d:%02d",
    minutes / 60,
    minutes % 60,
)

private enum class ScheduleTime { START, END }

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NextTramTheme {
        SettingsScreen(
            darkModeEnabled = false,
            onDarkModeChanged = {},
            autoUpdateSettings = AutoUpdateSettings(
                enabled = true,
                startMinutes = 6 * 60,
                endMinutes = 9 * 60,
            ),
            onAutoUpdateEnabledChanged = {},
            onAutoUpdateStartChanged = {},
            onAutoUpdateEndChanged = {},
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
