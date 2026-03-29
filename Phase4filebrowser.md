# Phase 4 — File Browser Screen

> **Read `DECISIONS.md` before starting this phase.**

## Goal

A fully functional video file browser. Users can navigate folders, see video thumbnails and metadata, sort the list, and manually long-press to delete individual files.

---

## Deliverables Checklist

- [ ] `FileBrowserScreen` shows all video files from `VideoRepository`
- [ ] Video thumbnails loaded via Coil + MediaStore thumbnail API
- [ ] Each list item shows: thumbnail, filename, duration, size, age, folder path
- [ ] Sort options: Name, Size, Duration, Date Modified
- [ ] Folder filter — tap a folder chip to show only that folder's videos
- [ ] Long-press to enter selection mode — checkboxes appear
- [ ] "Delete X files" action bar appears when files are selected
- [ ] Deletion uses `MediaStore.createDeleteRequest()` with system confirmation dialog
- [ ] Result snackbar: "X files deleted · Y MB freed"

---

## 1. Screen Layout

```
┌─────────────────────────────────┐
│ 📁 All Videos          Sort ▾   │  ← TopAppBar
│ [All] [DCIM] [Downloads] [...]  │  ← Folder filter chips (horizontal scroll)
├─────────────────────────────────┤
│ ┌──────┐ filename.mp4           │
│ │thumb │ 00:03:22 · 145 MB      │  ← Video list item
│ │      │ /DCIM/Camera · 2d ago  │
│ └──────┘                        │
│  ...                            │
└─────────────────────────────────┘

Selection mode (long-press activates):
┌─────────────────────────────────┐
│ ✕  3 selected                   │  ← TopAppBar changes
├─────────────────────────────────┤
│ ☑ ┌──────┐ filename.mp4        │
│   │thumb │ 00:03:22 · 145 MB   │
│   └──────┘                      │
├─────────────────────────────────┤
│      🗑  Delete 3 files          │  ← Bottom action bar
└─────────────────────────────────┘
```

---

## 2. ViewModel

```kotlin
// ui/viewmodel/FileBrowserViewModel.kt

data class FileBrowserUiState(
    val allFiles: List<ScannedFile> = emptyList(),
    val displayedFiles: List<ScannedFile> = emptyList(),
    val folders: List<String> = emptyList(),         // unique folder paths
    val selectedFolder: String? = null,              // null = show all
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

enum class SortOrder(val label: String) {
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    SIZE_ASC("Smallest first"),
    SIZE_DESC("Largest first"),
    DURATION_ASC("Shortest first"),
    DURATION_DESC("Longest first"),
    DATE_ASC("Oldest first"),
    DATE_DESC("Newest first")
}
```

---

## 3. Video Thumbnail Loading

Use Coil with the `VideoFrameDecoder`:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(scannedFile.uri)
        .decoderFactory(VideoFrameDecoder.Factory())
        .build(),
    contentDescription = scannedFile.name,
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
)
```

Show a `VideoFile` icon placeholder while loading or on error.

---

## 4. Deletion via MediaStore

```kotlin
// In ViewModel — delete selected files
suspend fun deleteSelected(activity: Activity) {
    val uris = selectedFiles.map { it.uri }
    val deleteRequest = MediaStore.createDeleteRequest(
        context.contentResolver,
        uris
    )
    // Launch the IntentSender — Android shows the system permission dialog
    activity.startIntentSenderForResult(
        deleteRequest.intentSender,
        DELETE_REQUEST_CODE, null, 0, 0, 0
    )
}
```

Handle the result in the screen via `rememberLauncherForActivityResult`.

---

## 5. File Structure Added in This Phase

```
└── java/com/smartfilemanager/app/
    └── ui/
        ├── screen/
        │   └── FileBrowserScreen.kt
        ├── viewmodel/
        │   └── FileBrowserViewModel.kt
        └── components/
            ├── VideoListItem.kt         # Reusable video row component
            ├── FolderFilterChips.kt     # Horizontal scroll of folder chips
            └── SortDropdownMenu.kt     # Sort order picker
```

---

## Phase 4 Complete When

The file browser shows real video files from the device, thumbnails load correctly, sorting and folder filtering work, and long-press → delete successfully removes a file and updates the list.