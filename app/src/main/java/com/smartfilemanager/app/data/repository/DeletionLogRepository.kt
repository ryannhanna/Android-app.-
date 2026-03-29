package com.smartfilemanager.app.data.repository

import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.DeletionLogEntity
import kotlinx.coroutines.flow.Flow

class DeletionLogRepository(private val db: AppDatabase) {

    fun getAllLogs(): Flow<List<DeletionLogEntity>> = db.deletionLogDao().getAllLogs()

    suspend fun getLogById(id: Int): DeletionLogEntity? = db.deletionLogDao().getLogById(id)

    suspend fun saveLog(log: DeletionLogEntity) {
        db.deletionLogDao().insertLog(log)
    }

    suspend fun deleteLog(log: DeletionLogEntity) {
        db.deletionLogDao().deleteLog(log)
    }
}
