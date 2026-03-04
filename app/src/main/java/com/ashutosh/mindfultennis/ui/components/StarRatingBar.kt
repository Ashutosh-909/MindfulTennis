package com.ashutosh.mindfultennis.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Reusable star rating bar composable.
 * Displays 5 stars that the user can tap to set a rating (1–5).
 *
 * @param rating Current selected rating (0 = none, 1–5 = selected).
 * @param onRatingChanged Callback when user taps a star.
 * @param modifier Modifier for the row.
 * @param starSize Size of each star icon (default 40.dp for touch target).
 * @param activeColor Color of filled stars.
 * @param inactiveColor Color of unfilled stars.
 * @param enabled Whether the stars are tappable.
 * @param label Semantics label prefix for accessibility (e.g., "Forehand").
 */
@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 40.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant,
    enabled: Boolean = true,
    label: String = "",
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = if (label.isNotEmpty()) {
                "$label rating, $rating out of 5 stars"
            } else {
                "$rating out of 5 stars"
            }
        },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (starIndex in 1..MAX_STARS) {
            val isFilled = starIndex <= rating
            val interactionSource = remember { MutableInteractionSource() }

            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = if (enabled) {
                    "Rate $starIndex star${if (starIndex > 1) "s" else ""}"
                } else {
                    "$starIndex star${if (starIndex > 1) "s" else ""}"
                },
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (enabled) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                onRatingChanged(starIndex)
                            }
                        } else {
                            Modifier
                        }
                    ),
                tint = if (isFilled) activeColor else inactiveColor,
            )
        }
    }
}

private const val MAX_STARS = 5
