package com.smartfilemanager.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conditions",
    foreignKeys = [ForeignKey(
        entity = RuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["ruleId"],
        onDelete = ForeignKey.CASCADE   // deleting a rule deletes its conditions
    )],
    indices = [Index("ruleId")]
)
data class ConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleId: Int,
    val field: String,      // "duration" | "age" | "size" | "extension" | "file_name" | "directory"
    val operator: String,   // "lt" | "gt" | "eq" | "contains" | "not_contains"
    val value: String,      // "60", "25", ".mp4", "screen_record" — always stored as String
    val unit: String?       // "seconds" | "days" | "mb" | "gb" | null
)
