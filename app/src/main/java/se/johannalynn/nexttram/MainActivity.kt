package se.johannalynn.nexttram

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
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

    val uiState by viewModel.uiState.collectAsState()
    val selectedStation by viewModel.selectedStation.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val stationQuery by viewModel.stationQuery.collectAsState()
    val stationSearchState by viewModel.stationSearchState.collectAsState()
    val autoUpdateSettings by viewModel.autoUpdateSettings.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.start()
    }

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
                                contentDescription = stringResource(it.labelResId)
                            )
                        },
                        label = { Text(stringResource(it.labelResId)) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        LifecycleStartEffect(viewModel) {
                            viewModel.startAutoUpdate()
                            onStopOrDispose { viewModel.stopAutoUpdate() }
                        }
                        TimetableScreenWrapper(
                            uiState = uiState,
                            stationName = selectedStation.name,
                            selectedPlatform = selectedPlatform,
                            onPlatformSelected = viewModel::selectPlatform,
                            onRefresh = viewModel::fetchDepartures,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    AppDestinations.SETTINGS -> SettingsScreen(
                        darkModeEnabled = darkModeEnabled,
                        onDarkModeChanged = { darkModeEnabled = it },
                        autoUpdateSettings = autoUpdateSettings,
                        onAutoUpdateEnabledChanged = viewModel::setAutoUpdateEnabled,
                        onAutoUpdateStartChanged = viewModel::setAutoUpdateStart,
                        onAutoUpdateEndChanged = viewModel::setAutoUpdateEnd,
                        selectedStationName = selectedStation.name,
                        stationQuery = stationQuery,
                        stationSearchState = stationSearchState,
                        onStationQueryChanged = viewModel::onStationQueryChanged,
                        onStationSelected = viewModel::selectStation,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    @StringRes val labelResId: Int,
    val icon: ImageVector,
) {
    HOME(R.string.navigation_home, Icons.Default.Home),
    SETTINGS(R.string.navigation_settings, Icons.Default.Settings),
}
