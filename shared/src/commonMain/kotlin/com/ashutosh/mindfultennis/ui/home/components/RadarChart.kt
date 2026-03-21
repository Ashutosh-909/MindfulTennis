package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.ashutosh.mindfultennis.domain.model.Aspect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A spider-web / radar chart that plots aspect ratings on 8 axes.
 *
 * @param selfData   Self-rating averages per aspect (0-5). Empty map -> no polygon.
 * @param partnerData Partner-feedback averages per aspect (0-5). Empty map -> no polygon.
 * @param selfColor  Color for the self polygon fill + stroke.
 * @param partnerColor Color for the partner polygon fill + stroke.
 * @param maxValue   Maximum value on each axis (default 5 for 5-star scale).
 * @param gridLevels Number of concentric grid rings (default 5).
 */
@Composable
fun RadarChart(
    selfData: Map<Aspect, Float>,
    partnerData: Map<Aspect, Float>,
    modifier: Modifier = Modifier,
    selfColor: Color = MaterialTheme.colorScheme.primary,
    partnerColor: Color = MaterialTheme.colorScheme.tertiary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    maxValue: Float = 5f,
    gridLevels: Int = 5,
) {
    val aspects = remember { Aspect.entries.toList() }
    val axisCount = aspects.size
    val angleStep = (2 * PI / axisCount).toFloat()
    // Start from top (-PI/2)
    val startAngle = (-PI / 2).toFloat()

    val textMeasurer = rememberTextMeasurer()

    val labelTexts = remember {
        aspects.map { aspect ->
            aspect.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        // Leave margin for labels
        val radius = (size.minDimension / 2f) * 0.72f

        // --- 1. Draw grid rings ---
        for (level in 1..gridLevels) {
            val fraction = level.toFloat() / gridLevels
            val r = radius * fraction
            val path = Path().apply {
                for (i in 0 until axisCount) {
                    val angle = startAngle + i * angleStep
                    val x = centerX + r * cos(angle)
                    val y = centerY + r * sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(
                path = path,
                color = gridColor,
                style = Stroke(width = 1f),
            )
        }

        // --- 2. Draw axis spokes ---
        for (i in 0 until axisCount) {
            val angle = startAngle + i * angleStep
            drawLine(
                color = gridColor,
                start = Offset(centerX, centerY),
                end = Offset(
                    centerX + radius * cos(angle),
                    centerY + radius * sin(angle),
                ),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
        }

        // --- 3. Draw color-coded data polygons ---
        if (selfData.isNotEmpty()) {
            drawColorCodedPolygon(
                data = selfData,
                aspects = aspects,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                maxValue = maxValue,
                startAngle = startAngle,
                angleStep = angleStep,
                baseColor = selfColor,
            )
        }

        if (partnerData.isNotEmpty()) {
            drawColorCodedPolygon(
                data = partnerData,
                aspects = aspects,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                maxValue = maxValue,
                startAngle = startAngle,
                angleStep = angleStep,
                baseColor = partnerColor,
            )
        }

        // --- 4. Draw color-coded dot markers on vertices ---
        if (selfData.isNotEmpty()) {
            drawColorCodedDotMarkers(selfData, aspects, centerX, centerY, radius, maxValue, startAngle, angleStep)
        }
        if (partnerData.isNotEmpty()) {
            drawColorCodedDotMarkers(partnerData, aspects, centerX, centerY, radius, maxValue, startAngle, angleStep)
        }

        // --- 5. Draw axis labels using TextMeasurer ---
        for (i in aspects.indices) {
            val angle = startAngle + i * angleStep
            val labelRadius = radius * 1.18f
            val lx = centerX + labelRadius * cos(angle)
            val ly = centerY + labelRadius * sin(angle)

            val measuredText = textMeasurer.measure(
                labelTexts[i],
                TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal),
            )
            drawText(
                measuredText,
                color = labelColor,
                topLeft = Offset(
                    lx - measuredText.size.width / 2f,
                    ly - measuredText.size.height / 2f,
                ),
            )
        }
    }
}

/**
 * Maps a rating value (0-maxValue) to a color on a red -> amber -> green gradient.
 * 0 -> red, ~40% -> amber/orange, ~70%+ -> green.
 */
private fun performanceColor(value: Float, maxValue: Float): Color {
    val fraction = (value / maxValue).coerceIn(0f, 1f)
    val red = Color(0xFFE53935)    // low
    val amber = Color(0xFFFFA726)  // medium
    val green = Color(0xFF43A047)  // high
    return when {
        fraction < 0.5f -> lerp(red, amber, fraction / 0.5f)
        else -> lerp(amber, green, (fraction - 0.5f) / 0.5f)
    }
}

/**
 * Draws triangular segments from the center to each pair of adjacent data
 * vertices, each filled with a performance-based color and outlined with
 * [baseColor] for series identity.
 */
private fun DrawScope.drawColorCodedPolygon(
    data: Map<Aspect, Float>,
    aspects: List<Aspect>,
    centerX: Float,
    centerY: Float,
    radius: Float,
    maxValue: Float,
    startAngle: Float,
    angleStep: Float,
    baseColor: Color,
) {
    val center = Offset(centerX, centerY)
    val points = aspects.mapIndexed { i, aspect ->
        val value = (data[aspect] ?: 0f).coerceIn(0f, maxValue)
        val fraction = value / maxValue
        val angle = startAngle + i * angleStep
        Triple(
            Offset(centerX + radius * fraction * cos(angle), centerY + radius * fraction * sin(angle)),
            value,
            fraction,
        )
    }

    // Draw color-coded filled segments (triangle: center -> vertex i -> vertex i+1)
    for (i in points.indices) {
        val j = (i + 1) % points.size
        val avgValue = (points[i].second + points[j].second) / 2f
        val segmentColor = performanceColor(avgValue, maxValue)

        val segmentPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(points[i].first.x, points[i].first.y)
            lineTo(points[j].first.x, points[j].first.y)
            close()
        }
        drawPath(path = segmentPath, color = segmentColor.copy(alpha = 0.28f), style = Fill)
    }

    // Draw the outer polygon stroke for series identity
    val outlinePath = Path().apply {
        for (i in points.indices) {
            if (i == 0) moveTo(points[i].first.x, points[i].first.y)
            else lineTo(points[i].first.x, points[i].first.y)
        }
        close()
    }
    drawPath(path = outlinePath, color = baseColor, style = Stroke(width = 2.5f))
}

/**
 * Draws dot markers at each data vertex, colored by performance level.
 */
private fun DrawScope.drawColorCodedDotMarkers(
    data: Map<Aspect, Float>,
    aspects: List<Aspect>,
    centerX: Float,
    centerY: Float,
    radius: Float,
    maxValue: Float,
    startAngle: Float,
    angleStep: Float,
) {
    for (i in aspects.indices) {
        val value = (data[aspects[i]] ?: 0f).coerceIn(0f, maxValue)
        val fraction = value / maxValue
        val angle = startAngle + i * angleStep
        val x = centerX + radius * fraction * cos(angle)
        val y = centerY + radius * fraction * sin(angle)
        val dotColor = performanceColor(value, maxValue)
        drawCircle(color = dotColor, radius = 5f, center = Offset(x, y))
        // White center for better visibility
        drawCircle(color = Color.White, radius = 2f, center = Offset(x, y))
    }
}
