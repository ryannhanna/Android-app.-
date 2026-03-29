# Project Decisions & Constants

> This file is the source of truth for all project-wide decisions.
> Reference it in every phase. Do NOT hardcode these values anywhere else.

---

## App Identity

| Decision | Value |
|---|---|
| App name | `SmartFileManager` |
| Package name | `com.smartfilemanager.app` |
| Min SDK | API 36 (Android 16) |
| Target SDK | API 36 (Android 16) |
| Compile SDK | 36 |
| Version name | `1.0.0` |
| Version code | `1` |

---

## Android Version Notes

This app targets **Android 16 (API 36) exclusively**. Do NOT add compatibility shims or fallback code for older Android versions. Specifically:

- Use `READ_MEDIA_VIDEO` permission only — do NOT include legacy `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`
- Use `MediaStore.Video.Media` as the primary file access API
- Use `MediaStore.createDeleteRequest()` for all deletions (introduced in API 30, standard on 16)
- Scoped storage is always in effect — do NOT use `File()` paths to access external storage
- `MANAGE_EXTERNAL_STORAGE` is NOT requested — use `MediaStore` exclusively

---

## Primary File Target

**Video files only.** The app is optimized for video file management.

- Primary `MediaStore` collection: `MediaStore.Video.Media`
- Duration is always available and prominently displayed
- File browser shows video thumbnails where possible (using `MediaStore` thumbnail API)
- Supported extensions for display: `.mp4`, `.mkv`, `.mov`, `.avi`, `.webm`, `.m4v`, `.3gp`, `.ts`
- Duration displayed as `HH:MM:SS` or `MM:SS`
- Size displayed in MB or GB

---

## Theme

- Support **light mode and dark mode**
- Follow **system default** on first launch
- User can override in Settings screen (stored in `DataStore`)
- Use Material Design 3 dynamic color where available (API 31+), with a static fallback palette
- Primary brand color (fallback): `#1565C0` (deep blue)

### Theme preference storage
```kotlin
// Use Jetpack DataStore (Preferences), NOT SharedPreferences
// Key: "theme_preference"
// Values: "system" | "light" | "dark"
```

---

## Architecture Decisions

| Decision | Choice | Reason |
|---|---|---|
| UI | Jetpack Compose | Modern, no XML layouts |
| State | `StateFlow` + `ViewModel` | Lifecycle-safe, Compose-friendly |
| DB | Room | Typed, coroutine-native |
| Prefs | Jetpack DataStore | Replaces SharedPreferences |
| Async | Kotlin Coroutines + Flow | Native Android async |
| DI | Manual (no Hilt) | Simpler for solo project |
| Navigation | Compose Navigation | Single-activity pattern |

---

## Deletion Safety Rules (Non-Negotiable)

1. **Never delete without showing a preview list first**
2. **Always require a confirmation dialog before deletion executes**
3. **Use `MediaStore.createDeleteRequest()` — this triggers the system permission dialog, which is the correct Android 16 behavior**
4. **Log every deletion to Room immediately after it completes**
5. **If any file fails to delete, report it in the result screen — do not silently skip**