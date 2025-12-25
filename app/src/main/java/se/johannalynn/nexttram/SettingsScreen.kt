package se.johannalynn.nexttram

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row {
            Text(
                text = "Inställningar",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Hållplats") }
        )
    }
}

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NextTramTheme {
        SettingsScreen()
    }
}
