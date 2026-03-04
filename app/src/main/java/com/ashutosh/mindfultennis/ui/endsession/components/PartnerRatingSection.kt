package com.ashutosh.mindfultennis.ui.endsession.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.ui.components.StarRatingBar
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Section that shows an "+ Add Partner's Feedback" button, or a summary card with edit/remove
 * if partner's feedback has been saved. Partner's feedback = your partner's opinion on YOUR game.
 */
@Composable
fun PartnerRatingSection(
    partnerRatings: Map<Aspect, Int>,
    onSave: (Map<Aspect, Int>) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Partner's Feedback (optional)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        if (partnerRatings.isNotEmpty()) {
            // Summary card
            PartnerRatingSummaryCard(
                ratings = partnerRatings,
                onEdit = { showDialog = true },
                onClear = onClear,
            )
        } else {
            // Add button
            OutlinedButton(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Ask Your Partner for Feedback")
            }
        }
    }

    if (showDialog) {
        PartnerRatingDialog(
            initialRatings = partnerRatings,
            onSave = { ratings ->
                onSave(ratings)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun PartnerRatingSummaryCard(
    ratings: Map<Aspect, Int>,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Partner's Feedback",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear partner's feedback")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Compact star display
            Aspect.entries.forEach { aspect ->
                val rating = ratings[aspect] ?: 0
                if (rating > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = aspect.displayName(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(Spacing.md * 5),
                        )
                        StarRatingBar(
                            rating = rating,
                            onRatingChanged = {},
                            enabled = false,
                            label = aspect.displayName(),
                            starSize = Spacing.lg,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerRatingDialog(
    initialRatings: Map<Aspect, Int>,
    onSave: (Map<Aspect, Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var ratings by remember {
        mutableStateOf(
            Aspect.entries.associateWith { initialRatings[it] ?: 0 }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partner's Feedback") },
        confirmButton = {
            Button(
                onClick = {
                    onSave(ratings.filter { it.value > 0 })
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(
                modifier = Modifier.semantics {
                    contentDescription = "Partner's Feedback dialog"
                },
            ) {
                Aspect.entries.forEach { aspect ->
                    AspectRatingRow(
                        label = aspect.displayName(),
                        rating = ratings[aspect] ?: 0,
                        onRatingChanged = { newRating ->
                            ratings = ratings.toMutableMap().also { it[aspect] = newRating }
                        },
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
        },
    )
}

@Composable
internal fun AspectRatingRow(
    label: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        StarRatingBar(
            rating = rating,
            onRatingChanged = onRatingChanged,
            label = label,
            enabled = enabled,
        )
    }
}

/**
 * Extension to format aspect name for display.
 */
fun Aspect.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }
