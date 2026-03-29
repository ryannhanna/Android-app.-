package com.smartfilemanager.app.domain.model

import android.net.Uri

data class ScannedFile(
    val id: Long,                       // MediaStore row ID
    val uri: Uri,                       // content:// URI — use this for deletion
    val name: String,                   // Display name
    val path: String,                   // Relative path (e.g. /DCIM/Camera/)
    val sizeBytes: Long,
    val durationMs: Long,               // Always populated for video
    val lastModified: Long,             // Epoch millis
    val width: Int?,
    val height: Int?,
    val isSelected: Boolean = false     // UI state for checkbox selection
)
