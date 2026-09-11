# TacoCasaOS Kotlin Multiplatform Migration

## Current Status: Phase 1-2 Complete ✅

This document tracks the migration from Android-only to Kotlin Multiplatform (KMP).

### What's Been Done

#### Phase 1: Multiplatform Gradle Structure
- ✅ Created `shared/` module with KMP configuration
- ✅ Set up platform-specific source sets: `commonMain`, `androidMain`, `iosMain`, `desktopMain`
- ✅ Updated root `build.gradle.kts` with KMP plugins
- ✅ Updated `settings.gradle.kts` to include shared module

#### Phase 2: Business Logic Migration
- ✅ Moved all data models to `shared/src/commonMain/kotlin/`
  - `TacoCasaState.kt` — All enums and data classes
  - Support for serialization via `@Serializable`
- ✅ Moved core `TacoCasaViewModel` to shared code
  - Platform-independent business logic
  - `expect`/`actual` declarations for persistence
- ✅ Created platform-specific persistence layers:
  - Android: DataStore (in `androidMain/`)
  - iOS: UserDefaults/Keychain (in `iosMain/`)
  - Desktop: JSON files (in `desktopMain/`)
- ✅ Refactored ViewModel to use `kotlinx.datetime` instead of `java.time`

#### Android App Refactoring
- ✅ Updated `androidApp/build.gradle.kts` to depend on `:shared`
- ✅ Created Android UI layer in `androidApp/src/main/kotlin/`
  - `MainActivity.kt` — Entry point, navigation
  - `ui/screens/HomeScreen.kt` — First screen implementation
- ✅ Maintained Compose as UI framework

### Architecture

```
TacoCasaOS-KMP/
├── shared/                    # Shared Kotlin Multiplatform code
│   ├── src/commonMain/
│   │   └── kotlin/com/tacocasa/os/
│   │       ├── model/         # Data models (platform-independent)
│   │       └── viewmodel/     # Business logic with expect/actual
│   ├── src/androidMain/       # Android DataStore persistence
│   ├── src/iosMain/           # iOS UserDefaults persistence
│   └── src/desktopMain/       # Desktop JSON persistence
├── androidApp/                # Android UI layer (depends on shared)
├── iosApp/                    # iOS app (planned)
├── desktopApp/                # Desktop app (planned)
└── build.gradle.kts           # Root Gradle with KMP plugins
```

### Next Steps

#### Phase 3: UI Sharing with Compose Multiplatform
- [ ] Add Compose Multiplatform dependencies to `shared/build.gradle.kts`
- [ ] Move UI screens to `shared/src/commonMain/compose/`
- [ ] Share Compose theme across platforms
- [ ] Test on Android with shared UI

#### Phase 4: iOS Integration
- [ ] Create Xcode project in `iosApp/`
- [ ] Set up Kotlin/Native framework compilation
- [ ] Create SwiftUI wrappers for Compose or shared ViewModel
- [ ] Test Android + iOS parity

#### Phase 5: Desktop Integration
- [ ] Complete `desktopApp/` with Compose for Desktop
- [ ] Add Compose Multiplatform dependencies
- [ ] Create desktop entry point
- [ ] Test Android + Desktop parity

### How to Build & Test

**Android:**
```bash
./gradlew :androidApp:assembleDebug
# Or in Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)
```

**Shared Tests:**
```bash
./gradlew :shared:commonTest
```

**Desktop (when ready):**
```bash
./gradlew :desktopApp:run
```

### Key Decisions

1. **Serialization:** Using `kotlinx-serialization` for persistence across platforms
2. **DateTime:** Using `kotlinx-datetime` instead of `java.time` for platform independence
3. **Persistence Abstraction:** `expect`/`actual` pattern allows platform-specific storage:
   - Android: DataStore (modern, recommended)
   - iOS: UserDefaults + Keychain
   - Desktop: JSON files in home directory
4. **UI Strategy:** Gradual migration to Compose Multiplatform (Phase 3)

### Testing Progress

- [ ] Android app builds with shared module ← **Next: Run this!**
- [ ] Shared tests pass
- [ ] Android app runs and displays HomeScreen
- [ ] State persists across app restarts (Android)
- [ ] iOS framework compiles
- [ ] iOS app runs
- [ ] Desktop app runs

### Troubleshooting

**"Cannot find symbol TacoCasaViewModel"** on Android build:
- Ensure `:androidApp/build.gradle.kts` has `implementation(project(":shared"))`
- Run `./gradlew clean` and retry

**Kotlin version mismatch:**
- Check that all modules use `kotlin("multiplatform") version "1.9.24"`
- Sync Gradle: File → Sync Now (Android Studio)

**Missing DataStore on Android:**
- Verify `shared/build.gradle.kts` has DataStore only in `androidMain` dependencies
- Don't add it to `commonMain`
