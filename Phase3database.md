# Phase 3 — Room Database Schema

> **Read `DECISIONS.md` before starting this phase.**
> **After this phase, output the full schema as SQL and verify before continuing.**

## Goal

Define and create all Room entities, DAOs, and the database class. No UI in this phase — just the data layer.

---

## Deliverables Checklist

- [ ] `RuleEntity` with `@Entity` annotation
- [ ] `ConditionEntity` with FK to `RuleEntity`
- [ ] `DeletionLogEntity`
- [ ] DAOs for each entity with all needed queries
- [ ] `AppDatabase` class wired up
- [ ] Database compiles and can be instantiated without crashing
- [ ] **Output the full schema as SQL for verification before Phase 4**

---

## 1. Entities

### `RuleEntity`
```kotlin
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val conditionLogic: String,      // "AND" | "OR"
    val targetDirectory: String?,    // null = scan all storage
    val createdAt: Long,             // epoch millis
    val updatedAt: Long
)
```

### `ConditionEntity`
```kotlin
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
```

### `DeletionLogEntity`
```kotlin
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
```

---

## 2. DAOs

### `RuleDao`
```kotlin
@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY updatedAt DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Int): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)
}
```

### `ConditionDao`
```kotlin
@Dao
interface ConditionDao {
    @Query("SELECT * FROM conditions WHERE ruleId = :ruleId")
    suspend fun getConditionsForRule(ruleId: Int): List<ConditionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: List<ConditionEntity>)

    @Query("DELETE FROM conditions WHERE ruleId = :ruleId")
    suspend fun deleteConditionsForRule(ruleId: Int)
}
```

### `DeletionLogDao`
```kotlin
@Dao
interface DeletionLogDao {
    @Query("SELECT * FROM deletion_log ORDER BY runAt DESC")
    fun getAllLogs(): Flow<List<DeletionLogEntity>>

    @Query("SELECT * FROM deletion_log WHERE id = :id")
    suspend fun getLogById(id: Int): DeletionLogEntity?

    @Insert
    suspend fun insertLog(log: DeletionLogEntity): Long

    @Query("DELETE FROM deletion_log WHERE runAt < :beforeEpoch")
    suspend fun deleteLogsOlderThan(beforeEpoch: Long)
}
```

---

## 3. AppDatabase

```kotlin
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
                Room.databaseBuilder(context, AppDatabase::class.java, "smartfilemanager.db")
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
```

---

## 4. Supported Field & Operator Constants

Define these as constants (not magic strings) to be used by both the Rule Builder UI and the Rule Engine:

```kotlin
// domain/model/ConditionField.kt
enum class ConditionField(val label: String, val storedKey: String) {
    DURATION("Duration", "duration"),
    AGE("Age", "age"),
    SIZE("File size", "size"),
    EXTENSION("File extension", "extension"),
    FILE_NAME("File name", "file_name"),
    DIRECTORY("Folder path", "directory")
}

// domain/model/ConditionOperator.kt
enum class ConditionOperator(val label: String, val storedKey: String) {
    LESS_THAN("is less than", "lt"),
    GREATER_THAN("is greater than", "gt"),
    EQUALS("is exactly", "eq"),
    CONTAINS("contains", "contains"),
    NOT_CONTAINS("does not contain", "not_contains")
}
```

---

## 5. Repositories

### `RuleRepository`
```kotlin
class RuleRepository(private val db: AppDatabase) {
    fun getAllRules(): Flow<List<RuleEntity>> = db.ruleDao().getAllRules()
    suspend fun saveRule(rule: RuleEntity, conditions: List<ConditionEntity>)
    suspend fun deleteRule(rule: RuleEntity)
    suspend fun getRuleWithConditions(ruleId: Int): Pair<RuleEntity, List<ConditionEntity>>?
}
```

### `DeletionLogRepository`
```kotlin
class DeletionLogRepository(private val db: AppDatabase) {
    fun getAllLogs(): Flow<List<DeletionLogEntity>> = db.deletionLogDao().getAllLogs()
    suspend fun getLogById(id: Int): DeletionLogEntity?
    suspend fun saveLog(log: DeletionLogEntity)
}
```

---

## 6. File Structure Added in This Phase

```
└── java/com/smartfilemanager/app/
    ├── data/
    │   ├── db/
    │   │   ├── AppDatabase.kt
    │   │   ├── RuleDao.kt
    │   │   ├── ConditionDao.kt
    │   │   └── DeletionLogDao.kt
    │   ├── entity/
    │   │   ├── RuleEntity.kt
    │   │   ├── ConditionEntity.kt
    │   │   └── DeletionLogEntity.kt
    │   └── repository/
    │       ├── RuleRepository.kt
    │       └── DeletionLogRepository.kt
    └── domain/
        └── model/
            ├── ConditionField.kt
            └── ConditionOperator.kt
```

---

## ⚠️ Verification Step — Do This Before Phase 4

After completing this phase, ask Claude Code:

> "Output the full Room schema as raw SQL CREATE TABLE statements."

Verify:
1. All three tables exist with correct column types
2. `conditions.ruleId` has a foreign key to `rules.id` with `ON DELETE CASCADE`
3. There are no typos in column names — the Rule Engine in Phase 6 depends on these exact names