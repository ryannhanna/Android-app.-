# Android Smart File Manager — Build Specification

> A native Android file manager app (APK) with rule-based, automated file deletion using customizable filters and thresholds.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Core Features](#3-core-features)
4. [Filter & Rule System](#4-filter--rule-system)
5. [Screen Structure & Navigation](#5-screen-structure--navigation)
6. [Data Model](#6-data-model)
7. [Android Permissions](#7-android-permissions)
8. [Build Phases](#8-build-phases)
9. [Key Libraries & Dependencies](#9-key-libraries--dependencies)
10. [Project File Structure](#10-project-file-structure)
11. [Out of Scope — Future Ideas](#11-out-of-scope--future-ideas)

---

## 1. Project Overview

A personal Android file manager that lets users browse their device storage and define smart deletion rules. Instead of manually hunting for old or short audio files, users set filter criteria once — and the app finds and removes matching files automatically or with a single confirmation tap.

**Primary Goal:** Rule-based batch file deletion from Android device storage.

**Target User:** Solo personal use, sideloaded via APK.

**Key Principles:**
- Rules are composable — combine multiple conditions with AND/OR logic
- Always show a preview of what will be deleted before any files are removed
- Never delete silently — require explicit user confirmation
- Fast, clean Material Design 3 UI

---

## 2. Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Language | Kotlin | Modern, idiomatic Android development |
| UI Framework | Jetpack Compose | Declarative UI, Material Design 3 |
| Architecture | MVVM + Repository | `ViewModel`, `StateFlow`, `Room` |
| Local Database | Room (SQLite) | Persisting saved rules/filters |
| File Access | `MediaStore` API + `java.io.File` | Scoped storage for Android 10+, broad access for older |
| Background Work | Kotlin Coroutines | Async file scanning and deletion |
| Navigation | Compose Navigation | Single-activity, multi-screen |
| Build System | Gradle (Kotlin DSL) | `.kts` build files |
| Min SDK | API 26 (Android 8.0) | Covers ~95% of active devices |
| Target SDK | API 34 (Android 14) | Latest stable target |

---

## 3. Core Features

### File Browser
- Browse device internal storage and SD card
- Show file name, size, type icon, last modified date, and duration (for audio/video)
- Long-press to manually select and delete individual files
- Sort by name, size, date modified, duration

### Rule Builder
- Create named filter rules with one or more conditions
- Supported condition types (see Section 4 for full list)
- Combine conditions with AND / OR logic
- Save rules for reuse
- Edit and delete saved rules

### Rule Runner — Scan & Preview
- Select a saved rule (or run an ad-hoc filter)
- Choose a target folder to scan (or scan all storage)
- App scans and lists all matching files before any deletion
- Shows: file name, path, size, age, duration (where applicable)
- Total count and total size to be freed displayed prominently

### Confirm & Delete
- User reviews the matched file list
- Can uncheck individual files to exclude from deletion
- Tap "Delete Selected" — requires a final confirmation dialog
- Deletion result screen: files deleted, storage freed, any errors

### Rule History / Log
- Log of every rule run: timestamp, rule name, files deleted, MB freed
- Tap any log entry to see the file list from that run

---

## 4. Filter & Rule System

Rules consist of one or more **conditions**. Each condition has a **field**, an **operator**, and a **value**.

### Supported Condition Fields

| Field | Applies To | Example |
|---|---|---|
| `duration` | Audio, Video | Duration < 60 seconds |
| `age` | All files | Last modified > 25 days ago |
| `size` | All files | File size < 100 KB |
| `extension` | All files | Extension is `.tmp`, `.log` |
| `file_name` | All files | Name contains `"cache"` |
| `mime_type` | All files | MIME type starts with `audio/` |
| `directory` | All files | Path contains `/Downloads/` |

### Supported Operators

| Operator | Symbol | Valid For |
|---|---|---|
| Less than | `<` | duration, age, size |
| Greater than | `>` | duration, age, size |
| Equals | `=` | extension, mime_type |
| Contains | `contains` | file_name, directory |
| Not contains | `not contains` | file_name, directory |

### Condition Logic

- Multiple conditions within a rule can be joined with **AND** (all must match) or **OR** (any must match)
- Mixed AND/OR supported with simple grouping (condition A AND condition B OR condition C)

### Example Rules

```
Rule: "Short Audio Clips"
  duration < 60 seconds
  AND mime_type starts with audio/

Rule: "Old Downloads"
  age > 25 days
  AND directory contains /Download/

Rule: "Temp & Cache Files"
  extension = .tmp
  OR extension = .cache
  OR file_name contains "temp"

Rule: "Large Old Videos"
  size > 500 MB
  AND age > 60 days
  AND mime_type starts with video/
```

---

## 5. Screen Structure & Navigation

```
MainActivity (single Activity)
│
├── HomeScreen
│   ├── "Browse Files" button → FileBrowserScreen
│   ├── "My Rules" button → RulesListScreen
│   ├── "Run a Rule" button → RunRuleScreen
│   └── "History" button → HistoryScreen
│
├── FileBrowserScreen
│   ├── Folder navigation (breadcrumb bar)
│   ├── File list with metadata
│   └── Long-press → manual delete flow
│
├── RulesListScreen
│   ├── List of saved rules (name, condition summary)
│   ├── FAB → RuleBuilderScreen (new rule)
│   └── Tap rule → RuleBuilderScreen (edit)
│
├── RuleBuilderScreen
│   ├── Rule name input
│   ├── Add condition (field, operator, value)
│   ├── AND / OR toggle per condition
│   ├── Folder scope picker (optional)
│   └── Save Rule / Run Now buttons
│
├── RunRuleScreen
│   ├── Select rule from dropdown (or build ad-hoc)
│   ├── Select scan target folder
│   └── "Scan Now" → ScanResultsScreen
│
├── ScanResultsScreen
│   ├── Summary: X files matched, Y MB to free
│   ├── Scrollable file list with checkboxes
│   ├── Uncheck to exclude individual files
│   └── "Delete Selected" → confirmation dialog → DeletionResultScreen
│
├── DeletionResultScreen
│   ├── Files deleted count
│   ├── Storage freed
│   └── Any errors listed
│
└── HistoryScreen
    ├── Log entries: date, rule name, files deleted, MB freed
    └── Tap entry → read-only file list from that run
```

---

## 6. Data Model

### Room Entities

#### `RuleEntity`
```kotlin
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val conditionLogic: String,      // "AND" or "OR"
    val targetDirectory: String?,    // null = scan all storage
    val createdAt: Long,
    val updatedAt: Long
)
```

#### `ConditionEntity`
```kotlin
@Entity(tableName = "conditions")
data class ConditionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleId: Int,                 // FK to RuleEntity
    val field: String,               // "duration", "age", "size", "extension", etc.
    val operator: String,            // "<", ">", "=", "contains", "not_contains"
    val value: String,               // "60", "25", ".tmp", "cache", etc.
    val unit: String?                // "seconds", "days", "MB" — for display
)
```

#### `DeletionLogEntity`
```kotlin
@Entity(tableName = "deletion_log")
data class DeletionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleId: Int?,                // null if ad-hoc run
    val ruleName: String,
    val runAt: Long,
    val filesDeleted: Int,
    val bytesFreed: Long,
    val fileListJson: String         // JSON array of deleted file paths
)
```

### Key Domain Classes (not persisted)

```kotlin
data class ScannedFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,          // epoch millis
    val durationMs: Long?,           // null if not audio/video
    val mimeType: String?
)
```

---

## 7. Android Permissions

### Required Permissions (`AndroidManifest.xml`)

```xml
<!-- Android 9 and below — broad storage access -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="29"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29"/>

<!-- Android 10–12 — scoped media access -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>

<!-- Android 13+ — granular media permissions -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33"/>
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"
    android:minSdkVersion="33"/>
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"
    android:minSdkVersion="33"/>
```

### Runtime Permission Handling
- Request permissions on first launch with a clear explanation screen
- If denied, show a settings deep-link so the user can grant manually
- App will not function without storage permissions — show a blocking UI if not granted

### Sideloading Notes (APK Install)
- User must enable **"Install from unknown sources"** in Android Settings > Security
- The app does **not** require the Play Store — it is fully self-contained
- No internet permission needed — everything runs on-device

---

## 8. Build Phases

| Phase | Name | Deliverables |
|---|---|---|
| **Phase 1** | Project Setup | Init Android project (Kotlin + Compose), Gradle config, Material 3 theme, baseline navigation scaffold |
| **Phase 2** | Permissions & Storage | Runtime permission flow, storage access across API 26–34, basic file listing from `MediaStore` and `java.io.File` |
| **Phase 3** | File Browser | `FileBrowserScreen` — folder navigation, file list with metadata (name, size, date, duration), sort options |
| **Phase 4** | Room Database | `RuleEntity`, `ConditionEntity`, `DeletionLogEntity`, DAOs, database migrations |
| **Phase 5** | Rule Builder UI | `RuleBuilderScreen` — add/remove conditions, field/operator/value pickers, AND/OR toggle, save rule |
| **Phase 6** | Rule Engine | `RuleEngine` class — takes a rule + folder path, scans files, evaluates all conditions, returns `List<ScannedFile>` |
| **Phase 7** | Scan & Preview | `RunRuleScreen` + `ScanResultsScreen` — run rule, display matched files with checkboxes, uncheck to exclude |
| **Phase 8** | Deletion Flow | Confirmation dialog, batch delete via `ContentResolver` / `File.delete()`, `DeletionResultScreen` |
| **Phase 9** | History Log | Write log entries after each run, `HistoryScreen` with log list and detail view |
| **Phase 10** | Polish & APK | Edge cases, empty states, error handling, dark mode, APK build with `./gradlew assembleRelease` |

---

## 9. Key Libraries & Dependencies

```kotlin
// build.gradle.kts (app)

dependencies {
    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Media metadata (duration extraction)
    implementation("androidx.media:media:1.7.0")

    // Gson (for serializing file list in deletion log)
    implementation("com.google.code.gson:gson:2.10.1")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")
}
```

---

## 10. Project File Structure

```
app/src/main/
├── AndroidManifest.xml
└── java/com/yourname/filemanager/
    ├── MainActivity.kt                  # Single activity, hosts Compose NavHost
    │
    ├── data/
    │   ├── db/
    │   │   ├── AppDatabase.kt           # Room database definition
    │   │   ├── RuleDao.kt
    │   │   ├── ConditionDao.kt
    │   │   └── DeletionLogDao.kt
    │   ├── entity/
    │   │   ├── RuleEntity.kt
    │   │   ├── ConditionEntity.kt
    │   │   └── DeletionLogEntity.kt
    │   └── repository/
    │       ├── RuleRepository.kt        # CRUD for rules + conditions
    │       └── DeletionLogRepository.kt
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Rule.kt                  # Domain model (not Room entity)
    │   │   ├── Condition.kt
    │   │   └── ScannedFile.kt
    │   └── engine/
    │       └── RuleEngine.kt            # Core logic: scan folder, evaluate conditions
    │
    ├── ui/
    │   ├── theme/
    │   │   ├── Theme.kt                 # Material 3 theme
    │   │   ├── Color.kt
    │   │   └── Type.kt
    │   ├── navigation/
    │   │   └── NavGraph.kt              # Compose navigation routes
    │   ├── screen/
    │   │   ├── HomeScreen.kt
    │   │   ├── FileBrowserScreen.kt
    │   │   ├── RulesListScreen.kt
    │   │   ├── RuleBuilderScreen.kt
    │   │   ├── RunRuleScreen.kt
    │   │   ├── ScanResultsScreen.kt
    │   │   ├── DeletionResultScreen.kt
    │   │   └── HistoryScreen.kt
    │   └── viewmodel/
    │       ├── FileBrowserViewModel.kt
    │       ├── RuleBuilderViewModel.kt
    │       ├── ScanViewModel.kt
    │       └── HistoryViewModel.kt
    │
    └── util/
        ├── FileMetadataUtil.kt          # Extract duration, MIME type, size
        ├── PermissionUtil.kt            # Runtime permission helpers
        └── FormatUtil.kt               # Human-readable size, duration, date
```

---

## 11. Out of Scope — Future Ideas

- Scheduled / automatic rule runs (WorkManager background jobs)
- Rule sharing — export/import rules as JSON
- Cloud backup of deletion logs
- Duplicate file detection
- Preview images/audio before deleting
- Recycle bin / undo delete (move to trash before permanent removal)
- Widget on home screen showing storage freed
- Multiple rule profiles

---

## How to Build the APK

Once the project is complete:

```bash
# Debug APK (for testing/sideloading)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (optimized, requires signing config)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

Transfer the `.apk` file to your Android device via USB, Google Drive, or email, then open it to install. You must have **"Install from unknown sources"** enabled in **Settings > Security > Install unknown apps**.




# SmartFileManager — Android APK

> **Video-focused file manager with rule-based deletion filters**
> Android 16 · Kotlin · Jetpack Compose · Material Design 3 · Light/Dark mode

---

## How to Use These Files

This spec is split into focused files so Claude Code can work through them one at a time without losing context.

**Always read `DECISIONS.md` first before starting any phase.**

### Build Order

| File | Phase | What Gets Built |
|---|---|---|
| `DECISIONS.md` | — | Project constants, architecture rules, safety rules |
| `PHASE_1_setup.md` | 1 | Project init, Gradle, theme, navigation scaffold |
| `PHASE_2_permissions.md` | 2 | Android 16 permissions, MediaStore access, video file listing |
| `PHASE_3_database.md` | 3 | Room schema — rules, conditions, deletion log |
| `PHASE_4_file_browser.md` | 4 | File browser screen with video thumbnails and metadata |
| `PHASE_5_rule_builder.md` | 5 | Rule builder UI — conditions, AND/OR logic, save/edit |
| `PHASE_6_rule_engine.md` | 6 | Core scan engine — evaluate rules against MediaStore results |
| `PHASE_7_scan_and_delete.md` | 7 | Scan results preview, checkbox selection, deletion flow |
| `PHASE_8_history.md` | 8 | Deletion history log screen |
| `PHASE_9_settings.md` | 9 | Settings screen — light/dark/system theme toggle |
| `PHASE_10_polish.md` | 10 | Empty states, error handling, APK build |

### Recommended Workflow

1. Start a Claude Code session
2. Upload `DECISIONS.md` + the current phase file
3. Complete that phase fully before moving to the next
4. After Phase 3, ask Claude Code to output the Room schema as SQL so you can verify it before continuing
5. After Phase 7, install a debug APK and test deletion on a real device before continuing

---

## App Summary

**SmartFileManager** lets you define named filter rules with conditions like:

- `duration < 60 seconds` — delete short clips
- `age > 25 days` — delete old recordings
- `size < 5 MB` — delete tiny/corrupt files
- `file_name contains "temp"` — delete scratch recordings

Rules are saved, reusable, and always show a preview before anything is deleted.