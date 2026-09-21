package se.johannalynn.nexttram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import se.johannalynn.nexttram.ui.theme.NextTramTheme
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    weather: WeatherUiState,
    rememberItems: List<RememberItem>,
    onItemChecked: (String, Boolean) -> Unit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier,
    departures: @Composable (Modifier) -> Unit,
) {
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    Column(
        modifier = modifier.fillMaxSize().testTag("dashboard")
            .then(if (tablet) Modifier else Modifier.verticalScroll(rememberScrollState())),
    ) {
        departures(if (tablet) Modifier.weight(3f) else Modifier.height(360.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        if (tablet) {
            Row(modifier = Modifier.weight(3f).fillMaxWidth()) {
                WeatherPanel(
                    state = weather,
                    onRefresh = onRefreshWeather,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                )
                VerticalDivider()
                RememberPanel(
                    items = rememberItems,
                    onChecked = onItemChecked,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                )
            }
        } else {
            WeatherPanel(
                state = weather,
                onRefresh = onRefreshWeather,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            RememberPanel(
                items = rememberItems,
                onChecked = onItemChecked,
                modifier = Modifier.height(280.dp).fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun WeatherPanel(
    state: WeatherUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.updatedAtMillis?.let {
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(LocalConfiguration.current.locales[0]).withZone(weatherZone)
                Text(
                    stringResource(R.string.weather_updated, formatter.format(Instant.ofEpochMilli(it))),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp))
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.weather_refresh))
                }
            }
        }
        if (state.refreshFailed) {
            Text(stringResource(R.string.error_update_weather), color = MaterialTheme.colorScheme.error)
        }
        if (state.periods.isEmpty() && !state.isLoading) {
            Text(stringResource(R.string.weather_unavailable), style = MaterialTheme.typography.bodyMedium)
        }
        if (state.periods.isNotEmpty()) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.weather_period_header), modifier = Modifier.weight(1.65f))
                    Spacer(Modifier.width(24.dp))
                    Text(stringResource(R.string.weather_temperature_header),
                        modifier = Modifier.weight(0.95f), textAlign = TextAlign.End)
                    Text(stringResource(R.string.weather_precipitation_header),
                        modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
                    Text(stringResource(R.string.weather_wind_header),
                        modifier = Modifier.weight(1.25f), textAlign = TextAlign.End)
                }
            }
        }
        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
            state.periods.forEach { forecast ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val periodName = stringResource(when (forecast.period) {
                        WeatherPeriod.MORNING -> R.string.weather_morning
                        WeatherPeriod.AFTERNOON -> R.string.weather_afternoon
                        WeatherPeriod.EVENING -> R.string.weather_evening
                    })
                    Text(periodName, modifier = Modifier.weight(1.65f))
                    val (icon, description) = weatherSymbol(forecast.symbol)
                    val descriptionText = stringResource(description)
                    Text(
                        icon,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(24.dp).semantics { contentDescription = descriptionText },
                    )
                    val minTemperature = forecast.hours.minOf { it.temperature }.roundToInt()
                    val maxTemperature = forecast.hours.maxOf { it.temperature }.roundToInt()
                    val temperatureHue = when {
                        minTemperature < 0 -> WeatherHue.COLD
                        maxTemperature >= 25 -> WeatherHue.HOT
                        else -> null
                    }
                    Text(if (minTemperature == maxTemperature) {
                        stringResource(R.string.weather_temperature_single, minTemperature)
                    } else stringResource(R.string.weather_temperature, minTemperature, maxTemperature),
                        color = temperatureHue?.let { weatherColor(it, darkTheme) } ?: Color.Unspecified,
                        fontWeight = if (minTemperature <= -10 || maxTemperature >= 30) FontWeight.Medium else null,
                        modifier = Modifier.weight(0.95f), textAlign = TextAlign.End)
                    Text(forecast.precipitation?.let { stringResource(R.string.weather_rain, it) }
                        ?: stringResource(R.string.weather_rain_missing),
                        color = if (forecast.precipitation?.let { it > 0 } == true) {
                            weatherColor(WeatherHue.RAIN, darkTheme)
                        } else Color.Unspecified,
                        fontWeight = if (forecast.precipitation?.let { it > 2 } == true) {
                            FontWeight.Medium
                        } else null,
                        modifier = Modifier.weight(1.4f), textAlign = TextAlign.End)
                    val minWind = forecast.hours.minOf { it.wind }.roundToInt()
                    val maxWindSpeed = forecast.hours.maxOf { it.wind }
                    val maxWind = maxWindSpeed.roundToInt()
                    val wind = if (minWind == maxWind) {
                        stringResource(R.string.weather_wind_single, minWind)
                    } else stringResource(R.string.weather_wind, minWind, maxWind)
                    val gust = forecast.hours.mapNotNull { it.gust }.maxOrNull()
                    val gustText = gust?.let { stringResource(R.string.weather_gust, it) }
                        ?: stringResource(R.string.weather_gust_missing)
                    val strongestWind = maxOf(maxWindSpeed, gust ?: 0.0)
                    Text(stringResource(R.string.weather_wind_with_gust, wind, gustText),
                        color = if (strongestWind >= 8) weatherColor(WeatherHue.WIND, darkTheme)
                            else Color.Unspecified,
                        fontWeight = if (strongestWind >= 14) FontWeight.Medium else null,
                        modifier = Modifier.weight(1.25f), textAlign = TextAlign.End)
                }
            }
        }
    }
}

