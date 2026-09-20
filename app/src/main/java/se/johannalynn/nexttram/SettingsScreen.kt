package se.johannalynn.nexttram

import android.text.format.DateFormat
import androidx.annotation.StringRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    rememberItems: List<RememberItem>,
    onSaveRememberItem: (String?, String) -> Unit,
    onDeleteRememberItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editedTime by remember { mutableStateOf<ScheduleTime?>(null) }
    var editingRememberItem by rememberSaveable { mutableStateOf(false) }
    var editedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var editedItemName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormatter = remember(locale, is24Hour) {
        val skeleton = if (is24Hour) "Hm" else "hm"
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_dark_mode), modifier = Modifier.weight(1f))
                Switch(
                    checked = darkModeEnabled,
                    onCheckedChange = onDarkModeChanged
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_update_minute_interval)) },
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
                    headlineContent = { Text(stringResource(R.string.settings_auto_update_from)) },
                    trailingContent = { Text(formatTime(autoUpdateSettings.startMinutes, timeFormatter)) },
                    modifier = Modifier.clickable { editedTime = ScheduleTime.START },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_auto_update_to)) },
                    trailingContent = { Text(formatTime(autoUpdateSettings.endMinutes, timeFormatter)) },
                    modifier = Modifier.clickable { editedTime = ScheduleTime.END },
                )
            }
            Text(
                text = stringResource(R.string.settings_station),
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
                label = { Text(stringResource(R.string.settings_search_station)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }

        when (stationSearchState) {
            StationSearchUiState.Idle -> Unit
            StationSearchUiState.Loading -> item { CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(24.dp)
            ) }
            StationSearchUiState.Error -> item { Text(
                text = stringResource(R.string.error_search_stations),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            ) }
            is StationSearchUiState.Success -> {
                if (stationSearchState.stations.isEmpty()) {
                    item { Text(
                        text = stringResource(R.string.settings_no_stations_found),
                        modifier = Modifier.padding(top = 16.dp)
                    ) }
                } else {
                    items(
                        items = stationSearchState.stations,
                        key = { "station:${it.gid}" },
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
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.remember_title),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    editedItemId = null
                    editedItemName = ""
                    editingRememberItem = true
                }) { Text(stringResource(R.string.remember_add)) }
            }
        }
        items(rememberItems, key = { "remember:${it.id}" }) { item ->
            ListItem(
                headlineContent = { Text(item.name) },
                trailingContent = {
                    IconButton(onClick = { onDeleteRememberItem(item.id) }) {
                        Icon(Icons.Default.Delete,
                            contentDescription = stringResource(R.string.remember_delete, item.name))
                    }
                },
                modifier = Modifier.clickable {
                    editedItemId = item.id
                    editedItemName = item.name
                    editingRememberItem = true
                },
            )
        }
    }

    if (editingRememberItem) {
        AlertDialog(
            onDismissRequest = { editingRememberItem = false },
            title = { Text(stringResource(if (editedItemId == null) R.string.remember_add else R.string.remember_edit)) },
            text = {
                OutlinedTextField(
                    value = editedItemName,
                    onValueChange = { editedItemName = it },
                    label = { Text(stringResource(R.string.remember_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editedItemName.isNotBlank(),
                    onClick = {
                        onSaveRememberItem(editedItemId, editedItemName)
                        editingRememberItem = false
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingRememberItem = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    when (editedTime) {
        ScheduleTime.START -> ScheduleTimePickerDialog(
            titleResId = R.string.settings_start_time,
            initialMinutes = autoUpdateSettings.startMinutes,
            onConfirm = {
                onAutoUpdateStartChanged(it)
                editedTime = null
            },
            onDismiss = { editedTime = null },
        )
        ScheduleTime.END -> ScheduleTimePickerDialog(
            titleResId = R.string.settings_end_time,
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
    @StringRes titleResId: Int,
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun formatTime(minutes: Int, formatter: DateTimeFormatter): String = formatter.format(
    LocalTime.of(minutes / 60, minutes % 60)
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
            rememberItems = listOf(RememberItem("1", "Matlåda")),
            onSaveRememberItem = { _, _ -> },
            onDeleteRememberItem = {},
        )
    }
}
