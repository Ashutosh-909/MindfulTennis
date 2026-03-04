package com.ashutosh.mindfultennis.ui.startsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.mindfultennis.ui.components.RequestNotificationPermission
import com.ashutosh.mindfultennis.ui.theme.Spacing

/**
 * Start Session screen.
 * Shows an encouraging header, a focus note text field, recent focus point chips,
 * and a "Start Session" button.
 */
@Composable
fun StartSessionScreen(
    viewModel: StartSessionViewModel,
    onSessionStarted: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS permission on Android 13+ before starting a session
    RequestNotificationPermission()

    // Navigate back to Home when session starts
    LaunchedEffect(uiState.sessionStarted) {
        if (uiState.sessionStarted) {
            onSessionStarted()
        }
    }

    StartSessionContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StartSessionContent(
    state: StartSessionUiState,
    onEvent: (StartSessionUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(StartSessionUiEvent.ErrorDismissed)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("New Session") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Encouraging header
            Text(
                text = "All the best for this session.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Focus note text field
            OutlinedTextField(
                value = state.focusNote,
                onValueChange = { onEvent(StartSessionUiEvent.FocusNoteChanged(it)) },
                label = { Text("What do you want to work on today?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Session focus note" },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text("${state.focusNote.length}/500")
                },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Recent focus points
            if (state.recentFocusPoints.isNotEmpty()) {
                Text(
                    text = "Recent Focus Points:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    state.recentFocusPoints.forEach { focusPoint ->
                        AssistChip(
                            onClick = {
                                onEvent(StartSessionUiEvent.FocusPointChipClicked(focusPoint.text))
                            },
                            label = { Text(focusPoint.text) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Start Session button
            Button(
                onClick = { onEvent(StartSessionUiEvent.StartSessionClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Start Session ▶",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}
