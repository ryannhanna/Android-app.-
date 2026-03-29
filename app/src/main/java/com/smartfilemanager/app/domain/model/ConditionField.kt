package com.smartfilemanager.app.domain.model

enum class ConditionField(val label: String, val storedKey: String) {
    DURATION("Duration",       "duration"),
    AGE("Age",                 "age"),
    SIZE("File size",          "size"),
    EXTENSION("File extension","extension"),
    FILE_NAME("File name",     "file_name"),
    DIRECTORY("Folder path",   "directory");

    companion object {
        fun fromKey(key: String): ConditionField? = entries.find { it.storedKey == key }
    }
}
