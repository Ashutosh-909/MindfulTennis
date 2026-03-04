package com.ashutosh.mindfultennis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * A shimmer loading placeholder for a card section.
 * Displays animated placeholder bars to indicate loading state.
 */
@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    barCount: Int = 3,
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.md),
    ) {
        repeat(barCount) { index ->
            val fraction = when (index) {
                0 -> 0.6f
                barCount - 1 -> 0.4f
                else -> 0.8f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(if (index == 0) 16.dp else 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush),
            )
            if (index < barCount - 1) {
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp))
                .background(brush),
        )
    }
}
