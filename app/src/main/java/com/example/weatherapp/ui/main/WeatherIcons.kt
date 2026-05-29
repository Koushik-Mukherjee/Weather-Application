package com.example.weatherapp.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherIcon(
    weatherCode: Int,
    modifier: Modifier = Modifier,
    isDay: Boolean = true
) {
    val weatherType = getWeatherType(weatherCode)
    val infiniteTransition = rememberInfiniteTransition(label = "weatherIconTransition")

    // Define standard animations used across multiple icon components
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val drift by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val rainOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainOffset1"
    )

    val rainOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(300)
        ),
        label = "rainOffset2"
    )

    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0
                0f at 1800
                1f at 1900
                0f at 2000
                1f at 2100
                0f at 2200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lightningAlpha"
    )

    Canvas(modifier = modifier.size(100.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (weatherType) {
            WeatherType.SUNNY -> {
                if (isDay) {
                    // Draw Sun
                    val sunColor = Color(0xFFFFD54F)
                    val glowColor = Color(0xFFFFE082).copy(alpha = 0.4f)

                    // Draw outer glow pulse
                    drawCircle(color = glowColor, radius = cx * 0.5f * pulse, center = Offset(cx, cy))

                    // Draw inner sun disk
                    drawCircle(color = sunColor, radius = cx * 0.4f, center = Offset(cx, cy))

                    // Draw rotating sunbeams
                    rotate(rotation, pivot = Offset(cx, cy)) {
                        val numRays = 8
                        val rayLength = cx * 0.15f
                        val rayWidth = cx * 0.05f
                        for (i in 0 until numRays) {
                            val angleRad = Math.toRadians((i * (360f / numRays)).toDouble()).toFloat()
                            val startDist = cx * 0.48f
                            val startX = cx + cos(angleRad) * startDist
                            val startY = cy + sin(angleRad) * startDist
                            val endX = cx + cos(angleRad) * (startDist + rayLength)
                            val endY = cy + sin(angleRad) * (startDist + rayLength)

                            drawLine(
                                color = sunColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = rayWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                } else {
                    // Draw Moon for clear night
                    val moonColor = Color(0xFFECEFF1)
                    val glowColor = Color(0xFFECEFF1).copy(alpha = 0.15f)

                    drawCircle(color = glowColor, radius = cx * 0.45f * pulse, center = Offset(cx, cy))

                    // Draw crescent moon using clipping path or subtracting circles
                    val mainMoonPath = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(cx - cx * 0.35f, cy - cy * 0.35f, cx + cx * 0.35f, cy + cy * 0.35f))
                    }
                    val maskMoonPath = Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(cx - cx * 0.15f, cy - cy * 0.45f, cx + cx * 0.55f, cy + cy * 0.25f))
                    }
                    
                    // Subtract paths to get crescent
                    val crescentPath = Path.combine(
                        androidx.compose.ui.graphics.PathOperation.Difference,
                        mainMoonPath,
                        maskMoonPath
                    )
                    
                    drawPath(path = crescentPath, color = moonColor)

                    // Draw small twinkling stars
                    val starColor = Color.White.copy(alpha = pulse)
                    drawStar(cx - cx * 0.4f, cy - cy * 0.4f, cx * 0.08f, starColor, this)
                    drawStar(cx + cx * 0.35f, cy - cy * 0.35f, cx * 0.06f, starColor, this)
                }
            }

            WeatherType.CLOUDY -> {
                // If it is day, draw a sun behind the cloud, if night draw a moon
                if (isDay) {
                    drawSunBehindCloud(cx - cx * 0.15f, cy - cy * 0.2f, cx * 0.28f, rotation, pulse, this)
                } else {
                    drawMoonBehindCloud(cx - cx * 0.15f, cy - cy * 0.2f, cx * 0.25f, pulse, this)
                }

                // Draw main cloud in front
                drawCloud(
                    x = cx + drift * (w / 150f),
                    y = cy + h * 0.05f,
                    width = w * 0.65f,
                    height = h * 0.35f,
                    color = Color.White,
                    shadowColor = Color(0xFFCFD8DC),
                    drawScope = this
                )
            }

            WeatherType.RAINY -> {
                // Draw Cloud
                drawCloud(
                    x = cx,
                    y = cy - h * 0.1f,
                    width = w * 0.62f,
                    height = h * 0.32f,
                    color = Color(0xFFECEFF1),
                    shadowColor = Color(0xFFB0BEC5),
                    drawScope = this
                )

                // Draw animated rain drops falling
                val rainColor = Color(0xFF64B5F6)
                drawRaindrop(cx - w * 0.18f, cy + h * 0.12f + rainOffset1, h * 0.1f, rainOffset1 / 25f, rainColor, this)
                drawRaindrop(cx, cy + h * 0.15f + rainOffset2, h * 0.1f, rainOffset2 / 25f, rainColor, this)
                drawRaindrop(cx + w * 0.18f, cy + h * 0.12f + rainOffset1, h * 0.1f, rainOffset1 / 25f, rainColor, this)
            }

            WeatherType.SNOWY -> {
                // Draw Cloud
                drawCloud(
                    x = cx,
                    y = cy - h * 0.1f,
                    width = w * 0.62f,
                    height = h * 0.32f,
                    color = Color(0xFFECEFF1),
                    shadowColor = Color(0xFFCFD8DC),
                    drawScope = this
                )

                // Draw falling snowflakes
                val snowColor = Color.White
                drawSnowflake(cx - w * 0.18f, cy + h * 0.12f + rainOffset1, cx * 0.06f, rotation * 2f, rainOffset1 / 25f, snowColor, this)
                drawSnowflake(cx, cy + h * 0.15f + rainOffset2, cx * 0.06f, -rotation * 2f, rainOffset2 / 25f, snowColor, this)
                drawSnowflake(cx + w * 0.18f, cy + h * 0.12f + rainOffset1, cx * 0.06f, rotation * 2f, rainOffset1 / 25f, snowColor, this)
            }

            WeatherType.STORMY -> {
                // Draw dark storm cloud
                drawCloud(
                    x = cx,
                    y = cy - h * 0.12f,
                    width = w * 0.65f,
                    height = h * 0.35f,
                    color = Color(0xFF78909C),
                    shadowColor = Color(0xFF455A64),
                    drawScope = this
                )

                // Draw rain
                val rainColor = Color(0xFF90A4AE)
                drawRaindrop(cx - w * 0.15f, cy + h * 0.15f + rainOffset1, h * 0.08f, rainOffset1 / 25f, rainColor, this)
                drawRaindrop(cx + w * 0.15f, cy + h * 0.15f + rainOffset2, h * 0.08f, rainOffset2 / 25f, rainColor, this)

                // Draw flashing lightning bolt
                if (lightningAlpha > 0f) {
                    val lightningColor = Color(0xFFFFEB3B).copy(alpha = lightningAlpha)
                    drawLightning(cx - w * 0.02f, cy + h * 0.08f, w * 0.15f, h * 0.28f, lightningColor, this)
                }
            }
        }
    }
}

