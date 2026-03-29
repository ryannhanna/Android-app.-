package com.smartfilemanager.app.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartfilemanager.app.ui.viewmodel.HistoryViewModel
import com.smartfilemanager.app.util.FormatUtil

private data class DeletedFileRecord(
    val name: String = "",
    val path: String = "",
    val sizeBytes: Long = 0L
)

@Composable
fun HistoryDetailScreen(
    logId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(application))
    val log by vm.detailLog.collectAsState()

    LaunchedEffect(logId) { vm.loadDetail(logId) }

    Column(modifier = modifier.fillMaxSize()) {

        // Header
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
                    text = if (log != null) {
                        "${log!!.ruleName} — ${formatRunAtShort(log!!.runAt)}"
                    } else {
                        "History Detail"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider()

        if (log == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Column
        }

        val entry = log!!

        // Summary bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "${entry.filesDeleted} ${if (entry.filesDeleted == 1) "file" else "files"} · ${FormatUtil.formatSize(entry.bytesFreed)} freed",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        HorizontalDivider()

        // File list parsed from JSON
        val deletedFiles = parseFileList(entry.fileListJson)
        if (deletedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "No file details recorded",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(deletedFiles) { file ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = FormatUtil.formatSize(file.sizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (file.path.isNotBlank()) {
                            Text(
                                text = file.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

private val gson = Gson()

private fun parseFileList(json: String): List<DeletedFileRecord> {
    return try {
        val type = object : TypeToken<List<DeletedFileRecord>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun formatRunAtShort(epochMillis: Long): String {
    return java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(epochMillis))
}
