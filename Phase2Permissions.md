# Phase 2 — Permissions & MediaStore Video Access

> **Read `DECISIONS.md` before starting this phase.**

## Goal

Request the correct Android 16 storage permission and prove it works by listing real video files from the device using `MediaStore`.

---

## Deliverables Checklist

- [ ] `READ_MEDIA_VIDEO` permission declared in `AndroidManifest.xml`
- [ ] Runtime permission request on first launch
- [ ] Permission denied state — shows explanation UI with Settings deep-link
- [ ] `VideoRepository` can query all video files from `MediaStore.Video.Media`
- [ ] Each `ScannedFile` object populated with: name, path, size, duration, date modified, resolution
- [ ] Permission granted state — list of videos visible in a basic debug screen

---

## 1. Manifest Permission

```xml
<manifest ...>
    <!-- Android 16: READ_MEDIA_VIDEO is the only storage permission needed -->
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

    <application ...>
        ...
    </application>
</manifest>
```

**Do NOT add:**
- `READ_EXTERNAL_STORAGE`
- `WRITE_EXTERNAL_STORAGE`
- `MANAGE_EXTERNAL_STORAGE`

---

## 2. Runtime Permission Flow

```
App launches
    ↓
Check if READ_MEDIA_VIDEO is granted
    ↓ No
Show PermissionScreen
    ├── Explain why permission is needed
    ├── "Grant Permission" button → system dialog
    └── If permanently denied → "Open Settings" button (deep-link to app settings)
    ↓ Yes
Proceed to main NavGraph
```

Create `util/PermissionUtil.kt`:
```kotlin
object PermissionUtil {
    const val VIDEO_PERMISSION = Manifest.permission.READ_MEDIA_VIDEO

    fun isGranted(context: Context): Boolean
    fun shouldShowRationale(activity: Activity): Boolean
    fun openAppSettings(context: Context)  // Intent to Settings.ACTION_APPLICATION_DETAILS_SETTINGS
}
```

---

## 3. Domain Model — `ScannedFile`

```kotlin
// domain/model/ScannedFile.kt
data class ScannedFile(
    val id: Long,                    // MediaStore row ID
    val uri: Uri,                    // content:// URI — use this for deletion
    val name: String,                // Display name
    val path: String,                // Relative path (e.g. /DCIM/Camera/)
    val sizeBytes: Long,
    val durationMs: Long,            // Always populated for video
    val lastModified: Long,          // Epoch millis
    val width: Int?,
    val height: Int?,
    val isSelected: Boolean = false  // UI state for checkbox selection
)
```

---

## 4. VideoRepository — MediaStore Query

```kotlin
// data/repository/VideoRepository.kt

class VideoRepository(private val context: Context) {

    // Returns all video files on the device
    suspend fun getAllVideos(): List<ScannedFile>

    // Returns videos in a specific directory path
    suspend fun getVideosInDirectory(directory: String): List<ScannedFile>
}
```

### MediaStore projection to use:
```kotlin
val projection = arrayOf(
    MediaStore.Video.Media._ID,
    MediaStore.Video.Media.DISPLAY_NAME,
    MediaStore.Video.Media.RELATIVE_PATH,
    MediaStore.Video.Media.SIZE,
    MediaStore.Video.Media.DURATION,
    MediaStore.Video.Media.DATE_MODIFIED,
    MediaStore.Video.Media.WIDTH,
    MediaStore.Video.Media.HEIGHT
)

val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
```

### URI construction:
```kotlin
val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
```

---

## 5. FormatUtil — Implement These Helpers

```kotlin
// util/FormatUtil.kt

object FormatUtil {
    // 3661000ms → "1:01:01"  |  90000ms → "1:30"
    fun formatDuration(ms: Long): String

    // 1048576 → "1.0 MB"  |  1073741824 → "1.0 GB"
    fun formatSize(bytes: Long): String

    // epochMillis → "3 days ago"  |  "Today"  |  "25 Jan 2025"
    fun formatAge(epochMillis: Long): String

    // epochMillis → days since that date (used by RuleEngine in Phase 6)
    fun ageInDays(epochMillis: Long): Long
}
```

---

## 6. File Structure Added in This Phase

```
├── AndroidManifest.xml              # Updated with READ_MEDIA_VIDEO
└── java/com/smartfilemanager/app/
    ├── domain/
    │   └── model/
    │       └── ScannedFile.kt
    ├── data/
    │   └── repository/
    │       └── VideoRepository.kt
    ├── ui/
    │   └── screen/
    │       └── PermissionScreen.kt
    └── util/
        └── PermissionUtil.kt
        └── FormatUtil.kt            # Fully implemented
```

---

## Phase 2 Complete When

Running the app on a real Android 16 device grants permission and `VideoRepository.getAllVideos()` returns a populated list with correct duration, size, and path for at least one video file.