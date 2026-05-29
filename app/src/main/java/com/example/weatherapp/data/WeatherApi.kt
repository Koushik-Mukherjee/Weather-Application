package com.example.weatherapp.data

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// --- Geocoding Models ---
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    @SerializedName("country_code") val countryCode: String?,
    val admin1: String?, // Region/State
    val timezone: String?
) {
    val displayName: String
        get() = buildString {
            append(name)
            if (!admin1.isNullOrEmpty()) append(", ").append(admin1)
            if (!country.isNullOrEmpty()) append(", ").append(country)
        }
}

// --- Weather Models ---
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String,
    val current: CurrentWeather,
    val hourly: HourlyWeather,
    val daily: DailyWeather
)

data class CurrentWeather(
    val time: String,
    @SerializedName("temperature_2m") val temperature2m: Double,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: Double,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    @SerializedName("is_day") val isDay: Int,
    val precipitation: Double,
    val rain: Double,
    val showers: Double,
    val snowfall: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("cloud_cover") val cloudCover: Double,
    @SerializedName("pressure_msl") val pressureMsl: Double,
    @SerializedName("wind_speed_10m") val windSpeed10m: Double
)

data class HourlyWeather(
    val time: List<String>,
    @SerializedName("temperature_2m") val temperature2m: List<Double>,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("wind_speed_10m") val windSpeed10m: List<Double>
)

data class DailyWeather(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperature2mMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double>
)

// UI Consolidated Model
data class WeatherData(
    val location: GeocodingResult,
    val weather: WeatherResponse
)

// --- Weather API Client ---
class WeatherApi {
    private val client = OkHttpClient()
    private val gson = com.google.gson.Gson()

    suspend fun searchCity(query: String): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${UriEncode(query)}&count=6&language=en"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                val geocodingResponse = gson.fromJson(responseBody, GeocodingResponse::class.java)
                geocodingResponse.results ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResponse = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,wind_speed_10m" +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max" +
                "&timezone=auto"

        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            gson.fromJson(responseBody, WeatherResponse::class.java)
        }
    }

    private fun UriEncode(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8")
    }
}
