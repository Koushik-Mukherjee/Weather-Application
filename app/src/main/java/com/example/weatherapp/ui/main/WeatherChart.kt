package com.example.weatherapp.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HourlyForecastChart(
    times: List<String>,
    temps: List<Double>,
    weatherCodes: List<Int>,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    // Take only the first 24 hours for the chart
    val limit = 24
    val displayTimes = remember(times) { times.take(limit) }
    val displayTemps = remember(temps) { temps.take(limit) }
    val displayCodes = remember(weatherCodes) { weatherCodes.take(limit) }

    if (displayTimes.isEmpty()) return

    val density = LocalDensity.current
    val itemWidth = 70.dp
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val chartHeight = 120.dp
    val chartHeightPx = with(density) { chartHeight.toPx() }
    
    val textMeasurer = rememberTextMeasurer()
    val scrollState = rememberScrollState()

    val minTemp = remember(displayTemps) { displayTemps.minOrNull() ?: 0.0 }
    val maxTemp = remember(displayTemps) { displayTemps.maxOrNull() ?: 100.0 }
    val tempRange = remember(minTemp, maxTemp) { 
        val r = maxTemp - minTemp
        if (r == 0.0) 1.0 else r 
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        val totalWidth = itemWidth * limit

        // 1. Draw the continuous curve background first
        Canvas(
            modifier = Modifier
                .width(totalWidth)
                .height(chartHeight + 80.dp) // Extra height for text padding
                .align(Alignment.BottomStart)
        ) {
            val points = displayTemps.mapIndexed { index, temp ->
                val x = index * itemWidthPx + itemWidthPx / 2f
                // Map temp to Y: leave 20dp padding top/bottom inside the chartHeight area
                val padding = 20.dp.toPx()
                val innerHeight = chartHeightPx - 2 * padding
                val y = chartHeightPx - padding - ((temp - minTemp) / tempRange).toFloat() * innerHeight + 40.dp.toPx()
                Offset(x, y)
            }

            // Create Bezier Path
            val curvePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx1 = p0.x + (p1.x - p0.x) / 2f
                        val cy1 = p0.y
                        val cx2 = p0.x + (p1.x - p0.x) / 2f
                        val cy2 = p1.y
                        cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                    }
                }
            }

            // Draw fading area under the curve
            val fillPath = Path().apply {
                if (points.isNotEmpty()) {
                    addPath(curvePath)
                    lineTo(points.last().x, chartHeightPx + 40.dp.toPx())
                    lineTo(points.first().x, chartHeightPx + 40.dp.toPx())
                    close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    startY = points.minOfOrNull { it.y } ?: 0f,
                    endY = chartHeightPx + 40.dp.toPx()
                )
            )

            // Draw the curve line
            drawPath(
                path = curvePath,
                color = Color.White,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw points & temperature labels
            points.forEachIndexed { index, point ->
                val tempVal = displayTemps[index]
                val formattedTemp = if (isFahrenheit) {
                    ((tempVal * 9 / 5) + 32).toInt()
                } else {
                    tempVal.toInt()
                }

                // Draw point dot
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color(0xFF64B5F6),
                    radius = 3.dp.toPx(),
                    center = point
                )

                // Measure & Draw Temp Text above dot
                val tempText = "$formattedTemp°"
                val textLayoutResult = textMeasurer.measure(
                    text = tempText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    color = Color.White,
                    topLeft = Offset(
                        point.x - textLayoutResult.size.width / 2f,
                        point.y - textLayoutResult.size.height - 6.dp.toPx()
                    )
                )
            }
        }

        // 2. Lay out standard Compose elements overlay (Time & Icons)
        Row(
            modifier = Modifier.width(totalWidth)
        ) {
            displayTimes.forEachIndexed { index, timeStr ->
                val formattedTime = remember(timeStr) {
                    try {
                        val localDateTime = LocalDateTime.parse(timeStr)
                        val hour = localDateTime.hour
                        when {
                            hour == 0 -> "12 AM"
                            hour == 12 -> "12 PM"
                            hour > 12 -> "${hour - 12} PM"
                            else -> "$hour AM"
                        }
                    } catch (e: Exception) {
                        timeStr.substringAfter("T").take(5)
                    }
                }

                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time Label
                    Text(
                        text = formattedTime,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Custom animated weather icon
                    WeatherIcon(
                        weatherCode = displayCodes[index],
                        modifier = Modifier.size(36.dp),
                        isDay = remember(timeStr) {
                            try {
                                val hour = LocalDateTime.parse(timeStr).hour
                                hour in 6..18
                            } catch (e: Exception) {
                                true
                            }
                        }
                    )

                    // Spacer containing the canvas curve height
                    Spacer(modifier = Modifier.height(chartHeight))

                    // Extra weather detail: e.g. wind speed
                    Text(
                        text = "${displayTemps[index].toInt()}°", // Extra reference or placeholder
                        color = Color.Transparent, // Invisible, acts as spacer padding for labels
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
