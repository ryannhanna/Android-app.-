package com.smartfilemanager.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.data.entity.DeletionLogEntity
import com.smartfilemanager.app.data.entity.RuleEntity

@Database(
    entities = [RuleEntity::class, ConditionEntity::class, DeletionLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao
    abstract fun conditionDao(): ConditionDao
    abstract fun deletionLogDao(): DeletionLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartfilemanager.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
