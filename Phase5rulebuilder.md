# Phase 5 — Rule Builder UI

> **Read `DECISIONS.md` before starting this phase.**

## Goal

A screen where users create and edit named filter rules. Each rule has a name, one or more conditions, AND/OR logic, and an optional target folder. Rules are saved to Room.

---

## Deliverables Checklist

- [ ] `RulesListScreen` — list of saved rules with name and condition summary
- [ ] FAB on list screen opens blank `RuleBuilderScreen`
- [ ] Tapping a rule opens it in `RuleBuilderScreen` for editing
- [ ] Rule name input (required)
- [ ] Add condition button — opens condition editor row
- [ ] Each condition: field picker → operator picker → value input → unit label
- [ ] AND / OR toggle applies to all conditions in the rule
- [ ] Optional target folder picker (directory path input)
- [ ] Save rule → persists to Room via `RuleRepository`
- [ ] Delete rule → removes rule and all conditions (CASCADE)
- [ ] Swipe-to-delete on rules list

---

## 1. Rules List Screen Layout

```
┌─────────────────────────────────┐
│  My Rules                       │
├─────────────────────────────────┤
│  Short Clips                    │
│  duration < 60s · age > 7d  AND │
│                                 │
│  Old Downloads                  │
│  age > 25d · /Download/      OR │
│                                 │
│  Temp Files                     │
│  name contains "temp"        OR │
└─────────────────────────────────┘
                              [ + ]  ← FAB
```

---

## 2. Rule Builder Screen Layout

```
┌─────────────────────────────────┐
│ ← New Rule              Save    │
├─────────────────────────────────┤
│ Rule name                       │
│ [Short Clips               ]    │
│                                 │
│ Conditions                      │
│ Match  [AND ▾]  of the following│
│                                 │
│ [Duration ▾] [less than ▾] [60] seconds  🗑│
│ [Age     ▾] [greater than▾] [7] days     🗑│
│                                 │
│ [ + Add condition ]             │
│                                 │
│ Scan folder (optional)          │
│ [/DCIM/Camera              ]    │
│                                 │
│ [ Run Now ]    [ Save Rule ]    │
└─────────────────────────────────┘
```

---

## 3. Condition Row — Field/Operator/Value Logic

The value input type changes based on the selected field:

| Field | Value Input | Unit Label |
|---|---|---|
| `duration` | Number keyboard | "seconds" |
| `age` | Number keyboard | "days" |
| `size` | Number keyboard + unit toggle (MB/GB) | "MB" or "GB" |
| `extension` | Text (e.g. `.mp4`) | none |
| `file_name` | Text | none |
| `directory` | Text (path) | none |

Operator options change based on field:

| Field | Available Operators |
|---|---|
| `duration`, `age`, `size` | less than, greater than |
| `extension` | is exactly |
| `file_name`, `directory` | contains, does not contain |

---

## 4. ViewModel

```kotlin
data class RuleBuilderUiState(
    val ruleId: Int? = null,             // null = new rule
    val name: String = "",
    val conditionLogic: String = "AND",  // "AND" | "OR"
    val conditions: List<ConditionDraft> = emptyList(),
    val targetDirectory: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

data class ConditionDraft(
    val id: String = UUID.randomUUID().toString(),  // local ID for list key
    val field: ConditionField = ConditionField.DURATION,
    val operator: ConditionOperator = ConditionOperator.LESS_THAN,
    val value: String = "",
    val unit: String? = "seconds"
)
```

---

## 5. Validation Rules

Before saving, validate:
- Rule name is not blank
- At least one condition exists
- All condition values are non-blank
- Numeric fields (`duration`, `age`, `size`) have a valid positive number as value

Show inline error messages — do not use Toast.

---

## 6. File Structure Added in This Phase

```
└── java/com/smartfilemanager/app/
    └── ui/
        ├── screen/
        │   ├── RulesListScreen.kt
        │   └── RuleBuilderScreen.kt
        ├── viewmodel/
        │   └── RuleBuilderViewModel.kt
        └── components/
            ├── ConditionRow.kt          # Single editable condition row
            ├── FieldPickerDropdown.kt
            └── OperatorPickerDropdown.kt
```

---

## Phase 5 Complete When

A user can create a rule named "Short Clips" with the condition `duration < 60 seconds`, save it, see it in the list, tap to edit it, and delete it via swipe. All data persists after app restart.