// Draw a star shape on canvas
private fun drawStar(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val path = Path().apply {
        moveTo(x, y - size)
        lineTo(x + size * 0.25f, y - size * 0.25f)
        lineTo(x + size, y)
        lineTo(x + size * 0.25f, y + size * 0.25f)
        lineTo(x, y + size)
        lineTo(x - size * 0.25f, y + size * 0.25f)
        lineTo(x - size, y)
        lineTo(x - size * 0.25f, y - size * 0.25f)
        close()
    }
    drawScope.drawPath(path = path, color = color)
}

// Draw a sun peeked behind a cloud
private fun drawSunBehindCloud(
    x: Float,
    y: Float,
    radius: Float,
    rotation: Float,
    pulse: Float,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val sunColor = Color(0xFFFFCA28)
    // Draw disk
    drawScope.drawCircle(color = sunColor, radius = radius, center = Offset(x, y))
    
    // Draw rotating rays
    drawScope.rotate(rotation, pivot = Offset(x, y)) {
        val numRays = 8
        val rayLength = radius * 0.4f
        val rayWidth = radius * 0.1f
        for (i in 0 until numRays) {
            val angleRad = Math.toRadians((i * (360f / numRays)).toDouble()).toFloat()
            val startDist = radius * 1.1f
            val startX = x + cos(angleRad) * startDist
            val startY = y + sin(angleRad) * startDist
            val endX = x + cos(angleRad) * (startDist + rayLength)
            val endY = y + sin(angleRad) * (startDist + rayLength)

            drawLine(
                color = sunColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = rayWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

// Draw a moon peeked behind a cloud
private fun drawMoonBehindCloud(
    x: Float,
    y: Float,
    radius: Float,
    pulse: Float,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val moonColor = Color(0xFFECEFF1)
    val mainMoonPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(x - radius, y - radius, x + radius, y + radius))
    }
    val maskMoonPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(x - radius * 0.4f, y - radius * 1.2f, x + radius * 1.6f, y + radius * 0.8f))
    }
    
    val crescentPath = Path.combine(
        androidx.compose.ui.graphics.PathOperation.Difference,
        mainMoonPath,
        maskMoonPath
    )
    
    drawScope.drawPath(path = crescentPath, color = moonColor)
}

