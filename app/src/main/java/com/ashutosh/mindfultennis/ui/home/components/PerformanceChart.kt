package com.ashutosh.mindfultennis.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
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

// ── Card palette (always dark regardless of system theme) ─────────────────────
private val CardBg = Color(0xFF0F1428)
private val SegmentBg = Color(0xFF1A2038)
private val SegmentSelectedBg = Color(0xFF2D3460)
private val ChartLine = Color(0xFFCCD6E0)
private val GridLine = Color(0xFF1C2246)
private val AxisLabel = Color(0xFF6B7394)
private val SubtitleText = Color(0xFF8B92B0)
private val KpiDeltaUp = Color(0xFF4CAF50)
private val KpiDeltaDown = Color(0xFFF44336)
private val BestColor = Color(0xFF4CAF50)
private val LowColor = Color(0xFFF44336)
private val FootnoteText = Color(0xFF6B7394)

// ── Computed KPI helper ───────────────────────────────────────────────────────
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

// ── Public composable (same signature — drop-in replacement) ──────────────────

/**
 * Performance Trend hero card.
 * Dark-themed card showing KPI summary, time-range selector,
 * and annotated line chart with min/max callouts.
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
    val kpi = remember(trend) { TrendKpi.compute(trend) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = CardBg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {

            // ── KPI strip ───────────────────────────────────────────────
            if (kpi.currentScore != null) {
                KpiStrip(kpi = kpi, selectedDuration = selectedDuration)
                Spacer(Modifier.height(Spacing.md))
            }

            // ── Time-range segmented control ────────────────────────────
            TimeRangeSegmentedControl(
                selected = selectedDuration,
                onSelected = onDurationSelected,
            )

            Spacer(Modifier.height(Spacing.md))

            // ── Chart body ──────────────────────────────────────────────
            when {
                isLoading -> LoadingShimmer(height = 200.dp, barCount = 0)
                error != null -> ErrorRetryCard(message = error, onRetry = onRetry)
                trend.isEmpty() -> EmptyChartPlaceholder()
                else -> TrendLineChart(trend = trend, kpi = kpi)
            }

            // ── Footnote ────────────────────────────────────────────────
            if (trend.isNotEmpty() && kpi.currentScore != null) {
                Spacer(Modifier.height(Spacing.sm))
                TrendFootnote(selectedDuration = selectedDuration)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

// ── KPI Strip ─────────────────────────────────────────────────────────────────

@Composable
private fun KpiStrip(
    kpi: TrendKpi,
    selectedDuration: DurationFilter,
    modifier: Modifier = Modifier,
) {
    val deltaColor = when {
        kpi.delta == null -> SubtitleText
        kpi.delta >= 0f -> KpiDeltaUp
        else -> KpiDeltaDown
    }
    val arrow = when {
        kpi.delta == null -> ""
        kpi.delta >= 0f -> "▲"
        else -> "▼"
    }

    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Normal, fontSize = 14.sp)) {
                append("Latest Performance: ")
            }
            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                append("${kpi.currentScore}")
            }
            if (kpi.delta != null) {
                append("  ")
                withStyle(SpanStyle(color = deltaColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)) {
                    append("$arrow${if (kpi.delta >= 0) "+" else ""}${"%.1f".format(kpi.delta)}")
                }
                withStyle(SpanStyle(color = SubtitleText, fontSize = 12.sp)) {
                    append(" vs previous ${selectedDuration.label}")
                }
            }
            if (kpi.periodAverage != null) {
                append("  ")
                withStyle(SpanStyle(color = SubtitleText, fontSize = 12.sp)) {
                    append("(Avg ${selectedDuration.label}: ${"%.1f".format(kpi.periodAverage)})")
                }
            }
        },
    )
}

// ── Time-range segmented control ──────────────────────────────────────────────

@Composable
private fun TimeRangeSegmentedControl(
    selected: DurationFilter,
    onSelected: (DurationFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = SegmentBg, shape = RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        DurationFilter.entries.forEach { duration ->
            val isSelected = duration == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) SegmentSelectedBg else Color.Transparent)
                    .clickable { onSelected(duration) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = duration.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else SubtitleText,
                )
            }
        }
    }
}

// ── Empty placeholder ─────────────────────────────────────────────────────────

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
            color = SubtitleText,
        )
    }
}

// ── Annotated line chart (Canvas) ─────────────────────────────────────────────

@Composable
private fun TrendLineChart(
    trend: List<PerformanceTrend>,
    kpi: TrendKpi,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

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

        // ── Grid lines & Y-axis labels ─────────────────────────────
        val yTicks = listOf(0, 25, 50, 75, 100)
        yTicks.forEach { value ->
            val y = padTop + chartH * (1f - value / 100f)
            drawLine(GridLine, Offset(padLeft, y), Offset(padLeft + chartW, y), 1f)

            val label = textMeasurer.measure(
                value.toString(),
                TextStyle(fontSize = 10.sp, color = AxisLabel),
            )
            drawText(label, topLeft = Offset(padLeft - label.size.width - 6f, y - label.size.height / 2f))
        }

        // ── Single-point edge case ─────────────────────────────────
        if (trend.size == 1) {
            val cx = padLeft + chartW / 2f
            val cy = padTop + chartH * (1f - trend.first().overallScore / 100f)
            drawCircle(Color.White, 7f, Offset(cx, cy))
            return@Canvas
        }

        // ── Compute point positions ────────────────────────────────
        val minDate = trend.first().date.toFloat()
        val maxDate = trend.last().date.toFloat()
        val dateRange = (maxDate - minDate).coerceAtLeast(1f)

        val points = trend.map { pt ->
            Offset(
                x = padLeft + chartW * ((pt.date - minDate) / dateRange),
                y = padTop + chartH * (1f - pt.overallScore / 100f),
            )
        }

        // ── Draw line path ─────────────────────────────────────────
        val path = Path().apply {
            points.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
        }
        drawPath(path, ChartLine, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // ── Draw regular dots ──────────────────────────────────────
        points.forEachIndexed { i, offset ->
            val isBest = i == kpi.bestIndex
            val isLow = i == kpi.lowIndex
            val isLast = i == points.lastIndex
            if (!isBest && !isLow && !isLast) {
                drawCircle(Color.White, 4.5f, offset)
                drawCircle(ChartLine, 3.5f, offset)
            }
        }

        // ── Best annotation ────────────────────────────────────────
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

        // ── Low annotation ─────────────────────────────────────────
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

        // ── Last-point emphasis ────────────────────────────────────
        val lastPt = points.last()
        val lastScore = trend.last().overallScore
        // Outer glow ring
        drawCircle(Color.White.copy(alpha = 0.18f), 12f, lastPt)
        drawCircle(Color.White, 7f, lastPt)
        drawCircle(ChartLine, 5f, lastPt)

        // Value label to the right
        val lastLabel = textMeasurer.measure(
            lastScore.toString(),
            TextStyle(fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold),
        )
        drawText(
            lastLabel,
            topLeft = Offset(lastPt.x + 12f, lastPt.y - lastLabel.size.height / 2f),
        )

        // ── X-axis date labels ─────────────────────────────────────
        val firstDateLabel = textMeasurer.measure(
            DateTimeUtils.formatShortDate(trend.first().date),
            TextStyle(fontSize = 10.sp, color = AxisLabel),
        )
        drawText(firstDateLabel, topLeft = Offset(padLeft, size.height - padBottom + 6f))

        val lastDateLabel = textMeasurer.measure(
            DateTimeUtils.formatShortDate(trend.last().date),
            TextStyle(fontSize = 10.sp, color = AxisLabel),
        )
        drawText(
            lastDateLabel,
            topLeft = Offset(padLeft + chartW - lastDateLabel.size.width, size.height - padBottom + 6f),
        )
    }
}

// ── Footnote ──────────────────────────────────────────────────────────────────

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
            tint = FootnoteText,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Change vs previous $periodText",
            style = MaterialTheme.typography.labelSmall,
            color = FootnoteText,
        )
    }
}
