package com.smartfilemanager.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.smartfilemanager.app.data.entity.DeletionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletionLogDao {

    @Query("SELECT * FROM deletion_log ORDER BY runAt DESC")
    fun getAllLogs(): Flow<List<DeletionLogEntity>>

    @Query("SELECT * FROM deletion_log WHERE id = :id")
    suspend fun getLogById(id: Int): DeletionLogEntity?

    @Insert
    suspend fun insertLog(log: DeletionLogEntity): Long

    @Delete
    suspend fun deleteLog(log: DeletionLogEntity)

    @Query("DELETE FROM deletion_log WHERE runAt < :beforeEpoch")
    suspend fun deleteLogsOlderThan(beforeEpoch: Long)
}
