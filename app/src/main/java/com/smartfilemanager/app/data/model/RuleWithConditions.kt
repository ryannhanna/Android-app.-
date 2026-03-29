package com.smartfilemanager.app.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.data.entity.RuleEntity

data class RuleWithConditions(
    @Embedded val rule: RuleEntity,
    @Relation(parentColumn = "id", entityColumn = "ruleId")
    val conditions: List<ConditionEntity>
)
