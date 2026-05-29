package com.example.weatherapp.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// Weather condition category enum
enum class WeatherType {
    SUNNY, CLOUDY, RAINY, SNOWY, STORMY
}

fun getWeatherType(code: Int): WeatherType {
    return when (code) {
        0, 1 -> WeatherType.SUNNY
        2, 3, 45, 48 -> WeatherType.CLOUDY
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherType.RAINY
        71, 73, 75, 77, 85, 86 -> WeatherType.SNOWY
        95, 96, 99 -> WeatherType.STORMY
        else -> WeatherType.SUNNY
    }
}

// Particle representation
private class WeatherParticle(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var size: Float,
    var alpha: Float,
    var angle: Float = 0f,
    var sizeGrowth: Float = 0f // Used for ripples
)

@Composable
fun WeatherBackground(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val weatherType = remember(weatherCode) { getWeatherType(weatherCode) }

    // --- Weather Colors ---
    val (topColor, bottomColor) = when (weatherType) {
        WeatherType.SUNNY -> {
            if (isDay) {
                Color(0xFF4A90E2) to Color(0xFF50E3C2)
            } else {
                Color(0xFF0F2027) to Color(0xFF203A43)
            }
        }
        WeatherType.CLOUDY -> {
            if (isDay) {
                Color(0xFF757F9A) to Color(0xFFD7DDE8)
            } else {
                Color(0xFF1F1C2C) to Color(0xFF928DAB)
            }
        }
        WeatherType.RAINY -> {
            if (isDay) {
                Color(0xFF3E5151) to Color(0xFFDECBA4)
            } else {
                Color(0xFF141E30) to Color(0xFF243B55)
            }
        }
        WeatherType.SNOWY -> {
            if (isDay) {
                Color(0xFF8A939E) to Color(0xFFE9ECEF)
            } else {
                Color(0xFF111936) to Color(0xFF2A3A5C)
            }
        }
        WeatherType.STORMY -> {
            Color(0xFF0F0C20) to Color(0xFF2F2F3B)
        }
    }

    // Animate background colors for a smooth weather morph
    val animatedTopColor by animateColorAsState(topColor, animationSpec = tween(1500), label = "topColor")
    val animatedBottomColor by animateColorAsState(bottomColor, animationSpec = tween(1500), label = "bottomColor")

    val gradient = Brush.verticalGradient(
        colors = listOf(animatedTopColor, animatedBottomColor)
    )

    // Lightning Flash state for storms
    var lightningAlpha by remember { mutableFloatStateOf(0f) }
    if (weatherType == WeatherType.STORMY) {
        LaunchedEffect(Unit) {
            while (isActive) {
                // Wait for a random interval
                delay((5000..12000).random().toLong())
                // Flash 1
                lightningAlpha = 0.6f
                delay(80)
                lightningAlpha = 0f
                delay(50)
                // Flash 2 (Double strike)
                lightningAlpha = 0.8f
                delay(120)
                lightningAlpha = 0f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        val density = LocalDensity.current
        var width by remember { mutableStateOf(0f) }
        var height by remember { mutableStateOf(0f) }

        // Setup particles list
        val particles = remember(weatherType) { mutableStateListOf<WeatherParticle>() }
        
        // Setup initial clouds for cloudy background
        val clouds = remember(weatherType) {
            if (weatherType == WeatherType.CLOUDY) {
                List(3) { index ->
                    WeatherParticle(
                        x = Random.nextFloat() * 1000f,
                        y = 100f + index * 120f,
                        speedX = 0.2f + Random.nextFloat() * 0.3f,
                        speedY = 0f,
                        size = 150f + Random.nextFloat() * 100f,
                        alpha = 0.15f + Random.nextFloat() * 0.15f
                    )
                }
            } else emptyList()
        }

        // Setup ticking framework for frame animations
        var lastTime by remember { mutableLongStateOf(0L) }
        val triggerFrame = remember { mutableStateOf(0) }

        LaunchedEffect(weatherType) {
            lastTime = System.currentTimeMillis()
            while (isActive) {
                withFrameMillis {
                    triggerFrame.value = triggerFrame.value + 1
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            width = size.width
            height = size.height

            val now = System.currentTimeMillis()
            val dt = ((now - lastTime) / 16.6f).coerceIn(0.1f, 3.0f) // Normalized time step (~60fps)
            lastTime = now

            // Initialize particles once screen dimensions are resolved
            if (width > 0 && particles.isEmpty()) {
                val maxParticles = when (weatherType) {
                    WeatherType.SUNNY -> 15 // Sunbeams/dust particles
                    WeatherType.CLOUDY -> 0  // Cloud particles drawn below
                    WeatherType.RAINY -> 80 // Raindrops
                    WeatherType.SNOWY -> 60 // Snowflakes
                    WeatherType.STORMY -> 90 // Rain + Storm drops
                }
                repeat(maxParticles) {
                    particles.add(createParticle(weatherType, width, height, isInitial = true))
                }
            }

            // --- Draw Ambient Effects depending on weatherType ---
            when (weatherType) {
                WeatherType.SUNNY -> {
                    // Draw a subtle sun glow in top right
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = if (isDay) 0.15f else 0.05f),
                        radius = width * 0.4f,
                        center = Offset(width * 0.85f, height * 0.1f)
                    )
                    drawCircle(
                        color = Color(0xFFFFE57F).copy(alpha = if (isDay) 0.25f else 0.08f),
                        radius = width * 0.2f,
                        center = Offset(width * 0.85f, height * 0.1f)
                    )

                    // Update & Draw sunny light dust particles
                    particles.forEach { p ->
                        p.y -= p.speedY * dt
                        p.x += p.speedX * dt
                        p.angle += 0.01f * dt
                        p.alpha = (sin(p.angle) * 0.2f + 0.3f).coerceIn(0.05f, 0.5f)

                        // Boundary reset
                        if (p.y < 0 || p.x < 0 || p.x > width) {
                            p.y = height + 10f
                            p.x = Random.nextFloat() * width
                        }

                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha),
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
                WeatherType.CLOUDY -> {
                    // Update and Draw floating soft clouds
                    clouds.forEach { c ->
                        c.x += c.speedX * dt
                        if (c.x - c.size > width) {
                            c.x = -c.size
                        }

                        // Draw overlapping circles for a fluffy cloud feel
                        val cloudColor = Color.White.copy(alpha = c.alpha)
                        drawCircle(color = cloudColor, radius = c.size, center = Offset(c.x, c.y))
                        drawCircle(color = cloudColor, radius = c.size * 0.8f, center = Offset(c.x - c.size * 0.5f, c.y + c.size * 0.1f))
                        drawCircle(color = cloudColor, radius = c.size * 0.8f, center = Offset(c.x + c.size * 0.5f, c.y + c.size * 0.1f))
                    }
                }
                WeatherType.RAINY, WeatherType.STORMY -> {
                    // Update and Draw falling rain streaks and splashing ripples
                    particles.forEach { p ->
                        if (p.sizeGrowth > 0f) {
                            // This is a splash ripple at the bottom
                            p.size += p.sizeGrowth * dt
                            p.alpha -= 0.04f * dt

                            if (p.alpha <= 0f) {
                                // Reset to falling rain particle at the top
                                val fresh = createParticle(weatherType, width, height, isInitial = false)
                                p.x = fresh.x
                                p.y = fresh.y
                                p.speedX = fresh.speedX
                                p.speedY = fresh.speedY
                                p.size = fresh.size
                                p.alpha = fresh.alpha
                                p.sizeGrowth = 0f
                            } else {
                                // Draw splash oval
                                drawOvalRipple(p.x, p.y, p.size, p.alpha, this)
                            }
                        } else {
                            // Standard falling raindrop
                            p.y += p.speedY * dt
                            p.x += p.speedX * dt

                            // Check collision with the floor
                            val floor = height * 0.95f
                            if (p.y >= floor) {
                                p.y = floor
                                p.alpha = 0.6f
                                p.size = 2f
                                p.sizeGrowth = 1.2f + Random.nextFloat() * 1.5f // Convert to splash ripple
                            } else {
                                // Draw rain line
                                val lineLength = p.size * 5f
                                val endX = p.x + (p.speedX / p.speedY) * lineLength
                                val endY = p.y + lineLength
                                drawLine(
                                    color = Color.White.copy(alpha = p.alpha),
                                    start = Offset(p.x, p.y),
                                    end = Offset(endX, endY),
                                    strokeWidth = p.size
                                )
                            }
                        }
                    }
                }
                WeatherType.SNOWY -> {
                    // Update and Draw falling and swaying snowflakes
                    particles.forEach { p ->
                        p.y += p.speedY * dt
                        p.angle += 0.03f * dt
                        p.x += (p.speedX + sin(p.angle) * 0.8f) * dt

                        // Boundary reset
                        if (p.y > height) {
                            p.y = -10f
                            p.x = Random.nextFloat() * width
                        }

                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha),
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }

            // Draw Stormy Lightning Overlay
            if (weatherType == WeatherType.STORMY && lightningAlpha > 0f) {
                drawRect(
                    color = Color(0xFFE3E0FF).copy(alpha = lightningAlpha),
                    size = this.size
                )
            }
        }
    }
}

private fun createParticle(
    weatherType: WeatherType,
    width: Float,
    height: Float,
    isInitial: Boolean
): WeatherParticle {
    val startY = if (isInitial) Random.nextFloat() * height else -15f
    val startX = Random.nextFloat() * width

    return when (weatherType) {
        WeatherType.SUNNY -> {
            WeatherParticle(
                x = startX,
                y = startY,
                speedX = -0.1f + Random.nextFloat() * 0.2f,
                speedY = 0.1f + Random.nextFloat() * 0.3f,
                size = 3f + Random.nextFloat() * 5f,
                alpha = 0.1f + Random.nextFloat() * 0.3f,
                angle = Random.nextFloat() * 10f
            )
        }
        WeatherType.RAINY, WeatherType.STORMY -> {
            val speedY = 15f + Random.nextFloat() * 12f
            val size = 1.5f + Random.nextFloat() * 2f
            WeatherParticle(
                x = startX,
                y = startY,
                speedX = -1.5f - Random.nextFloat() * 2f, // Left-drifting rain
                speedY = speedY,
                size = size,
                alpha = 0.25f + Random.nextFloat() * 0.35f
            )
        }
        WeatherType.SNOWY -> {
            val speedY = 1.5f + Random.nextFloat() * 2.5f
            val size = 3f + Random.nextFloat() * 6f
            WeatherParticle(
                x = startX,
                y = startY,
                speedX = -0.3f + Random.nextFloat() * 0.6f,
                speedY = speedY,
                size = size,
                alpha = 0.3f + Random.nextFloat() * 0.5f,
                angle = Random.nextFloat() * 6.28f
            )
        }
        else -> WeatherParticle(0f, 0f, 0f, 0f, 0f, 0f)
    }
}

private fun drawOvalRipple(
    x: Float,
    y: Float,
    radius: Float,
    alpha: Float,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val path = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                left = x - radius * 2f,
                top = y - radius * 0.5f,
                right = x + radius * 2f,
                bottom = y + radius * 0.5f
            )
        )
    }
    drawScope.drawPath(
        path = path,
        color = Color.White.copy(alpha = alpha),
        style = Stroke(width = 1.5f)
    )
}
