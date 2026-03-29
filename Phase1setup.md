# Phase 1 — Project Setup

> **Read `DECISIONS.md` before starting this phase.**

## Goal

Create a working Android project skeleton: compiling, themed, and navigable — but with no real functionality yet.

---

## Deliverables Checklist

- [ ] Android project created with Kotlin + Jetpack Compose
- [ ] Gradle configured per DECISIONS.md
- [ ] Material Design 3 theme with light/dark support
- [ ] DataStore dependency added (for theme preference)
- [ ] Bottom navigation bar scaffold with 4 tabs
- [ ] App builds and runs on device/emulator without errors

---

## 1. Gradle Configuration

### `build.gradle.kts` (project level)
```kotlin
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.9.22" apply false
}
```

### `build.gradle.kts` (app level)
```kotlin
android {
    namespace = "com.smartfilemanager.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.smartfilemanager.app"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures { compose = true }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore (theme preference)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson (deletion log serialization)
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil (video thumbnails)
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-video:2.5.0")
}
```

---

## 2. Theme Setup

Create `ui/theme/Theme.kt` with full light/dark support and a system-default fallback.

```kotlin
enum class AppTheme { SYSTEM, LIGHT, DARK }
```

The theme preference is loaded from DataStore (see Phase 9 for the Settings screen). In Phase 1, just wire up the theme to accept an `AppTheme` parameter and apply it — the DataStore reading comes later.

**Fallback color palette (used when dynamic color is unavailable):**

| Token | Light | Dark |
|---|---|---|
| Primary | `#1565C0` | `#90CAF9` |
| Background | `#FFFFFF` | `#121212` |
| Surface | `#F5F5F5` | `#1E1E1E` |
| On-surface | `#1A1A1A` | `#E0E0E0` |

---

## 3. Navigation Scaffold

Four bottom nav tabs. These are shells only in Phase 1 — screens are built in later phases.

| Tab | Icon | Route | Screen (built in phase) |
|---|---|---|---|
| Files | `VideoFile` | `files` | Phase 4 |
| Rules | `Rule` | `rules` | Phase 5 |
| Run | `PlayArrow` | `run` | Phase 7 |
| History | `History` | `history` | Phase 8 |

Settings is accessible via a gear icon in the top app bar (not a bottom tab).

---

## 4. File Structure to Create in This Phase

```
app/src/main/
├── AndroidManifest.xml          # Permissions added in Phase 2 — leave mostly empty for now
└── java/com/smartfilemanager/app/
    ├── MainActivity.kt
    ├── ui/
    │   ├── theme/
    │   │   ├── Theme.kt
    │   │   ├── Color.kt
    │   │   └── Type.kt
    │   └── navigation/
    │       └── NavGraph.kt
    └── util/
        └── FormatUtil.kt        # Stub file — add formatDuration(), formatSize(), formatAge()
```

---

## Phase 1 Complete When

The app launches, shows a bottom nav bar with 4 tabs, each tab navigates to a placeholder screen, and the app compiles cleanly with no warnings.