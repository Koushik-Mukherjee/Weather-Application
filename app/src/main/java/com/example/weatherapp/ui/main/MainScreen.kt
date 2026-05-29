package com.example.weatherapp.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.weatherapp.data.GeocodingResult
import com.example.weatherapp.data.LocationHelper
import com.example.weatherapp.data.WeatherData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: MainScreenViewModel = viewModel { 
        MainScreenViewModel(context) 
    }
    val locationHelper = remember { LocationHelper(context) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isFahrenheit by viewModel.isFahrenheit.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    // Permission launcher for Location
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions.values.any { it }
            if (granted) {
                viewModel.fetchWeatherForCurrentLocation(locationHelper)
            }
        }
    )

    // Current weather code and day/night state for background
    val (weatherCode, isDay) = remember(uiState) {
        when (val state = uiState) {
            is WeatherUiState.Success -> {
                state.weatherData.weather.current.weatherCode to (state.weatherData.weather.current.isDay == 1)
            }
            else -> 0 to true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Particle Canvas Background
        WeatherBackground(
            weatherCode = weatherCode,
            isDay = isDay,
            modifier = Modifier.fillMaxSize()
        )

        // Main content column
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Search and Utilities Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glassmorphic Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search location...", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                // Locate Button
                IconButton(
                    onClick = {
                        if (locationHelper.hasLocationPermission()) {
                            viewModel.fetchWeatherForCurrentLocation(locationHelper)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Get current location",
                        tint = Color.White
                    )
                }

                // C / F Toggle Button
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.toggleUnit() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFahrenheit) "°F" else "°C",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Search Results / Recents Suggestion Box Overlay
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty() || searchResults.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE01E293B), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        if (searchResults.isEmpty() && searchQuery.length >= 2) {
                            Text(
                                "No locations found",
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            searchResults.forEach { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectLocation(loc) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = loc.displayName,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            // Weather Content (Success / Loading / Error states)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val state = uiState) {
                    is WeatherUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is WeatherUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error Loading Weather",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is WeatherUiState.Success -> {
                        WeatherDashboardContent(
                            data = state.weatherData,
                            isFahrenheit = isFahrenheit,
                            onRefresh = { viewModel.fetchWeatherForLocation(state.weatherData.location) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDashboardContent(
    data: WeatherData,
    isFahrenheit: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val weather = data.weather
    val current = weather.current

    val currentTemp = remember(current.temperature2m, isFahrenheit) {
        if (isFahrenheit) ((current.temperature2m * 9 / 5) + 32).toInt() else current.temperature2m.toInt()
    }

    val feelsLike = remember(current.apparentTemperature, isFahrenheit) {
        if (isFahrenheit) ((current.apparentTemperature * 9 / 5) + 32).toInt() else current.apparentTemperature.toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location Info and Date Card
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = data.location.name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            data.location.country?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = remember { 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) 
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }

        // Main Weather Display (Big animated Icon and Temperature)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherIcon(
                weatherCode = current.weatherCode,
                modifier = Modifier.size(110.dp),
                isDay = current.isDay == 1
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "$currentTemp°",
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = getWeatherDescription(current.weatherCode),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- Glassmorphic Details Grid ---
        GridDetails(
            humidity = "${current.relativeHumidity2m.toInt()}%",
            windSpeed = "${current.windSpeed10m} km/h",
            feelsLike = "$feelsLike°",
            pressure = "${current.pressureMsl.toInt()} hPa",
            cloudCover = "${current.cloudCover.toInt()}%",
            uvIndex = "${weather.daily.uvIndexMax.firstOrNull() ?: 0.0}"
        )

        // --- 24-Hour Forecast Card ---
        GlassCard(
            title = "Hourly Temperature",
            icon = Icons.Default.Info
        ) {
            HourlyForecastChart(
                times = weather.hourly.time,
                temps = weather.hourly.temperature2m,
                weatherCodes = weather.hourly.weatherCode,
                isFahrenheit = isFahrenheit,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // --- 7-Day Forecast Card ---
        GlassCard(
            title = "7-Day Forecast",
            icon = Icons.Default.Info
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val daily = weather.daily
                val size = daily.time.size
                for (i in 0 until size) {
                    val dateStr = daily.time[i]
                    val formattedDay = remember(dateStr) {
                        try {
                            val localDate = LocalDate.parse(dateStr)
                            if (localDate.isEqual(LocalDate.now())) {
                                "Today"
                            } else {
                                localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            }
                        } catch (e: Exception) {
                            dateStr
                        }
                    }

                    val maxTemp = remember(daily.temperature2mMax[i], isFahrenheit) {
                        if (isFahrenheit) ((daily.temperature2mMax[i] * 9 / 5) + 32).toInt() else daily.temperature2mMax[i].toInt()
                    }
                    val minTemp = remember(daily.temperature2mMin[i], isFahrenheit) {
                        if (isFahrenheit) ((daily.temperature2mMin[i] * 9 / 5) + 32).toInt() else daily.temperature2mMin[i].toInt()
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedDay,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        WeatherIcon(
                            weatherCode = daily.weatherCode[i],
                            modifier = Modifier.size(28.dp),
                            isDay = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "$maxTemp°",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "$minTemp°",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    if (i < size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    }
                }
            }
        }
        
        // Footer spacer
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GridDetails(
    humidity: String,
    windSpeed: String,
    feelsLike: String,
    pressure: String,
    cloudCover: String,
    uvIndex: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailItem(label = "Feels Like", value = feelsLike, modifier = Modifier.weight(1f))
            DetailItem(label = "Humidity", value = humidity, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailItem(label = "Wind Speed", value = windSpeed, modifier = Modifier.weight(1f))
            DetailItem(label = "UV Index", value = uvIndex, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailItem(label = "Pressure", value = pressure, modifier = Modifier.weight(1f))
            DetailItem(label = "Cloud Cover", value = cloudCover, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GlassCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Clear sky"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow fall"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }
}