// Draw a customizable cloud shape
private fun drawCloud(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color,
    shadowColor: Color,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    // A cloud is drawn with a base rounded rectangle and 3 overlapping circles
    val left = x - width / 2f
    val top = y - height / 2f
    
    // Draw cloud shadow/bottom part for depth
    drawScope.drawRoundRect(
        color = shadowColor,
        topLeft = Offset(left, top + height * 0.2f),
        size = Size(width, height * 0.8f),
        cornerRadius = CornerRadius(height * 0.4f)
    )
    drawScope.drawCircle(
        color = shadowColor,
        radius = height * 0.5f,
        center = Offset(left + width * 0.3f, top + height * 0.3f)
    )
    drawScope.drawCircle(
        color = shadowColor,
        radius = height * 0.6f,
        center = Offset(left + width * 0.6f, top + height * 0.25f)
    )

    // Draw main cloud body
    drawScope.drawRoundRect(
        color = color,
        topLeft = Offset(left, top + height * 0.25f),
        size = Size(width, height * 0.75f),
        cornerRadius = CornerRadius(height * 0.38f)
    )
    drawScope.drawCircle(
        color = color,
        radius = height * 0.48f,
        center = Offset(left + width * 0.3f, top + height * 0.35f)
    )
    drawScope.drawCircle(
        color = color,
        radius = height * 0.58f,
        center = Offset(left + width * 0.6f, top + height * 0.3f)
    )
}

// Draw a raindrop that fades out as it falls
private fun drawRaindrop(
    x: Float,
    y: Float,
    length: Float,
    progress: Float,
    color: Color,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    drawScope.drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(x, y),
        end = Offset(x - length * 0.3f, y + length),
        strokeWidth = 3.dp.toPx(drawScope.density),
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
}

// Helper to convert Dp to Px in drawscope
private fun androidx.compose.ui.unit.Dp.toPx(density: Float): Float = value * density

// Draw snowflake (drawn as small cross/asterisk bars)
private fun drawSnowflake(
    x: Float,
    y: Float,
    size: Float,
    rotation: Float,
    progress: Float,
    color: Color,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    drawScope.withTransform({
        rotate(rotation, pivot = Offset(x, y))
    }) {
        val stroke = 2.dp.toPx(density)
        // Draw 3 crossing lines
        drawLine(color = color.copy(alpha = alpha), start = Offset(x - size, y), end = Offset(x + size, y), strokeWidth = stroke)
        drawLine(color = color.copy(alpha = alpha), start = Offset(x - size * 0.7f, y - size * 0.7f), end = Offset(x + size * 0.7f, y + size * 0.7f), strokeWidth = stroke)
        drawLine(color = color.copy(alpha = alpha), start = Offset(x - size * 0.7f, y + size * 0.7f), end = Offset(x + size * 0.7f, y - size * 0.7f), strokeWidth = stroke)
    }
}

// Draw lightning bolt
private fun drawLightning(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val path = Path().apply {
        moveTo(x + width * 0.6f, y)
        lineTo(x, y + height * 0.5f)
        lineTo(x + width * 0.45f, y + height * 0.5f)
        lineTo(x + width * 0.2f, y + height)
        lineTo(x + width, y + height * 0.45f)
        lineTo(x + width * 0.55f, y + height * 0.45f)
        close()
    }
    drawScope.drawPath(path = path, color = color)
}
