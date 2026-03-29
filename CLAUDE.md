# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SmartFileManager** — A native Android APK for rule-based, automated video file deletion using customizable filters. Designed for personal sideloaded use (no Play Store).

- Package: `com.smartfilemanager.app`
- Min/Target/Compile SDK: **36 (Android 16 only)** — no fallback code for older versions
- Language: Kotlin with Jetpack Compose

## Build Commands

```bash
./gradlew assembleDebug     # Build debug APK
./gradlew assembleRelease   # Build release APK
./gradlew test              # Run unit tests
./gradlew testDebugUnitTest # Run a specific test variant
```

Sideload the APK to device via `adb install app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

**MVVM + Repository pattern** with a single-activity Compose UI.

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material Design 3 |
| Navigation | Compose Navigation (single activity, 4 bottom nav tabs: Files, Rules, Run, History) |
| State | ViewModel + StateFlow |
| Database | Room (3 entities: `RuleEntity`, `ConditionEntity`, `DeletionLogEntity`) |
| Preferences | Jetpack DataStore (theme: `"system"` | `"light"` | `"dark"`) |
| File Access | MediaStore API (`MediaStore.Video.Media`) — no direct `File` paths to external storage |
| Async | Kotlin Coroutines + Flow |
| DI | Manual injection (no Hilt) |

## Key Architectural Decisions (Non-Negotiable)

**Storage & Permissions:**
- `READ_MEDIA_VIDEO` is the only storage permission — never request `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`
- All deletions via `MediaStore.createDeleteRequest()` — never direct file deletion
- Scoped storage always in effect

**Deletion Safety (must be enforced at all times):**
1. Always show a preview list before any deletion
2. Always require a confirmation dialog before executing deletion
3. Log every deletion to Room immediately after completion
4. Report failures explicitly — never silently skip a failed deletion

**File Scope:**
- Video files only: `.mp4`, `.mkv`, `.mov`, `.avi`, `.webm`, `.m4v`, `.3gp`, `.ts`
- Duration always displayed as `HH:MM:SS` or `MM:SS`
- Video thumbnails sourced from MediaStore

**Theme:**
- Material Design 3 with dynamic colors (API 31+), static fallback palette with primary `#1565C0`

## Key Source Locations (once implemented)

- `MainActivity.kt` — Single activity, hosts Compose NavHost
- `data/db/AppDatabase.kt` — Room database
- `data/entity/` — 3 Room entities
- `data/repository/` — CRUD repositories
- `domain/engine/RuleEngine.kt` — Core rule evaluation logic (AND/OR condition chains)
- `ui/screen/` — Screen composables (FileBrowser, RuleBuilder, RuleRunner, History, Settings)
- `ui/viewmodel/` — 4 ViewModels

## Rule Condition Types

Rules are composed of conditions with AND/OR logic:
- `duration` — `<` or `>` seconds
- `age` — `<` or `>` days
- `size` — `<` or `>` MB
- `extension` — equals `.mp4`, `.mkv`, etc.
- `file_name` — contains / not contains
- `mime_type` — equals `audio/*`, `video/*`, etc.
- `directory` — contains / not contains

## Implementation Phases

| Phase | Focus |
|-------|-------|
| 1 | Project setup: Gradle config, Compose theme, navigation scaffold |
| 2 | Runtime permissions, MediaStore API setup |
| 3 | Room schema (Rules, Conditions, Deletion Log) |
| 4 | File browser with video thumbnails and metadata |
| 5 | Rule builder UI with AND/OR condition logic |
| 6 | Rule engine: scan and evaluate rules against MediaStore |
| 7 | Scan preview, checkbox selection, deletion flow |
| 8 | History screen (deletion audit log) |
| 9 | Settings screen (theme toggle) |
| 10 | Empty states, error handling, APK polish |

## Key Dependencies

- Compose BOM: `2024.02.00`
- Room: `2.6.1` (kapt for annotation processing)
- Navigation Compose: `2.7.7`
- Lifecycle ViewModel Compose: `2.7.0`
- DataStore Preferences: `1.0.0`
- Coroutines Android: `1.7.3`
- Coil Compose: `2.5.0` (video thumbnails)
- Gson: `2.10.1`
- Android Gradle Plugin: `8.3.0`
- Kotlin: `1.9.22`
