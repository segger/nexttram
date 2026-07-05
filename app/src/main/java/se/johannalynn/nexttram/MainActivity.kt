package se.johannalynn.nexttram

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.johannalynn.nexttram.ui.theme.NextTramTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            NextTramApp()
        }
    }
}

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun NextTramApp(
    viewModel: TimetableViewModel = viewModel()
) {
    var currentDestination by remember { mutableStateOf(AppDestinations.HOME) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var defaultPlatform by remember { mutableStateOf("C") }

    val uiState by viewModel.uiState.collectAsState()

    NextTramTheme(darkTheme = darkModeEnabled) {
        val navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )

        NavigationSuiteScaffold(
            navigationSuiteColors = navigationSuiteColors,
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        modifier = Modifier.padding(8.dp),
                        icon = {
                            Icon(
                                it.icon,
                                contentDescription = it.label
                            )
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.HOME -> TimetableScreenWrapper(
                        uiState = uiState,
                        defaultPlatform = defaultPlatform,
                        onRefresh = viewModel::fetchDepartures,
                        modifier = Modifier.padding(innerPadding)
                    )
                    AppDestinations.SETTINGS -> SettingsScreen(
                        darkModeEnabled = darkModeEnabled,
                        onDarkModeChanged = { darkModeEnabled = it },
                        defaultPlatform = defaultPlatform,
                        onDefaultPlatformChanged = { defaultPlatform = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Hem", Icons.Default.Home),
    SETTINGS("Inställningar", Icons.Default.Settings),
}
