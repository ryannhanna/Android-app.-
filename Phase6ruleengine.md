# Phase 6 — Rule Engine

> **Read `DECISIONS.md` before starting this phase.**

## Goal

The core evaluation logic. Given a rule (with conditions) and a list of video files, the `RuleEngine` returns only the files that match. This is pure business logic — no UI, no coroutines, fully unit-testable.

---

## Deliverables Checklist

- [ ] `RuleEngine` class implemented with `evaluate()` function
- [ ] All 6 condition fields evaluated correctly
- [ ] AND / OR logic applied correctly
- [ ] Unit tests for each condition type with pass and fail cases
- [ ] Edge cases handled: zero-duration files, missing metadata, empty rules

---

## 1. RuleEngine Interface

```kotlin
// domain/engine/RuleEngine.kt

class RuleEngine {

    /**
     * Evaluates a list of files against a rule.
     * Returns only files that match ALL (AND) or ANY (OR) conditions.
     */
    fun evaluate(
        files: List<ScannedFile>,
        conditions: List<ConditionEntity>,
        logic: String   // "AND" | "OR"
    ): List<ScannedFile> {
        return files.filter { file ->
            if (logic == "AND") {
                conditions.all { condition -> matches(file, condition) }
            } else {
                conditions.any { condition -> matches(file, condition) }
            }
        }
    }

    fun matches(file: ScannedFile, condition: ConditionEntity): Boolean
}
```

---

## 2. Condition Evaluation Logic

Implement `matches()` for each field:

### `duration`
```kotlin
"duration" -> {
    val fileDurationSec = file.durationMs / 1000.0
    val threshold = condition.value.toDoubleOrNull() ?: return false
    when (condition.operator) {
        "lt" -> fileDurationSec < threshold
        "gt" -> fileDurationSec > threshold
        else -> false
    }
}
```

### `age`
```kotlin
"age" -> {
    val ageInDays = FormatUtil.ageInDays(file.lastModified)
    val threshold = condition.value.toLongOrNull() ?: return false
    when (condition.operator) {
        "lt" -> ageInDays < threshold
        "gt" -> ageInDays > threshold
        else -> false
    }
}
```

### `size`
```kotlin
"size" -> {
    // Value is stored in MB or GB — convert to bytes for comparison
    val unit = condition.unit ?: "mb"
    val multiplier = if (unit == "gb") 1_073_741_824L else 1_048_576L
    val thresholdBytes = (condition.value.toDoubleOrNull() ?: return false) * multiplier
    when (condition.operator) {
        "lt" -> file.sizeBytes < thresholdBytes
        "gt" -> file.sizeBytes > thresholdBytes
        else -> false
    }
}
```

### `extension`
```kotlin
"extension" -> {
    val ext = condition.value.trim().lowercase().let {
        if (it.startsWith(".")) it else ".$it"
    }
    file.name.lowercase().endsWith(ext)
}
```

### `file_name`
```kotlin
"file_name" -> {
    val target = condition.value.trim().lowercase()
    when (condition.operator) {
        "contains"     -> file.name.lowercase().contains(target)
        "not_contains" -> !file.name.lowercase().contains(target)
        else           -> false
    }
}
```

### `directory`
```kotlin
"directory" -> {
    val target = condition.value.trim().lowercase()
    when (condition.operator) {
        "contains"     -> file.path.lowercase().contains(target)
        "not_contains" -> !file.path.lowercase().contains(target)
        else           -> false
    }
}
```

---

## 3. Unit Tests

Create `RuleEngineTest.kt` in `src/test/`. Cover:

| Test | Expected |
|---|---|
| `duration < 60s` on a 30s video | Match |
| `duration < 60s` on a 90s video | No match |
| `age > 25 days` on a 30-day-old file | Match |
| `age > 25 days` on a 10-day-old file | No match |
| `size < 10 MB` on a 5 MB file | Match |
| `size > 1 GB` on a 500 MB file | No match |
| `extension = .mp4` on `video.mp4` | Match |
| `extension = .mp4` on `video.mkv` | No match |
| `file_name contains "temp"` on `temp_clip.mp4` | Match |
| `file_name not_contains "temp"` on `temp_clip.mp4` | No match |
| AND logic — both conditions must match | Only files matching both |
| OR logic — either condition matches | Files matching either |
| Empty conditions list | Return all files |
| File with `durationMs = 0` | Treat as 0 seconds, not null |

---

## 4. File Structure Added in This Phase

```
└── java/com/smartfilemanager/app/
    └── domain/
        └── engine/
            └── RuleEngine.kt

src/test/java/com/smartfilemanager/app/
    └── domain/
        └── engine/
            └── RuleEngineTest.kt
```

---

## Phase 6 Complete When

All unit tests pass. The `RuleEngine` correctly filters a manually constructed list of `ScannedFile` objects for every condition type and both AND/OR logic modes.