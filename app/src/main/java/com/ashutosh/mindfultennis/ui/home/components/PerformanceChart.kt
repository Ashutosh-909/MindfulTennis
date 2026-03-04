package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.PerformanceTrend
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils

/**
 * Performance trend line chart card for the dashboard.
 * Shows overall score trends over the selected duration.
 */
@Composable
fun PerformanceChart(
    trend: List<PerformanceTrend>,
    selectedDuration: DurationFilter,
    onDurationSelected: (DurationFilter) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = "Performance Trend",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            DurationFilterChips(
                selectedDuration = selectedDuration,
                onDurationSelected = onDurationSelected,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            when {
                isLoading -> {
                    LoadingShimmer(height = 160.dp, barCount = 0)
                }
                error != null -> {
                    ErrorRetryCard(message = error, onRetry = onRetry)
                }
                trend.isEmpty() -> {
                    EmptyChartPlaceholder()
                }
                else -> {
                    TrendLineChart(trend = trend)
                }
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Log your first session to see trends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrendLineChart(
    trend: List<PerformanceTrend>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    // Build accessibility description
    val avgScore = remember(trend) {
        if (trend.isEmpty()) 0 else trend.map { it.overallScore }.average().toInt()
    }
    val trendDirection = remember(trend) {
        if (trend.size < 2) "stable"
        else {
            val firstHalf = trend.take(trend.size / 2).map { it.overallScore }.average()
            val secondHalf = trend.drop(trend.size / 2).map { it.overallScore }.average()
            when {
                secondHalf > firstHalf + 5 -> "trending up"
                secondHalf < firstHalf - 5 -> "trending down"
                else -> "stable"
            }
        }
    }
    val chartDescription = "Performance $trendDirection, average score $avgScore"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = chartDescription },
    ) {
        val paddingLeft = 40f
        val paddingBottom = 32f
        val paddingTop = 16f
        val paddingRight = 16f

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        // Draw Y-axis grid lines and labels (0, 25, 50, 75, 100)
        val yLabels = listOf(0, 25, 50, 75, 100)
        yLabels.forEach { value ->
            val y = paddingTop + chartHeight * (1f - value / 100f)

            // Grid line
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 1f,
            )

            // Y-axis label
            val textResult = textMeasurer.measure(
                text = value.toString(),
                style = TextStyle(
                    fontSize = labelStyle.fontSize,
                    color = labelColor,
                ),
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = paddingLeft - textResult.size.width - 8f,
                    y = y - textResult.size.height / 2f,
                ),
            )
        }

        if (trend.size == 1) {
            // Single point — draw as a dot
            val point = trend.first()
            val x = paddingLeft + chartWidth / 2f
            val y = paddingTop + chartHeight * (1f - point.overallScore / 100f)
            drawCircle(color = dotColor, radius = 6f, center = Offset(x, y))
            return@Canvas
        }

        // Plot line
        val minDate = trend.first().date.toFloat()
        val maxDate = trend.last().date.toFloat()
        val dateRange = (maxDate - minDate).coerceAtLeast(1f)

        val points = trend.map { point ->
            val x = paddingLeft + chartWidth * ((point.date - minDate) / dateRange)
            val y = paddingTop + chartHeight * (1f - point.overallScore / 100f)
            Offset(x, y)
        }

        // Draw line path
        val path = Path().apply {
            points.forEachIndexed { index, offset ->
                if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Draw dots
        points.forEach { offset ->
            drawCircle(color = Color.White, radius = 5f, center = offset)
            drawCircle(color = dotColor, radius = 4f, center = offset)
        }

        // Draw X-axis date labels (first and last)
        if (trend.size >= 2) {
            val firstLabel = textMeasurer.measure(
                text = DateTimeUtils.formatDate(trend.first().date),
                style = TextStyle(fontSize = labelStyle.fontSize, color = labelColor),
            )
            drawText(
                textLayoutResult = firstLabel,
                topLeft = Offset(paddingLeft, size.height - paddingBottom + 4f),
            )

            val lastLabel = textMeasurer.measure(
                text = DateTimeUtils.formatDate(trend.last().date),
                style = TextStyle(fontSize = labelStyle.fontSize, color = labelColor),
            )
            drawText(
                textLayoutResult = lastLabel,
                topLeft = Offset(
                    size.width - paddingRight - lastLabel.size.width,
                    size.height - paddingBottom + 4f,
                ),
            )
        }
    }
}
