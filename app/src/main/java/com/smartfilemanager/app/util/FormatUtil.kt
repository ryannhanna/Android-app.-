package com.smartfilemanager.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtil {

    // 3661000ms → "1:01:01"  |  90000ms → "1:30"
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    // 1048576 → "1.0 MB"  |  1073741824 → "1.0 GB"
    fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            "%.2f GB".format(mb / 1024.0)
        } else {
            "%.1f MB".format(mb)
        }
    }

    // epochMillis → "3 days ago"  |  "Today"  |  "25 Jan 2025"
    fun formatAge(epochMillis: Long): String {
        val ageDays = ageInDays(epochMillis)
        return when {
            ageDays == 0L -> "Today"
            ageDays == 1L -> "Yesterday"
            ageDays < 30L -> "$ageDays days ago"
            ageDays < 365L -> "${ageDays / 30} months ago"
            else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
        }
    }

    // epochMillis → days since that date (used by RuleEngine in Phase 6)
    fun ageInDays(epochMillis: Long): Long {
        val ageMs = System.currentTimeMillis() - epochMillis
        return ageMs / (1000L * 60 * 60 * 24)
    }
}
