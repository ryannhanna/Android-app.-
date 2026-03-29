package com.smartfilemanager.app.domain.model

enum class ConditionOperator(val label: String, val storedKey: String) {
    LESS_THAN("is less than",       "lt"),
    GREATER_THAN("is greater than", "gt"),
    EQUALS("is exactly",            "eq"),
    CONTAINS("contains",            "contains"),
    NOT_CONTAINS("does not contain","not_contains");

    companion object {
        fun fromKey(key: String): ConditionOperator? = entries.find { it.storedKey == key }
    }
}
