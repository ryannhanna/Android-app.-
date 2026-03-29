package com.smartfilemanager.app.ui.screen

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartfilemanager.app.ui.components.FolderFilterChips
import com.smartfilemanager.app.ui.components.SortDropdownMenu
import com.smartfilemanager.app.ui.components.VideoListItem
import com.smartfilemanager.app.ui.viewmodel.FileBrowserViewModel

@Composable
fun FileBrowserScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: FileBrowserViewModel = viewModel(factory = FileBrowserViewModel.factory(application))
    val uiState by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            vm.clearSnackbar()
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        vm.onDeleteResult(result.resultCode)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Selection mode header bar
            if (uiState.isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.exitSelectionMode() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit selection")
                    }
                    Text(
                        text = "${uiState.selectedIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
            }

            // Folder chips + sort (normal mode only)
            if (!uiState.isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FolderFilterChips(
                        folders = uiState.folders,
                        selectedFolder = uiState.selectedFolder,
                        onFolderSelected = { vm.setFolder(it) },
                        modifier = Modifier.weight(1f)
                    )
                    SortDropdownMenu(
                        currentSort = uiState.sortOrder,
                        onSortSelected = { vm.setSort(it) }
                    )
                }
                HorizontalDivider()
            }

            // Main content area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        Text(
                            text = uiState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                    uiState.displayedFiles.isEmpty() -> {
                        Text(
                            text = "No videos found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.displayedFiles, key = { it.id }) { file ->
                                VideoListItem(
                                    file = file,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = file.id in uiState.selectedIds,
                                    onClick = {
                                        if (uiState.isSelectionMode) vm.toggleSelection(file.id)
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) vm.onFileLongPress(file.id)
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 100.dp))
                            }
                        }
                    }
                }
            }

            // Delete action bar — inside Column so it pushes the list up, not an overlay
            if (uiState.isSelectionMode && uiState.selectedIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp
                ) {
                    val count = uiState.selectedIds.size
                    Button(
                        onClick = {
                            val intentSender = vm.getDeleteIntentSender()
                            if (intentSender != null) {
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Delete $count ${if (count == 1) "file" else "files"}")
                    }
                }
            }
        }

        // Snackbar anchored at the bottom of the screen area
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
