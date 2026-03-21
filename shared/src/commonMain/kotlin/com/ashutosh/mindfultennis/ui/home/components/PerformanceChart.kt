package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.PerformanceTrend
import com.ashutosh.mindfultennis.ui.components.ErrorRetryCard
import com.ashutosh.mindfultennis.ui.components.LoadingShimmer
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils

// -- Semantic accent colors (same in both light and dark themes) --
private val KpiDeltaUp = Color(0xFF4CAF50)
private val KpiDeltaDown = Color(0xFFF44336)
private val BestColor = Color(0xFF4CAF50)
private val LowColor = Color(0xFFF44336)

// -- Computed KPI helper --
private data class TrendKpi(
    val currentScore: Int?,
    val periodAverage: Float?,
    val delta: Float?,
    val lastUpdatedMs: Long?,
    val bestScore: Int?,
    val bestIndex: Int,
    val lowScore: Int?,
    val lowIndex: Int,
) {
    companion object {
        fun compute(trend: List<PerformanceTrend>): TrendKpi {
            if (trend.isEmpty()) {
                return TrendKpi(null, null, null, null, null, -1, null, -1)
            }
            val scores = trend.map { it.overallScore }
            val current = scores.last()
            val avg = scores.average().toFloat()
            val lastDate = trend.last().date

            val delta = if (trend.size >= 4) {
                val mid = trend.size / 2
                val firstHalfAvg = scores.take(mid).average()
                val secondHalfAvg = scores.drop(mid).average()
                (secondHalfAvg - firstHalfAvg).toFloat()
            } else {
                null
            }

            val best = scores.max()
            val bestIdx = scores.indexOf(best)
            val low = scores.min()
            val lowIdx = scores.indexOf(low)

            return TrendKpi(current, avg, delta, lastDate, best, bestIdx, low, lowIdx)
        }
    }
}

// -- Public composable (same signature -- drop-in replacement) --

/**
 * Performance Trend hero card.
 * Dark-themed card showing KPI summary, time-range selector,
 * and annotated line chart with min/max callouts.
 */
@Composable
fun PerformanceChart(
    trend: List<PerformanceTrend>,
    selectedDuration: DurationFilter,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kpi = remember(trend) { TrendKpi.compute(trend) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {

            // -- KPI strip --
            if (kpi.currentScore != null) {
                KpiStrip(kpi = kpi, selectedDuration = selectedDuration)
                Spacer(Modifier.height(Spacing.md))
            }


            // -- Chart body --
            when {
                isLoading -> LoadingShimmer(height = 200.dp, barCount = 0)
                error != null -> ErrorRetryCard(message = error, onRetry = onRetry)
                trend.isEmpty() -> EmptyChartPlaceholder()
                else -> TrendLineChart(trend = trend, kpi = kpi)
            }

            // -- Footnote --
            if (trend.isNotEmpty() && kpi.currentScore != null) {
                Spacer(Modifier.height(Spacing.sm))
                TrendFootnote(selectedDuration = selectedDuration)
            }
        }
    }
}

// -- KPI Strip --

@Composable
private fun KpiStrip(
    kpi: TrendKpi,
    selectedDuration: DurationFilter,
    modifier: Modifier = Modifier,
) {
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val deltaColor = when {
        kpi.delta == null -> subtitleColor
        kpi.delta >= 0f -> KpiDeltaUp
        else -> KpiDeltaDown
    }
    val arrow = when {
        kpi.delta == null -> ""
        kpi.delta >= 0f -> "\u25B2"
        else -> "\u25BC"
    }

    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = onSurfaceColor, fontWeight = FontWeight.Normal, fontSize = 14.sp)) {
                append("Latest Performance: ")
            }
            withStyle(SpanStyle(color = onSurfaceColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                append("${kpi.currentScore}")
            }
            if (kpi.delta != null) {
                append("  ")
                withStyle(SpanStyle(color = deltaColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)) {
                    append("$arrow${if (kpi.delta >= 0) "+" else ""}${formatOneDecimal(kpi.delta)}")
                }
                withStyle(SpanStyle(color = subtitleColor, fontSize = 12.sp)) {
                    append(" vs previous ${selectedDuration.label}")
                }
            }
            if (kpi.periodAverage != null) {
                append("  ")
                withStyle(SpanStyle(color = subtitleColor, fontSize = 12.sp)) {
                    append("(Avg ${selectedDuration.label}: ${formatOneDecimal(kpi.periodAverage)})")
                }
            }
        },
    )
}

// -- Empty placeholder --

@Composable
private fun EmptyChartPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Log your first session to see trends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -- Annotated line chart (Canvas) --

