package com.smartfilemanager.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfilemanager.app.data.db.AppDatabase
import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.data.repository.RuleRepository
import com.smartfilemanager.app.domain.model.ConditionField
import com.smartfilemanager.app.domain.model.ConditionOperator
import com.smartfilemanager.app.util.PreselectRuleBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// --- Condition field/operator mapping utilities (used by ViewModel and ConditionRow) ---

fun operatorsFor(field: ConditionField): List<ConditionOperator> = when (field) {
    ConditionField.DURATION, ConditionField.AGE, ConditionField.SIZE ->
        listOf(ConditionOperator.LESS_THAN, ConditionOperator.GREATER_THAN)
    ConditionField.EXTENSION ->
        listOf(ConditionOperator.EQUALS)
    ConditionField.FILE_NAME, ConditionField.DIRECTORY ->
        listOf(ConditionOperator.CONTAINS, ConditionOperator.NOT_CONTAINS)
}

fun defaultOperatorFor(field: ConditionField): ConditionOperator = when (field) {
    ConditionField.DURATION, ConditionField.AGE, ConditionField.SIZE -> ConditionOperator.LESS_THAN
    ConditionField.EXTENSION -> ConditionOperator.EQUALS
    ConditionField.FILE_NAME, ConditionField.DIRECTORY -> ConditionOperator.CONTAINS
}

fun defaultUnitFor(field: ConditionField): String? = when (field) {
    ConditionField.DURATION -> "seconds"
    ConditionField.AGE -> "days"
    ConditionField.SIZE -> "MB"
    else -> null
}

// --- UI state models ---

data class ConditionDraft(
    val id: String = UUID.randomUUID().toString(),
    val field: ConditionField = ConditionField.DURATION,
    val operator: ConditionOperator = ConditionOperator.LESS_THAN,
    val value: String = "",
    val unit: String? = "seconds"
)

data class RuleBuilderUiState(
    val ruleId: Int? = null,
    val createdAt: Long = 0L,
    val name: String = "",
    val conditionLogic: String = "AND",
    val conditions: List<ConditionDraft> = emptyList(),
    val targetDirectory: String = "",
    val isSaving: Boolean = false,
    val savedRuleId: Int? = null,  // non-null signals save completed
    val error: String? = null
)

// --- ViewModel ---

class RuleBuilderViewModel(
    private val ruleRepository: RuleRepository,
    private val initialRuleId: Int? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RuleBuilderUiState())
    val uiState: StateFlow<RuleBuilderUiState> = _uiState.asStateFlow()

    init {
        if (initialRuleId != null) loadRule(initialRuleId)
    }

    private fun loadRule(ruleId: Int) {
        viewModelScope.launch {
            val pair = ruleRepository.getRuleWithConditions(ruleId) ?: return@launch
            val (rule, conditions) = pair
            val draftConditions = conditions.map { c ->
                val field = ConditionField.fromKey(c.field) ?: ConditionField.DURATION
                val operator = ConditionOperator.fromKey(c.operator) ?: ConditionOperator.LESS_THAN
                ConditionDraft(field = field, operator = operator, value = c.value, unit = c.unit)
            }
            _uiState.update {
                it.copy(
                    ruleId = rule.id,
                    createdAt = rule.createdAt,
                    name = rule.name,
                    conditionLogic = rule.conditionLogic,
                    conditions = draftConditions,
                    targetDirectory = rule.targetDirectory ?: ""
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun updateLogic(logic: String) {
        _uiState.update { it.copy(conditionLogic = logic) }
    }

    fun updateTargetDirectory(dir: String) {
        _uiState.update { it.copy(targetDirectory = dir) }
    }

    fun addCondition() {
        _uiState.update {
            it.copy(conditions = it.conditions + ConditionDraft(), error = null)
        }
    }

    fun removeCondition(id: String) {
        _uiState.update { it.copy(conditions = it.conditions.filter { c -> c.id != id }) }
    }

    fun updateConditionField(id: String, field: ConditionField) {
        _uiState.update {
            it.copy(conditions = it.conditions.map { c ->
                if (c.id == id) c.copy(
                    field = field,
                    operator = defaultOperatorFor(field),
                    unit = defaultUnitFor(field),
                    value = ""
                ) else c
            })
        }
    }

    fun updateConditionOperator(id: String, operator: ConditionOperator) {
        _uiState.update {
            it.copy(conditions = it.conditions.map { c ->
                if (c.id == id) c.copy(operator = operator) else c
            })
        }
    }

    fun updateConditionValue(id: String, value: String) {
        _uiState.update {
            it.copy(conditions = it.conditions.map { c ->
                if (c.id == id) c.copy(value = value) else c
            }, error = null)
        }
    }

    fun updateConditionUnit(id: String, unit: String) {
        _uiState.update {
            it.copy(conditions = it.conditions.map { c ->
                if (c.id == id) c.copy(unit = unit) else c
            })
        }
    }

    fun prefillForFolder(folderPath: String) {
        val folderName = folderPath.trimEnd('/').substringAfterLast('/').ifEmpty { folderPath }
        val durationCondition = ConditionDraft(
            field = ConditionField.DURATION,
            operator = ConditionOperator.LESS_THAN,
            value = "59",
            unit = "seconds"
        )
        _uiState.update {
            it.copy(
                name = folderName,
                targetDirectory = folderPath,
                conditions = listOf(durationCondition)
            )
        }
    }

    fun clearSavedRuleId() {
        _uiState.update { it.copy(savedRuleId = null) }
    }

    fun saveRule() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Rule name is required") }
            return
        }
        if (state.conditions.isEmpty()) {
            _uiState.update { it.copy(error = "Add at least one condition") }
            return
        }
        val numericFields = setOf(ConditionField.DURATION, ConditionField.AGE, ConditionField.SIZE)
        for (c in state.conditions) {
            if (c.value.isBlank()) {
                _uiState.update { it.copy(error = "All condition values must be filled in") }
                return
            }
            if (c.field in numericFields) {
                val num = c.value.toDoubleOrNull()
                if (num == null || num <= 0) {
                    _uiState.update { it.copy(error = "\"${c.field.label}\" value must be a positive number") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val now = System.currentTimeMillis()
            val ruleEntity = RuleEntity(
                id = state.ruleId ?: 0,
                name = state.name.trim(),
                conditionLogic = state.conditionLogic,
                targetDirectory = state.targetDirectory.trim().ifBlank { null },
                createdAt = if (state.createdAt == 0L) now else state.createdAt,
                updatedAt = now
            )
            val conditionEntities = state.conditions.map { draft ->
                ConditionEntity(
                    id = 0,
                    ruleId = 0,  // set by RuleRepository.saveRule()
                    field = draft.field.storedKey,
                    operator = draft.operator.storedKey,
                    value = draft.value,
                    unit = draft.unit
                )
            }
            val savedId = ruleRepository.saveRule(ruleEntity, conditionEntities)
            // Signal the Run screen to pre-select this rule (new rules only)
            if (state.ruleId == null) PreselectRuleBus.request(savedId)
            _uiState.update { it.copy(isSaving = false, savedRuleId = savedId) }
        }
    }

    companion object {
        fun factory(application: Application, ruleId: Int?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(application)
                    return RuleBuilderViewModel(RuleRepository(db), ruleId) as T
                }
            }
    }
}
