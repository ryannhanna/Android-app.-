package com.smartfilemanager.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Signals ScanViewModel to pre-select a rule by ID after it is created in
 * RuleBuilderViewModel. Uses StateFlow so the value is replayed to new
 * collectors (i.e. ScanViewModel created after the event is emitted).
 * Consumers must call consume() once they've acted on the value.
 */
object PreselectRuleBus {
    private val _pendingRuleId = MutableStateFlow<Int?>(null)
    val pendingRuleId = _pendingRuleId.asStateFlow()

    fun request(ruleId: Int) {
        _pendingRuleId.value = ruleId
    }

    fun consume() {
        _pendingRuleId.value = null
    }
}
