# WeatherApp ⛅

A stunning, highly modern, and fully functional native Android weather application built using **Kotlin** and **Jetpack Compose**. The app integrates the free, high-performance Open-Meteo APIs (Forecast & Geocoding) requiring no API keys. It features a premium glassmorphic UI design, dynamic canvas-based weather particle backgrounds, custom animated vector weather icons, and an interactive scrollable temperature chart.

---

## 🌟 Key Features

*   **🎨 Dynamic Weather Themes**: The background gradient morphs smoothly according to the weather condition (sunny, cloudy, rainy, snowy, or stormy) and the time of day (day/night).
*   **🌀 Canvas Particle Engine**: Custom frame-by-frame background animations:
    *   *Sunny*: Solar flares and slowly shifting sunbeams.
    *   *Cloudy*: Parallax-drifting fluffy clouds.
    *   *Rainy & Stormy*: Angled falling rain streaks with expanding splash ripples on the floor.
    *   *Snowy*: Floating, rotating, and swaying snowflakes.
    *   *Stormy*: Active rain combined with random, double-strike lightning flash overlays.
*   **✨ Animated Weather Icons**: Premium weather symbols drawn using Compose `Canvas` and animated natively with infinite transitions (spinning sunbeams, crescent moon twinkles, drifting clouds, and falling precipitation).
*   **📈 Bézier Curve Hourly Chart**: A horizontal scrollable `Canvas` chart drawing a smooth temperature trend line for the next 24 hours, featuring coordinates, temperature markers, and a glowing vertical gradient fill.
*   **🔍 Geocoding Autocomplete**: Input-debounced (400ms) search bar querying city details dynamically as you type.
*   **📍 Smart Geolocation**: Retrieve your local weather instantly with a single tap utilizing Android Location Services (permissions handled dynamically).
*   **🌡️ Unit Toggle**: Instant conversions of all temperature and speed values between Metric (°C / km/h) and Imperial (°F / mph).
*   **💾 Local State Persistence**: Remembers your temperature preferences and last searched city automatically using SharedPreferences.

---

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **UI Toolkit**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Networking & Parser**: OkHttp & Gson
*   **Location Services**: Google Play Services Fused Location Provider
*   **API Provider**: Open-Meteo API (Forecast & Geocoding)
*   **Build System**: Gradle 8.13 + Kotlin DSL + AGP 8.12.2

---

## 🚀 How to Run the Project

### Prerequisites
*   Android Studio (Ladybug or newer recommended)
*   JDK 17 or higher
*   Android SDK 24+ (minSdk) / SDK 36 (compileSdk)

### Getting Started

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-username/WeatherApp.git
    cd WeatherApp
    ```
2.  **Open in Android Studio**:
    *   Select **File > Open** and select the root directory of the project.
    *   Wait for Gradle to download dependencies and sync.
3.  **Run the App**:
    *   Set up an Android Virtual Device (AVD) or connect a physical phone with USB Debugging enabled.
    *   Click the **Run (Play)** button in the top toolbar.

---

## 📝 Project Structure

```text
app/src/main/java/com/example/weatherapp/
│
├── data/
│   ├── LocationHelper.kt      # Location Provider Client wrapper
│   ├── WeatherApi.kt          # Open-Meteo REST Client & JSON models
│   └── DataRepository.kt      # Template repository interface
│
├── ui/
│   └── main/
│       ├── MainScreen.kt           # Search overlay & details dashboard
│       ├── MainScreenViewModel.kt  # ViewModel (state & preference manager)
│       ├── WeatherBackground.kt    # Canvas Particle Engine (Backgrounds)
│       ├── WeatherIcons.kt         # Custom animated vector icons
│       └── WeatherChart.kt         # Horizontal scrollable Bezier chart
│
└── theme/
    └── Theme.kt               # App color schemes and Typography setup
```