@Composable
private fun TrendLineChart(
    trend: List<PerformanceTrend>,
    kpi: TrendKpi,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    // Resolve theme colors for Canvas (non-composable scope)
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val chartLineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Accessibility
    val avgScore = remember(trend) {
        if (trend.isEmpty()) 0 else trend.map { it.overallScore }.average().toInt()
    }
    val trendDir = remember(trend) {
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
    val a11yDesc = "Performance $trendDir, average score $avgScore"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = a11yDesc },
    ) {
        val padLeft = 42f
        val padRight = 56f   // room for right-side annotations
        val padTop = 28f
        val padBottom = 36f

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom

        // -- Grid lines & Y-axis labels --
        val yTicks = listOf(0, 25, 50, 75, 100)
        yTicks.forEach { value ->
            val y = padTop + chartH * (1f - value / 100f)
            drawLine(gridLineColor, Offset(padLeft, y), Offset(padLeft + chartW, y), 1f)

            val label = textMeasurer.measure(
                value.toString(),
                TextStyle(fontSize = 10.sp, color = axisLabelColor),
            )
            drawText(label, topLeft = Offset(padLeft - label.size.width - 6f, y - label.size.height / 2f))
        }

        // -- Single-point edge case --
        if (trend.size == 1) {
            val cx = padLeft + chartW / 2f
            val cy = padTop + chartH * (1f - trend.first().overallScore / 100f)
            drawCircle(onSurfaceColor, 7f, Offset(cx, cy))
            return@Canvas
        }

        // -- Compute point positions --
        val minDate = trend.first().date.toFloat()
        val maxDate = trend.last().date.toFloat()
        val dateRange = (maxDate - minDate).coerceAtLeast(1f)

        val points = trend.map { pt ->
            Offset(
                x = padLeft + chartW * ((pt.date - minDate) / dateRange),
                y = padTop + chartH * (1f - pt.overallScore / 100f),
            )
        }

        // -- Draw line path --
        val path = Path().apply {
            points.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        }
        drawPath(path, chartLineColor, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // -- Draw regular dots --
        points.forEachIndexed { i, offset ->
            val isBest = i == kpi.bestIndex
            val isLow = i == kpi.lowIndex
            val isLast = i == points.lastIndex
            if (!isBest && !isLow && !isLast) {
                drawCircle(onSurfaceColor, 4.5f, offset)
                drawCircle(chartLineColor, 3.5f, offset)
            }
        }

        // -- Best annotation --
        if (kpi.bestIndex in points.indices && kpi.bestScore != null) {
            val bp = points[kpi.bestIndex]
            drawCircle(BestColor, 6f, bp)

            val bestLabel = textMeasurer.measure(
                "Best ${kpi.bestScore}",
                TextStyle(fontSize = 11.sp, color = BestColor, fontWeight = FontWeight.SemiBold),
            )
            // Position label above-right, clamped inside chart
            val lx = (bp.x + 8f).coerceAtMost(padLeft + chartW - bestLabel.size.width)
            val ly = (bp.y - bestLabel.size.height - 6f).coerceAtLeast(padTop)
            // Background pill
            drawRoundRect(
                color = BestColor.copy(alpha = 0.15f),
                topLeft = Offset(lx - 4f, ly - 2f),
                size = Size(bestLabel.size.width + 8f, bestLabel.size.height + 4f),
                cornerRadius = CornerRadius(6f),
            )
            drawText(bestLabel, topLeft = Offset(lx, ly))
        }

        // -- Low annotation --
        if (kpi.lowIndex in points.indices && kpi.lowScore != null && kpi.lowIndex != kpi.bestIndex) {
            val lp = points[kpi.lowIndex]
            drawCircle(LowColor, 6f, lp)

            val lowLabel = textMeasurer.measure(
                "Low ${kpi.lowScore}",
                TextStyle(fontSize = 11.sp, color = LowColor, fontWeight = FontWeight.SemiBold),
            )
            val lx = (lp.x + 8f).coerceAtMost(padLeft + chartW - lowLabel.size.width)
            val ly = (lp.y + 8f).coerceAtMost(padTop + chartH - lowLabel.size.height)
            drawRoundRect(
                color = LowColor.copy(alpha = 0.15f),
                topLeft = Offset(lx - 4f, ly - 2f),
                size = Size(lowLabel.size.width + 8f, lowLabel.size.height + 4f),
                cornerRadius = CornerRadius(6f),
            )
            drawText(lowLabel, topLeft = Offset(lx, ly))
        }

        // -- Last-point emphasis --
        val lastPt = points.last()
        val lastScore = trend.last().overallScore
        // Outer glow ring
        drawCircle(onSurfaceColor.copy(alpha = 0.18f), 12f, lastPt)
        drawCircle(onSurfaceColor, 7f, lastPt)
        drawCircle(chartLineColor, 5f, lastPt)

        // Value label to the right
        val lastLabel = textMeasurer.measure(
            lastScore.toString(),
            TextStyle(fontSize = 14.sp, color = onSurfaceColor, fontWeight = FontWeight.Bold),
        )
        drawText(
            lastLabel,
            topLeft = Offset(lastPt.x + 12f, lastPt.y - lastLabel.size.height / 2f),
        )

        // -- X-axis date labels --
        val firstDateLabel = textMeasurer.measure(
            DateTimeUtils.formatShortDate(trend.first().date),
            TextStyle(fontSize = 10.sp, color = axisLabelColor),
        )
        drawText(firstDateLabel, topLeft = Offset(padLeft, size.height - padBottom + 6f))

        val lastDateLabel = textMeasurer.measure(
            DateTimeUtils.formatShortDate(trend.last().date),
            TextStyle(fontSize = 10.sp, color = axisLabelColor),
        )
        drawText(
            lastDateLabel,
            topLeft = Offset(padLeft + chartW - lastDateLabel.size.width, size.height - padBottom + 6f),
        )
    }
}

// -- Footnote --

@Composable
private fun TrendFootnote(
    selectedDuration: DurationFilter,
    modifier: Modifier = Modifier,
) {
    val periodText = when (selectedDuration) {
        DurationFilter.ONE_WEEK -> "1 week"
        DurationFilter.ONE_MONTH -> "1 month"
        DurationFilter.THREE_MONTHS -> "3 months"
        DurationFilter.SIX_MONTHS -> "6 months"
        DurationFilter.ONE_YEAR -> "1 year"
    }
    Row(
        modifier = modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Change vs previous $periodText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatOneDecimal(value: Number): String {
    val d = value.toDouble()
    val intPart = d.toLong()
    val decPart = ((d - intPart) * 10).toLong().let { kotlin.math.abs(it) }
    return if (d < 0 && intPart == 0L) "-0.$decPart" else "$intPart.$decPart"
}
