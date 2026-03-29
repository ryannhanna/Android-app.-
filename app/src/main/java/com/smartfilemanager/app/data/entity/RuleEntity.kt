package com.smartfilemanager.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val conditionLogic: String,     // "AND" | "OR"
    val targetDirectory: String?,   // null = scan all storage
    val createdAt: Long,            // epoch millis
    val updatedAt: Long
)
