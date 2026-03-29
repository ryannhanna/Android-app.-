package com.smartfilemanager.app.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartfilemanager.app.ui.components.VideoListItem
import com.smartfilemanager.app.ui.viewmodel.ScanUiState
import com.smartfilemanager.app.util.FormatUtil

@Composable
fun ScanResultsContent(
    uiState: ScanUiState,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onConfirmDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {

        // Header bar
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Results: ${uiState.selectedRule?.name ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                val allSelected = uiState.scanResults.isNotEmpty() &&
                    uiState.selectedIds.size == uiState.scanResults.size
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { checked ->
                        if (checked) onSelectAll() else onDeselectAll()
                    }
                )
                Text(
                    text = "All",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        HorizontalDivider()

        // Summary bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val totalBytes = uiState.scanResults.sumOf { it.sizeBytes }
            Text(
                text = "${uiState.scanResults.size} ${if (uiState.scanResults.size == 1) "file" else "files"} matched · ${FormatUtil.formatSize(totalBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        HorizontalDivider()

        // File list
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.scanResults.isEmpty()) {
                Text(
                    text = "No videos matched this rule.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.scanResults, key = { it.id }) { file ->
                        VideoListItem(
                            file = file,
                            isSelectionMode = true,
                            isSelected = file.id in uiState.selectedIds,
                            onClick = { onToggleSelection(file.id) },
                            onLongClick = {}
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 100.dp))
                    }
                }
            }
        }

        // Delete action bar
        if (uiState.selectedIds.isNotEmpty()) {
            val selectedFiles = uiState.scanResults.filter { it.id in uiState.selectedIds }
            val selectedBytes = selectedFiles.sumOf { it.sizeBytes }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 4.dp
            ) {
                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "Delete ${selectedFiles.size} ${if (selectedFiles.size == 1) "file" else "files"} · ${FormatUtil.formatSize(selectedBytes)}"
                    )
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        val selectedFiles = uiState.scanResults.filter { it.id in uiState.selectedIds }
        val selectedBytes = selectedFiles.sumOf { it.sizeBytes }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("Delete ${selectedFiles.size} ${if (selectedFiles.size == 1) "video" else "videos"}?")
            },
            text = {
                Text(
                    "This will permanently delete ${selectedFiles.size} ${if (selectedFiles.size == 1) "file" else "files"} " +
                        "and free ${FormatUtil.formatSize(selectedBytes)} of storage. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
