# Phase 7 — Scan, Preview & Delete Flow

> **Read `DECISIONS.md` before starting this phase.**
> **After this phase, install the debug APK on a real device and test before continuing.**

## Goal

The main end-to-end flow: pick a rule, scan for matching files, review the results with checkboxes, and execute deletion with confirmation.

---

## Deliverables Checklist

- [ ] `RunRuleScreen` — pick a saved rule and target folder
- [ ] "Scan Now" triggers async scan via `VideoRepository` + `RuleEngine`
- [ ] `ScanResultsScreen` — matched files list with checkboxes and summary
- [ ] Uncheck individual files to exclude from deletion
- [ ] "Select All" / "Deselect All" toggle
- [ ] "Delete X files · Y MB" bottom action bar
- [ ] Confirmation dialog with file count and size
- [ ] Deletion via `MediaStore.createDeleteRequest()` (system dialog)
- [ ] `DeletionResultScreen` — success count, errors, storage freed
- [ ] Deletion log written to Room on completion

---

## 1. Run Rule Screen

```
┌─────────────────────────────────┐
│  Run a Rule                     │
├─────────────────────────────────┤
│  Select rule                    │
│  [Short Clips (duration<60s) ▾] │
│                                 │
│  Scan folder                    │
│  ○ All storage                  │
│  ● Specific folder              │
│  [/DCIM/Camera             ]    │
│                                 │
│  ┌───────────────────────────┐  │
│  │      Scan Now 🔍          │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

If no rules have been saved yet, show an empty state with a button to go create one.

---

## 2. Scan Results Screen

```
┌─────────────────────────────────┐
│ ← Results: Short Clips  ☐ All  │
├─────────────────────────────────┤
│  12 files matched · 340 MB      │  ← Summary bar
├─────────────────────────────────┤
│ ☑ ┌──────┐ clip_001.mp4        │
│   │thumb │ 00:00:45 · 12 MB    │
│   └──────┘ /DCIM/ · 3d ago     │
│                                 │
│ ☑ ┌──────┐ screen_rec.mp4      │
│   │thumb │ 00:00:22 · 8 MB     │
│   └──────┘ /Movies/ · 1d ago   │
│  ...                            │
├─────────────────────────────────┤
│  🗑  Delete 12 files · 340 MB   │  ← Bottom action bar
└─────────────────────────────────┘
```

If zero files matched, show an empty state: "No videos matched this rule."

---

## 3. Confirmation Dialog

```
Delete 12 videos?

This will permanently delete 12 files
and free 340 MB of storage. This
cannot be undone.

[ Cancel ]    [ Delete ]
```

"Delete" button is red. After tapping "Delete", launch `MediaStore.createDeleteRequest()`.

---

## 4. Deletion Result Screen

```
┌─────────────────────────────────┐
│  Deletion Complete              │
├─────────────────────────────────┤
│                                 │
│         ✓ 11 files deleted      │
│           338 MB freed          │
│                                 │
│         ✗ 1 file failed         │
│           clip_003.mp4          │
│           (tap for details)     │
│                                 │
│  [ Back to Rules ]              │
└─────────────────────────────────┘
```

---

## 5. ScanViewModel

```kotlin
data class ScanUiState(
    val savedRules: List<RuleEntity> = emptyList(),
    val selectedRule: RuleEntity? = null,
    val targetDirectory: String? = null,     // null = all storage
    val isScanning: Boolean = false,
    val scanResults: List<ScannedFile> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isDeleting: Boolean = false,
    val deletionResult: DeletionResult? = null,
    val error: String? = null
)

data class DeletionResult(
    val deletedCount: Int,
    val failedFiles: List<String>,
    val bytesFreed: Long
)
```

---

## 6. Writing the Deletion Log

After deletion completes, write to Room:

```kotlin
val log = DeletionLogEntity(
    ruleId = selectedRule?.id,
    ruleName = selectedRule?.name ?: "Ad-hoc",
    runAt = System.currentTimeMillis(),
    filesDeleted = result.deletedCount,
    bytesFreed = result.bytesFreed,
    fileListJson = gson.toJson(deletedFiles.map {
        mapOf("name" to it.name, "path" to it.path, "sizeBytes" to it.sizeBytes)
    })
)
deletionLogRepository.saveLog(log)
```

---

## 7. File Structure Added in This Phase

```
└── java/com/smartfilemanager/app/
    └── ui/
        ├── screen/
        │   ├── RunRuleScreen.kt
        │   ├── ScanResultsScreen.kt
        │   └── DeletionResultScreen.kt
        └── viewmodel/
            └── ScanViewModel.kt
```

---

## ⚠️ Real Device Test — Do This Before Phase 8

Install the debug APK on your Android 16 device:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Test this exact flow:
1. Create a rule: `duration < 10 seconds`
2. Run it against `/DCIM/Camera`
3. Verify matched files look correct
4. Uncheck one file
5. Delete — confirm the system dialog appears
6. Verify the file is actually gone from your gallery app

Only continue to Phase 8 once deletion works correctly on a real device.