package com.smartfilemanager.app.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfilemanager.app.util.PermissionUtil

private enum class PermissionUiState { INITIAL, RATIONALE, PERMANENTLY_DENIED }

@Composable
fun PermissionScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity

    var uiState by remember { mutableStateOf(PermissionUiState.INITIAL) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            uiState = if (PermissionUtil.shouldShowRationale(activity)) {
                PermissionUiState.RATIONALE
            } else {
                PermissionUiState.PERMANENTLY_DENIED
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.VideoFile,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Storage Access Required",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        val bodyText = when (uiState) {
            PermissionUiState.PERMANENTLY_DENIED ->
                "Permission was permanently denied. Please enable \"Media videos\" access for SmartFileManager in your device settings."
            else ->
                "SmartFileManager needs access to your video files to scan, filter, and manage them using your rules."
        }

        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState == PermissionUiState.PERMANENTLY_DENIED) {
            Button(onClick = { PermissionUtil.openAppSettings(context) }) {
                Text("Open Settings")
            }
        } else {
            Button(onClick = { launcher.launch(PermissionUtil.VIDEO_PERMISSION) }) {
                Text("Grant Permission")
            }
        }

        if (uiState == PermissionUiState.RATIONALE) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { PermissionUtil.openAppSettings(context) }) {
                Text("Open Settings Instead")
            }
        }
    }
}
