# Taco Casa OS — Android (Compose) project

This is a full Kotlin/Jetpack Compose rewrite of `taco_casa_os.py`, structured as a
buildable Android Studio project. Every feature from the original console script has
a matching screen and view-model method — see the comments in
`TacoCasaViewModel.kt` for a method-by-method mapping back to the Python source.

## How to build the APK

1. Open this folder (`TacoCasaOS/`) directly in Android Studio (File → Open).
2. Android Studio will detect there's no `gradlew`/`gradle-wrapper.jar` in this
   folder and will offer to regenerate them automatically — accept that prompt
   (or run `gradle wrapper` once if you have Gradle installed locally). This step
   is unavoidable on my end: the wrapper jar is a compiled binary and I don't have
   network access in this environment to fetch the real one, so I left
   `gradle/wrapper/gradle-wrapper.properties` in place (pointing at Gradle 8.7)
   but omitted the jar/scripts rather than ship a fake or corrupt binary.
3. Let Gradle sync — it will pull the Android Gradle Plugin, Kotlin, and Compose
   dependencies listed in `app/build.gradle.kts`.
4. Build → Build Bundle(s) / APK(s) → Build APK(s). The debug APK lands in
   `app/build/outputs/apk/debug/app-debug.apk`. You can sideload that directly
   onto a phone or emulator.
5. For a signed release build, use Build → Generate Signed Bundle / APK and
   follow Android Studio's signing wizard (you'll need to create a keystore if
   you don't have one yet).

## Project layout

```
app/src/main/java/com/tacocasa/os/
  model/        — TacoCasaState and supporting data classes (port of the Python class's state)
  data/         — DataStore-backed persistence (port of save_data/load_data)
  viewmodel/    — TacoCasaViewModel, one method per Python method
  ui/theme/     — color tokens matching the web build's kitchen-ticket palette
  ui/components/— the reusable "Ticket" card (the signature visual element)
  ui/screens/   — Home, Prep, Inventory, Cleaning, Notes — one per nav tab
  MainActivity.kt
```

## What's faithfully ported vs. adapted

- All business logic (rush prediction, labor/food cost math, inventory thresholds,
  alert conditions, complaint-recovery flows) is ported as-is from the Python source.
- The CLI's numbered `input()` menu is replaced with real buttons, a persistent
  bottom nav, and live-updating timers — since "finish into an app" implies an
  actual touch interface, not a text menu running in a terminal emulator.
- State now survives app restarts via Jetpack DataStore (the Python version's
  in-memory state didn't persist between runs at all).
