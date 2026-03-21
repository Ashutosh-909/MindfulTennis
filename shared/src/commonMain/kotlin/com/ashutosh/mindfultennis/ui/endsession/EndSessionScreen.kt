package com.ashutosh.mindfultennis.ui.endsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.ui.endsession.components.AspectRatingRow
import com.ashutosh.mindfultennis.ui.endsession.components.PartnerRatingSection
import com.ashutosh.mindfultennis.ui.endsession.components.SetScoreSection
import com.ashutosh.mindfultennis.ui.endsession.components.displayName
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.ashutosh.mindfultennis.util.DateTimeUtils

/**
 * End Session / Rating screen.
 * Shows session metadata, 8 aspect star ratings, notes field,
 * optional partner ratings dialog, optional set scores dialog, and submit button.
 */
@Composable
fun EndSessionScreen(
    viewModel: EndSessionViewModel,
    onSessionSubmitted: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate back to Home when submitted
    LaunchedEffect(uiState.submitted) {
        if (uiState.submitted) {
            onSessionSubmitted()
        }
    }

    EndSessionContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndSessionContent(
    state: EndSessionUiState,
    onEvent: (EndSessionUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors via snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(EndSessionUiEvent.ErrorDismissed)
        }
    }

    LaunchedEffect(state.validationError) {
        state.validationError?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(EndSessionUiEvent.ValidationErrorDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Rate Your Session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.isLoading) {
            // Loading spinner centered
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // Session metadata header
            SessionMetadataHeader(state)

            Spacer(modifier = Modifier.height(Spacing.lg))

            // -- Your Ratings --
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Your Ratings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // 8 aspect star rating rows
            Aspect.entries.forEach { aspect ->
                AspectRatingRow(
                    label = aspect.displayName(),
                    rating = state.selfRatings[aspect] ?: 0,
                    onRatingChanged = { rating ->
                        onEvent(EndSessionUiEvent.SelfRatingChanged(aspect, rating))
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Notes / comments
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onEvent(EndSessionUiEvent.NotesChanged(it)) },
                label = { Text("Notes / comments") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // -- Partner Ratings (optional) --
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.sm))

            PartnerRatingSection(
                partnerRatings = state.partnerRatings,
                onSave = { ratings ->
                    onEvent(EndSessionUiEvent.PartnerRatingsSaved(ratings))
                },
                onClear = { onEvent(EndSessionUiEvent.PartnerRatingsCleared) },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // -- Set Scores --
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.sm))

            SetScoreSection(
                data = state.setScoreData,
                opponents = state.opponents,
                partners = state.partners,
                onSave = { data ->
                    onEvent(EndSessionUiEvent.SetScoresSaved(data))
                },
                onClear = { onEvent(EndSessionUiEvent.SetScoresCleared) },
                onCreateOpponent = { name ->
                    onEvent(EndSessionUiEvent.OpponentCreated(name))
                },
                onCreatePartner = { name ->
                    onEvent(EndSessionUiEvent.PartnerCreated(name))
                },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Submit button
            Button(
                onClick = { onEvent(EndSessionUiEvent.SubmitClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isSubmitting,
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Submit Session",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SessionMetadataHeader(state: EndSessionUiState) {
    val session = state.session ?: return

    Column {
        // Session date/time
        val timeText = if (session.endedAt != null) {
            "Session: ${DateTimeUtils.formatSessionDateRange(session.startedAt, session.endedAt)}"
        } else {
            "Session: Started ${DateTimeUtils.formatDate(session.startedAt)} at ${DateTimeUtils.formatTime(session.startedAt)}"
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Focus note
        if (session.focusNote.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Focus: \"${session.focusNote}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
