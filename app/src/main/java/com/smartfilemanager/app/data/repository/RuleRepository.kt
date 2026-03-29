package com.smartfilemanager.app.data.repository

import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.data.model.RuleWithConditions
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val db: AppDatabase) {

    fun getAllRules(): Flow<List<RuleEntity>> = db.ruleDao().getAllRules()

    fun getAllRulesWithConditions(): Flow<List<RuleWithConditions>> = db.ruleDao().getAllRulesWithConditions()

    suspend fun getRuleWithConditions(ruleId: Int): Pair<RuleEntity, List<ConditionEntity>>? {
        val rule = db.ruleDao().getRuleById(ruleId) ?: return null
        val conditions = db.conditionDao().getConditionsForRule(ruleId)
        return Pair(rule, conditions)
    }

    suspend fun saveRule(rule: RuleEntity, conditions: List<ConditionEntity>) {
        val ruleId = if (rule.id == 0) {
            db.ruleDao().insertRule(rule).toInt()
        } else {
            db.ruleDao().updateRule(rule)
            rule.id
        }
        db.conditionDao().deleteConditionsForRule(ruleId)
        db.conditionDao().insertConditions(conditions.map { it.copy(ruleId = ruleId) })
    }

    suspend fun deleteRule(rule: RuleEntity) {
        db.ruleDao().deleteRule(rule)
        // Conditions are cascade-deleted by the FK constraint
    }
}
