package se.johannalynn.nexttram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme

@Composable
fun TimetableScreenWrapper(
    uiState: TimetableUiState,
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
                modifier = modifier
            )
        }
    }
}


@Composable
fun TimetableScreen(
    departures: List<Departure>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)) {
        // Screen Header
        Text(
            text = "Axel Dahlströms torg ( C )",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

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

        // List
        LazyColumn {
            items(departures) { departure ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = departure.line,
                        modifier = Modifier.weight(0.2f)
                    )
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

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun TimetableScreenPreview() {
    NextTramTheme {
        TimetableScreen(
            departures = listOf(
                Departure("11", "Bergsjön", "Nu"),
                Departure("7", "Tynnered", "3 min"),
                Departure("11", "Saltholmen", "5 min"),
                Departure("6", "Länsmansgården", "8 min"),
                Departure("7", "Angered", "12 min")
            )
        )
    }
}
