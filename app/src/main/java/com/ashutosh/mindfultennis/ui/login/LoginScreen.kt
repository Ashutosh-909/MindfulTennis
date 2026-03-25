package com.ashutosh.mindfultennis.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ashutosh.mindfultennis.ui.theme.DarkNavy
import com.ashutosh.mindfultennis.ui.theme.Spacing

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSignedIn: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate when signed in
    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            onSignedIn()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Retry",
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(LoginUiEvent.RetryClicked)
            }
            viewModel.onEvent(LoginUiEvent.ErrorDismissed)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            // Tennis court illustration
            TennisCourtIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = Spacing.xl)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // "MINDFUL" branding — small-caps, letter-spaced
            Text(
                text = "MINDFUL",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 4.sp,
                    color = DarkNavy,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // "Tennis" branding — large serif font
            Text(
                text = "Tennis",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = DarkNavy,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Email field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEvent(LoginUiEvent.EmailChanged(it)) },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Password field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Sign Up / Sign In button
            Button(
                onClick = { viewModel.onEvent(LoginUiEvent.EmailAuthSubmitted) },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (uiState.isSignUpMode) "Sign Up" else "Sign In",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // Toggle auth mode
            TextButton(
                onClick = { viewModel.onEvent(LoginUiEvent.ToggleAuthMode) },
            ) {
                Text(
                    text = if (uiState.isSignUpMode)
                        "Already have an account? Sign In"
                    else
                        "Don't have an account? Sign Up",
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Google Sign-In button
            OutlinedButton(
                onClick = { viewModel.onEvent(LoginUiEvent.SignInWithGoogleClicked) },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Sign in with Google" },
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Legal text
            Text(
                text = "By signing in you agree to our Terms & Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

/**
 * Top-down line-art tennis court rendered with Canvas.
 * Dark navy strokes on transparent background.
 */
@Composable
private fun TennisCourtIllustration(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Tennis court illustration"
        }
    ) {
        val strokeWidth = 2.dp.toPx()
        val courtStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val color = DarkNavy

        val canvasW = size.width
        val canvasH = size.height

        // Court padding from canvas edges
        val padX = canvasW * 0.08f
        val padY = canvasH * 0.08f

        val courtLeft = padX
        val courtTop = padY
        val courtWidth = canvasW - 2 * padX
        val courtHeight = canvasH - 2 * padY
        val courtRight = courtLeft + courtWidth
        val courtBottom = courtTop + courtHeight

        // Outer court boundary (rounded rect)
        drawRoundRect(
            color = color,
            topLeft = Offset(courtLeft, courtTop),
            size = Size(courtWidth, courtHeight),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = courtStroke,
        )

        // Net line (horizontal center)
        val centerY = courtTop + courtHeight / 2f
        drawLine(
            color = color,
            start = Offset(courtLeft, centerY),
            end = Offset(courtRight, centerY),
            strokeWidth = strokeWidth * 1.5f,
            cap = StrokeCap.Round,
        )

        // Singles sidelines (inner vertical lines)
        val singlesInset = courtWidth * 0.12f
        drawLine(
            color = color,
            start = Offset(courtLeft + singlesInset, courtTop),
            end = Offset(courtLeft + singlesInset, courtBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(courtRight - singlesInset, courtTop),
            end = Offset(courtRight - singlesInset, courtBottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // Service lines (horizontal, between singles sidelines)
        val serviceLineOffset = courtHeight * 0.28f
        // Top service line
        drawLine(
            color = color,
            start = Offset(courtLeft + singlesInset, courtTop + serviceLineOffset),
            end = Offset(courtRight - singlesInset, courtTop + serviceLineOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        // Bottom service line
        drawLine(
            color = color,
            start = Offset(courtLeft + singlesInset, courtBottom - serviceLineOffset),
            end = Offset(courtRight - singlesInset, courtBottom - serviceLineOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // Center service line (vertical, between service lines)
        val centerX = courtLeft + courtWidth / 2f
        drawLine(
            color = color,
            start = Offset(centerX, courtTop + serviceLineOffset),
            end = Offset(centerX, courtBottom - serviceLineOffset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // Center marks (small perpendicular lines at top and bottom baselines)
        val markLength = courtHeight * 0.03f
        drawLine(
            color = color,
            start = Offset(centerX, courtTop),
            end = Offset(centerX, courtTop + markLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(centerX, courtBottom),
            end = Offset(centerX, courtBottom - markLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
