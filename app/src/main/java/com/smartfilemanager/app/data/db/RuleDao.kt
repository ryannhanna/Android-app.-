package com.smartfilemanager.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.data.model.RuleWithConditions
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY updatedAt DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Transaction
    @Query("SELECT * FROM rules ORDER BY updatedAt DESC")
    fun getAllRulesWithConditions(): Flow<List<RuleWithConditions>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Int): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)
}
