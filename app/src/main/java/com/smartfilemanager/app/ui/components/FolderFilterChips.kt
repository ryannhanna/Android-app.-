package com.smartfilemanager.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun FolderFilterChips(
    folders: List<String>,
    selectedFolder: String?,
    onFolderSelected: (String?) -> Unit,
    onFolderLongPress: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        FilterChip(
            selected = selectedFolder == null,
            onClick = { onFolderSelected(null) },
            label = { Text("All") },
            modifier = Modifier.padding(end = 8.dp)
        )
        folders.forEach { folder ->
            // Show the last path segment as the chip label (e.g. "DCIM/Camera/" → "Camera")
            val label = folder.trimEnd('/').substringAfterLast('/').ifEmpty { folder }
            FilterChip(
                selected = folder == selectedFolder,
                onClick = { onFolderSelected(folder) },
                label = { Text(label) },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .pointerInput(folder) {
                        detectTapGestures(
                            onLongPress = { onFolderLongPress?.invoke(folder) }
                        )
                    }
            )
        }
    }
}
