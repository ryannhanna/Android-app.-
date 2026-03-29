package com.smartfilemanager.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deletion_log")
data class DeletionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleId: Int?,           // null if run was ad-hoc
    val ruleName: String,       // snapshot of name at time of run
    val runAt: Long,            // epoch millis
    val filesDeleted: Int,
    val bytesFreed: Long,
    val fileListJson: String    // JSON: [{"name":"x.mp4","path":"/DCIM/","sizeBytes":1234}]
)
