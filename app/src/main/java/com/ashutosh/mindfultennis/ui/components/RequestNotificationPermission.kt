package com.ashutosh.mindfultennis.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Composable that requests POST_NOTIFICATIONS permission on Android 13+.
 * On older versions, this is a no-op.
 *
 * @param onResult Callback invoked with `true` if permission is granted, `false` if denied.
 *   Not called on pre-Android 13 (permission not required).
 */
@Composable
fun RequestNotificationPermission(
    onResult: (Boolean) -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var hasRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        onResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (hasRequested) return@LaunchedEffect
        hasRequested = true

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val status = ContextCompat.checkSelfPermission(context, permission)
        if (status != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
    }
}
