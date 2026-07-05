package se.johannalynn.nexttram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    defaultPlatform: String,
    onDefaultPlatformChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
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
        OutlinedTextField(
            value = defaultPlatform,
            onValueChange = onDefaultPlatformChanged,
            label = { Text("Default platform") },
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NextTramTheme {
        SettingsScreen(
            darkModeEnabled = false,
            onDarkModeChanged = {},
            defaultPlatform = "C",
            onDefaultPlatformChanged = {}
        )
    }
}
