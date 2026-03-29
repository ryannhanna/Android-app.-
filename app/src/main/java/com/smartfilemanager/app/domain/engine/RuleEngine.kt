package com.smartfilemanager.app.domain.engine

import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.domain.model.ScannedFile
import com.smartfilemanager.app.util.FormatUtil

class RuleEngine {

    /**
     * Evaluates a list of files against a rule.
     * Returns only files that match ALL (AND) or ANY (OR) conditions.
     * Returns all files unchanged if conditions list is empty.
     */
    fun evaluate(
        files: List<ScannedFile>,
        conditions: List<ConditionEntity>,
        logic: String   // "AND" | "OR"
    ): List<ScannedFile> {
        if (conditions.isEmpty()) return files
        return files.filter { file ->
            if (logic == "AND") {
                conditions.all { condition -> matches(file, condition) }
            } else {
                conditions.any { condition -> matches(file, condition) }
            }
        }
    }

    fun matches(file: ScannedFile, condition: ConditionEntity): Boolean {
        return when (condition.field) {
            "duration" -> {
                val fileDurationSec = file.durationMs / 1000.0
                val threshold = condition.value.toDoubleOrNull() ?: return false
                when (condition.operator) {
                    "lt" -> fileDurationSec < threshold
                    "gt" -> fileDurationSec > threshold
                    else -> false
                }
            }
            "age" -> {
                val ageInDays = FormatUtil.ageInDays(file.lastModified)
                val threshold = condition.value.toLongOrNull() ?: return false
                when (condition.operator) {
                    "lt" -> ageInDays < threshold
                    "gt" -> ageInDays > threshold
                    else -> false
                }
            }
            "size" -> {
                val unit = condition.unit?.lowercase() ?: "mb"
                val multiplier = if (unit == "gb") 1_073_741_824L else 1_048_576L
                val thresholdBytes = (condition.value.toDoubleOrNull() ?: return false) * multiplier
                when (condition.operator) {
                    "lt" -> file.sizeBytes < thresholdBytes
                    "gt" -> file.sizeBytes > thresholdBytes
                    else -> false
                }
            }
            "extension" -> {
                val ext = condition.value.trim().lowercase().let {
                    if (it.startsWith(".")) it else ".$it"
                }
                file.name.lowercase().endsWith(ext)
            }
            "file_name" -> {
                val target = condition.value.trim().lowercase()
                when (condition.operator) {
                    "contains"     -> file.name.lowercase().contains(target)
                    "not_contains" -> !file.name.lowercase().contains(target)
                    else           -> false
                }
            }
            "directory" -> {
                val target = condition.value.trim().lowercase()
                when (condition.operator) {
                    "contains"     -> file.path.lowercase().contains(target)
                    "not_contains" -> !file.path.lowercase().contains(target)
                    else           -> false
                }
            }
            else -> false
        }
    }
}
