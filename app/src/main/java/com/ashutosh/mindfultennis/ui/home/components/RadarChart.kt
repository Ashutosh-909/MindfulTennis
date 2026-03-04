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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.ashutosh.mindfultennis.domain.model.Aspect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A spider-web / radar chart that plots aspect ratings on 8 axes.
 *
 * @param selfData   Self-rating averages per aspect (0–5). Empty map → no polygon.
 * @param partnerData Partner-feedback averages per aspect (0–5). Empty map → no polygon.
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
    // Start from top (–π/2)
    val startAngle = (-PI / 2).toFloat()

    val density = LocalDensity.current
    val labelSizePx = with(density) { 10.sp.toPx() }

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

        // --- 3. Draw data polygons ---
        if (selfData.isNotEmpty()) {
            drawDataPolygon(
                data = selfData,
                aspects = aspects,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                maxValue = maxValue,
                startAngle = startAngle,
                angleStep = angleStep,
                fillColor = selfColor.copy(alpha = 0.20f),
                strokeColor = selfColor,
            )
        }

        if (partnerData.isNotEmpty()) {
            drawDataPolygon(
                data = partnerData,
                aspects = aspects,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                maxValue = maxValue,
                startAngle = startAngle,
                angleStep = angleStep,
                fillColor = partnerColor.copy(alpha = 0.20f),
                strokeColor = partnerColor,
            )
        }

        // --- 4. Draw dot markers on vertices ---
        if (selfData.isNotEmpty()) {
            drawDotMarkers(selfData, aspects, centerX, centerY, radius, maxValue, startAngle, angleStep, selfColor)
        }
        if (partnerData.isNotEmpty()) {
            drawDotMarkers(partnerData, aspects, centerX, centerY, radius, maxValue, startAngle, angleStep, partnerColor)
        }

        // --- 5. Draw axis labels ---
        val labelPaint = android.graphics.Paint().apply {
            color = labelColor.hashCode()
            textSize = labelSizePx
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        for (i in aspects.indices) {
            val angle = startAngle + i * angleStep
            val labelRadius = radius * 1.18f
            val lx = centerX + labelRadius * cos(angle)
            val ly = centerY + labelRadius * sin(angle)

            // Adjust vertical alignment so labels don't overlap the chart
            val yOffset = labelSizePx / 3f
            drawContext.canvas.nativeCanvas.drawText(
                labelTexts[i],
                lx,
                ly + yOffset,
                labelPaint,
            )
        }
    }
}

private fun DrawScope.drawDataPolygon(
    data: Map<Aspect, Float>,
    aspects: List<Aspect>,
    centerX: Float,
    centerY: Float,
    radius: Float,
    maxValue: Float,
    startAngle: Float,
    angleStep: Float,
    fillColor: Color,
    strokeColor: Color,
) {
    val path = Path().apply {
        for (i in aspects.indices) {
            val value = (data[aspects[i]] ?: 0f).coerceIn(0f, maxValue)
            val fraction = value / maxValue
            val angle = startAngle + i * angleStep
            val x = centerX + radius * fraction * cos(angle)
            val y = centerY + radius * fraction * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = fillColor, style = Fill)
    drawPath(path = path, color = strokeColor, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawDotMarkers(
    data: Map<Aspect, Float>,
    aspects: List<Aspect>,
    centerX: Float,
    centerY: Float,
    radius: Float,
    maxValue: Float,
    startAngle: Float,
    angleStep: Float,
    color: Color,
) {
    for (i in aspects.indices) {
        val value = (data[aspects[i]] ?: 0f).coerceIn(0f, maxValue)
        val fraction = value / maxValue
        val angle = startAngle + i * angleStep
        val x = centerX + radius * fraction * cos(angle)
        val y = centerY + radius * fraction * sin(angle)
        drawCircle(color = color, radius = 4.5f, center = Offset(x, y))
    }
}
