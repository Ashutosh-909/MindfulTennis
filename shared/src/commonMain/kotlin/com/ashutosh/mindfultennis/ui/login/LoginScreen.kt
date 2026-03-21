package com.ashutosh.mindfultennis.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashutosh.mindfultennis.ui.theme.DarkNavy
import com.ashutosh.mindfultennis.ui.theme.Spacing

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onSignedIn: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onSignedIn()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val result = snackbarHostState.showSnackbar(message = error, actionLabel = "Dismiss")
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(LoginUiEvent.ErrorDismissed)
            }
            viewModel.onEvent(LoginUiEvent.ErrorDismissed)
        }
    }

    LaunchedEffect(uiState.signUpSuccess) {
        uiState.signUpSuccess?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.onEvent(LoginUiEvent.SignUpSuccessDismissed)
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
                    .height(200.dp)
                    .padding(horizontal = Spacing.xl)
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // "MINDFUL" branding
            Text(
                text = "MINDFUL",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 4.sp,
                    color = DarkNavy,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // "Tennis" branding
            Text(
                text = "Tennis",
                style = MaterialTheme.typography.displayMedium.copy(color = DarkNavy),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Email/Password form
            EmailPasswordForm(
                email = uiState.email,
                password = uiState.password,
                isSignUpMode = uiState.isSignUpMode,
                isLoading = uiState.isLoading,
                onEmailChanged = { viewModel.onEvent(LoginUiEvent.EmailChanged(it)) },
                onPasswordChanged = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
                onSubmit = { viewModel.onEvent(LoginUiEvent.SignInWithEmailClicked) },
                onToggleMode = { viewModel.onEvent(LoginUiEvent.ToggleSignUpMode) },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  or  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(text = "Sign in with Google", style = MaterialTheme.typography.labelLarge)
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

@Composable
private fun EmailPasswordForm(
    email: String,
    password: String,
    isSignUpMode: Boolean,
    isLoading: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChanged,
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
    )

    Spacer(modifier = Modifier.height(Spacing.sm))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
                onSubmit()
            }
        ),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
    )

    Spacer(modifier = Modifier.height(Spacing.md))

    Button(
        onClick = {
            focusManager.clearFocus()
            onSubmit()
        },
        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(
                text = if (isSignUpMode) "Create Account" else "Sign In",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }

    Spacer(modifier = Modifier.height(Spacing.xs))

    TextButton(onClick = onToggleMode) {
        Text(
            text = if (isSignUpMode) "Already have an account? Sign in" else "Don't have an account? Sign up",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

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

        val padX = canvasW * 0.08f
        val padY = canvasH * 0.08f

        val courtLeft = padX
        val courtTop = padY
        val courtWidth = canvasW - 2 * padX
        val courtHeight = canvasH - 2 * padY
        val courtRight = courtLeft + courtWidth
        val courtBottom = courtTop + courtHeight

        drawRoundRect(
            color = color,
            topLeft = Offset(courtLeft, courtTop),
            size = Size(courtWidth, courtHeight),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = courtStroke,
        )

        val centerY = courtTop + courtHeight / 2f
        drawLine(color, Offset(courtLeft, centerY), Offset(courtRight, centerY), strokeWidth * 1.5f, StrokeCap.Round)

        val singlesInset = courtWidth * 0.12f
        drawLine(color, Offset(courtLeft + singlesInset, courtTop), Offset(courtLeft + singlesInset, courtBottom), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(courtRight - singlesInset, courtTop), Offset(courtRight - singlesInset, courtBottom), strokeWidth, StrokeCap.Round)

        val serviceLineOffset = courtHeight * 0.28f
        drawLine(color, Offset(courtLeft + singlesInset, courtTop + serviceLineOffset), Offset(courtRight - singlesInset, courtTop + serviceLineOffset), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(courtLeft + singlesInset, courtBottom - serviceLineOffset), Offset(courtRight - singlesInset, courtBottom - serviceLineOffset), strokeWidth, StrokeCap.Round)

        val centerX = courtLeft + courtWidth / 2f
        drawLine(color, Offset(centerX, courtTop + serviceLineOffset), Offset(centerX, courtBottom - serviceLineOffset), strokeWidth, StrokeCap.Round)

        val markLength = courtHeight * 0.03f
        drawLine(color, Offset(centerX, courtTop), Offset(centerX, courtTop + markLength), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(centerX, courtBottom), Offset(centerX, courtBottom - markLength), strokeWidth, StrokeCap.Round)
    }
}
