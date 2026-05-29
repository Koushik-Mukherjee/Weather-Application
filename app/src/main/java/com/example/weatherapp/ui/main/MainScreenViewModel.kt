package com.example.weatherapp.ui.main

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.data.GeocodingResult
import com.example.weatherapp.data.LocationHelper
import com.example.weatherapp.data.WeatherApi
import com.example.weatherapp.data.WeatherData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Error(val message: String) : WeatherUiState
    data class Success(val weatherData: WeatherData) : WeatherUiState
}

class MainScreenViewModel(
    private val context: Context,
    private val weatherApi: WeatherApi = WeatherApi()
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isFahrenheit = MutableStateFlow(false)
    val isFahrenheit: StateFlow<Boolean> = _isFahrenheit.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val recentSearches: StateFlow<List<GeocodingResult>> = _recentSearches.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadSettings()
        loadRecentSearches()
        // Load initial weather (either last selected or default to London)
        loadInitialWeather()
    }

    private fun loadSettings() {
        _isFahrenheit.value = sharedPrefs.getBoolean("is_fahrenheit", false)
    }

    fun toggleUnit() {
        val newVal = !_isFahrenheit.value
        _isFahrenheit.value = newVal
        sharedPrefs.edit().putBoolean("is_fahrenheit", newVal).apply()
    }

    private fun loadRecentSearches() {
        val json = sharedPrefs.getString("recent_searches", null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<GeocodingResult>>() {}.type
                val list: List<GeocodingResult> = gson.fromJson(json, type)
                _recentSearches.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveRecentSearches(list: List<GeocodingResult>) {
        _recentSearches.value = list
        val json = gson.toJson(list)
        sharedPrefs.edit().putString("recent_searches", json).apply()
    }

    private fun addToRecent(location: GeocodingResult) {
        val current = _recentSearches.value.toMutableList()
        current.removeAll { it.id == location.id }
        current.add(0, location)
        if (current.size > 5) {
            current.removeAt(current.size - 1)
        }
        saveRecentSearches(current)
    }

    private fun loadInitialWeather() {
        val json = sharedPrefs.getString("last_location", null)
        if (json != null) {
            try {
                val location = gson.fromJson(json, GeocodingResult::class.java)
                fetchWeatherForLocation(location)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Default location: London
        val defaultLocation = GeocodingResult(
            id = 2643743,
            name = "London",
            latitude = 51.50853,
            longitude = -0.12574,
            country = "United Kingdom",
            countryCode = "GB",
            admin1 = "England",
            timezone = "Europe/London"
        )
        fetchWeatherForLocation(defaultLocation)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // Debounce
            val results = weatherApi.searchCity(query)
            _searchResults.value = results
        }
    }

    fun selectLocation(location: GeocodingResult) {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        addToRecent(location)
        sharedPrefs.edit().putString("last_location", gson.toJson(location)).apply()
        fetchWeatherForLocation(location)
    }

    fun fetchWeatherForLocation(location: GeocodingResult) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = weatherApi.fetchWeather(location.latitude, location.longitude)
                _uiState.value = WeatherUiState.Success(WeatherData(location, response))
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = WeatherUiState.Error(e.message ?: "Failed to load weather data")
            }
        }
    }

    fun fetchWeatherForCurrentLocation(locationHelper: LocationHelper) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val loc = locationHelper.getCurrentLocation()
            if (loc == null) {
                _uiState.value = WeatherUiState.Error("Could not retrieve GPS location. Check your GPS and permissions.")
                // reload last location after a small delay
                delay(2000)
                loadInitialWeather()
                return@launch
            }

            try {
                // Reverse geocode using system geocoder to find city name
                val cityName = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        addresses?.firstOrNull()?.locality ?: "Current Location"
                    } catch (e: Exception) {
                        "Current Location"
                    }
                }

                val location = GeocodingResult(
                    id = System.currentTimeMillis(), // Temporary ID
                    name = cityName,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    country = "",
                    countryCode = null,
                    admin1 = null,
                    timezone = "auto"
                )

                val response = weatherApi.fetchWeather(loc.latitude, loc.longitude)
                _uiState.value = WeatherUiState.Success(WeatherData(location, response))
                
                // Add to recent and save last location
                addToRecent(location)
                sharedPrefs.edit().putString("last_location", gson.toJson(location)).apply()

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = WeatherUiState.Error(e.message ?: "Failed to fetch weather for current location")
            }
        }
    }
}
