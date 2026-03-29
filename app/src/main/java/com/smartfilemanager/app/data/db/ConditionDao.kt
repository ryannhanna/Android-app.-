package com.smartfilemanager.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartfilemanager.app.data.entity.ConditionEntity

@Dao
interface ConditionDao {

    @Query("SELECT * FROM conditions WHERE ruleId = :ruleId")
    suspend fun getConditionsForRule(ruleId: Int): List<ConditionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: List<ConditionEntity>)

    @Query("DELETE FROM conditions WHERE ruleId = :ruleId")
    suspend fun deleteConditionsForRule(ruleId: Int)
}
