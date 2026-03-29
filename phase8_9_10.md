# Phase 8 — Deletion History Log

> **Read `DECISIONS.md` before starting this phase.**

## Goal

A history screen showing every rule run: when it ran, what rule was used, how many files were deleted, and how much space was freed. Users can tap any entry to see the file list from that run.

---

## Deliverables Checklist

- [ ] `HistoryScreen` — list of deletion log entries from Room
- [ ] Each entry shows: date/time, rule name, file count, MB freed
- [ ] Tap entry → `HistoryDetailScreen` with the file list from that run
- [ ] Empty state when no history exists
- [ ] Swipe-to-delete to remove a log entry

---

## Screen Layout

```
┌─────────────────────────────────┐
│  History                        │
├─────────────────────────────────┤
│  Today, 2:34 PM                 │
│  Short Clips · 11 files · 338MB │
│                                 │
│  Yesterday, 10:12 AM            │
│  Old Downloads · 4 files · 1.2GB│
│  ...                            │
└─────────────────────────────────┘
```

### History Detail Screen

```
┌─────────────────────────────────┐
│ ← Short Clips — Mar 28, 2:34 PM │
├─────────────────────────────────┤
│  11 files · 338 MB freed        │
├─────────────────────────────────┤
│  clip_001.mp4   12 MB           │
│  /DCIM/Camera                   │
│  ...                            │
└─────────────────────────────────┘
```

Files in the detail view are read-only — they are already deleted. Display them as a plain list with name, path, and size parsed from `fileListJson`.

---

## File Structure Added in This Phase

```
└── ui/
    ├── screen/
    │   ├── HistoryScreen.kt
    │   └── HistoryDetailScreen.kt
    └── viewmodel/
        └── HistoryViewModel.kt
```

---

## Phase 8 Complete When

Running two rule scans produces two entries in the history screen. Tapping each shows the correct file list. Swiping deletes the log entry.

---
---

# Phase 9 — Settings Screen (Theme Toggle)

> **Read `DECISIONS.md` before starting this phase.**

## Goal

A settings screen where the user can choose light mode, dark mode, or follow system default. The preference is saved to DataStore and applied immediately across the whole app.

---

## Deliverables Checklist

- [ ] Settings screen accessible via gear icon in top app bar
- [ ] Three-option theme toggle: System / Light / Dark
- [ ] Selection saved to DataStore with key `"theme_preference"`
- [ ] Theme applies immediately on selection — no restart required
- [ ] Correct option highlighted on relaunch

---

## DataStore Setup

```kotlin
// data/preferences/ThemePreferenceStore.kt

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object ThemePreferenceStore {
    val THEME_KEY = stringPreferencesKey("theme_preference")

    // Returns Flow<AppTheme> — "system" | "light" | "dark"
    fun getTheme(context: Context): Flow<AppTheme>
    suspend fun setTheme(context: Context, theme: AppTheme)
}
```

---

## Wiring Theme to App Root

In `MainActivity.kt`, collect the theme preference as state and pass it to the `Theme.kt` composable:

```kotlin
val themePreference by themeStore.getTheme(context).collectAsState(initial = AppTheme.SYSTEM)

SmartFileManagerTheme(appTheme = themePreference) {
    NavGraph(...)
}
```

---

## Settings Screen Layout

```
┌─────────────────────────────────┐
│ ← Settings                      │
├─────────────────────────────────┤
│  Appearance                     │
│                                 │
│  Theme                          │
│  ○ Follow system default        │
│  ○ Light                        │
│  ● Dark                         │
│                                 │
│  About                          │
│  SmartFileManager v1.0.0        │
└─────────────────────────────────┘
```

---

## File Structure Added in This Phase

```
└── java/com/smartfilemanager/app/
    ├── data/
    │   └── preferences/
    │       └── ThemePreferenceStore.kt
    └── ui/
        └── screen/
            └── SettingsScreen.kt
```

---

## Phase 9 Complete When

Switching to Dark mode in Settings immediately changes the app to dark theme. Closing and reopening the app restores the last selected theme.

---
---

# Phase 10 — Polish & APK Build

> **Read `DECISIONS.md` before starting this phase.**

## Goal

Handle all edge cases, add empty states and loading indicators, verify dark mode looks correct across all screens, and produce a release APK ready to sideload.

---

## Deliverables Checklist

### Empty States
- [ ] File browser — no videos found
- [ ] Rules list — no rules created yet (with CTA to create one)
- [ ] Run rule — no rules exist (with CTA)
- [ ] Scan results — zero files matched rule
- [ ] History — no runs yet

### Loading States
- [ ] File browser loading spinner while `VideoRepository` queries MediaStore
- [ ] Scan progress indicator (indeterminate) while `RuleEngine` scans
- [ ] Skeleton/placeholder while video thumbnails load

### Error Handling
- [ ] Permission denied — shown on file browser and run rule screens
- [ ] MediaStore query failure — show retry button
- [ ] Deletion partial failure — shown clearly on result screen
- [ ] Room database error — log to console, show user-friendly message

### Dark Mode Audit
- [ ] All screens render correctly in dark mode
- [ ] No hardcoded white/black colors — use Material 3 tokens only (`MaterialTheme.colorScheme.*`)
- [ ] Video thumbnails have correct dark background placeholder

### APK Build

```bash
# Debug APK for sideloading (no signing required)
./gradlew assembleDebug

# Output
app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer the .apk file via:
# - USB file transfer (copy to Downloads, tap to install)
# - Google Drive upload → open on device
# - Email attachment
```

**Before installing:** Enable "Install unknown apps" in:
`Settings → Apps → Special app access → Install unknown apps → [your file manager] → Allow`

---

## Final Smoke Test Checklist

Run through this on the physical device before calling it done:

- [ ] Fresh install — permission screen appears
- [ ] Grant permission — file browser shows real videos
- [ ] Create rule: `duration < 30 seconds AND age > 7 days`
- [ ] Run rule — scan results show correct files
- [ ] Uncheck one file
- [ ] Delete — system dialog appears
- [ ] Deletion result shows correct count
- [ ] History has one entry
- [ ] Tap history entry — file list matches
- [ ] Switch to dark mode — all screens look correct
- [ ] Switch to light mode — all screens look correct
- [ ] Restart app — theme preference preserved, rules still there

---

## Phase 10 Complete When

The debug APK installs cleanly on Android 16, passes all smoke tests above, and all screens look correct in both light and dark mode.