private enum class WeatherHue { COLD, HOT, RAIN, WIND }

private fun weatherColor(hue: WeatherHue, darkTheme: Boolean): Color = when (hue) {
    WeatherHue.COLD -> Color(if (darkTheme) 0xFF9BC4E2 else 0xFF3F6F91)
    WeatherHue.HOT -> Color(if (darkTheme) 0xFFE9B872 else 0xFF946018)
    WeatherHue.RAIN -> Color(if (darkTheme) 0xFF86C5DA else 0xFF2F718F)
    WeatherHue.WIND -> Color(if (darkTheme) 0xFF8BC8BF else 0xFF39756D)
}

@Composable
private fun RememberPanel(
    items: List<RememberItem>,
    onChecked: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            stringResource(R.string.remember_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (items.isEmpty()) {
            Text(stringResource(R.string.remember_empty), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(modifier = Modifier.weight(1f).testTag("remember-list")) {
            items(items, key = RememberItem::id) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .toggleable(item.checked, role = Role.Checkbox) { onChecked(item.id, it) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = item.checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    Text(
                        item.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(if (item.checked) 0.5f else 1f),
                    )
                }
            }
        }
    }
}

private fun weatherSymbol(symbol: String?): Pair<String, Int> = when {
    symbol == null -> "—" to R.string.weather_unknown
    "thunder" in symbol -> "⛈" to R.string.weather_thunder
    "sleet" in symbol -> "🌨" to R.string.weather_sleet
    "snow" in symbol -> "❄" to R.string.weather_snow
    "rain" in symbol -> "🌧" to R.string.weather_rain_description
    "fog" in symbol -> "🌫" to R.string.weather_fog
    "partlycloudy" in symbol -> "⛅" to R.string.weather_partly_cloudy
    "cloudy" in symbol -> "☁" to R.string.weather_cloudy
    "clearsky" in symbol || "fair" in symbol ->
        (if ("night" in symbol) "☾" else "☀") to R.string.weather_clear
    else -> "—" to R.string.weather_unknown
}

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
private fun DashboardPreview() {
    NextTramTheme {
        DashboardScreen(
            weather = WeatherUiState(periods = WeatherPeriod.entries.map {
                PeriodForecast(it, listOf(WeatherHour(0, 30.0, "partlycloudy_day", 1.4, 5.0, 10.0)))
            }, updatedAtMillis = System.currentTimeMillis()),
            rememberItems = listOf(RememberItem("1", "Matlåda"), RememberItem("2", "Nycklar", true)),
            onItemChecked = { _, _ -> },
            onRefreshWeather = {},
        ) { modifier ->
            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Axel Dahlströms Torg")
            }
        }
    }
}
