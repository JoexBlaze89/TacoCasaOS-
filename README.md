# Taco Casa OS — Android (Compose) project

This is a Kotlin/Jetpack Compose rewrite of `taco_casa_os.py`, structured as a buildable Android Studio project. The operational state is persisted through Jetpack DataStore and historical operational changes are captured in an event ledger.

## How to build the APK

1. Open this folder (`TacoCasaOS/`) directly in Android Studio.
2. Android Studio can regenerate the missing Gradle wrapper files if needed; `gradle/wrapper/gradle-wrapper.properties` points at Gradle 8.7.
3. Let Gradle sync and resolve the Android, Kotlin, Compose, Serialization, and DataStore dependencies.
4. Build → Build Bundle(s) / APK(s) → Build APK(s).

## Project layout

```
app/src/main/java/com/tacocasa/os/
  model/        — TacoCasaState and @Serializable supporting data classes
  data/         — versioned DataStore persistence + OperationalEvent ledger
  viewmodel/    — TacoCasaViewModel and operational mutations
  ui/theme/     — Compose theme
  ui/components/— reusable kitchen-ticket components
  ui/screens/   — Home, Prep, Inventory, Cleaning, Notes
  MainActivity.kt

app/src/androidTest/java/com/tacocasa/os/
  TacoCasaRepositoryPersistenceTest.kt — write/recreate/read regression tests
```

## Persistence contract

Operational state is encoded as Kotlinx Serialization JSON inside a versioned `PersistedStateEnvelope` and stored in Jetpack DataStore. `LocalDateTime` values use an explicit ISO-8601 serializer. Unknown JSON fields are ignored so additive state-model changes remain backward compatible.

The state schema has an explicit version. Future releases should add migrations in `migrateState()` rather than changing the persisted contract blindly. Legacy raw `TacoCasaState` JSON is also accepted during the transition from the original placeholder implementation.

## Event ledger

Every operational state mutation is observed by the ViewModel persistence loop. The repository persists an append-only bounded ledger of `OperationalEvent` records alongside the current state. Events cover shifts, rush changes, revenue, expenses, staff, inventory, kitchen orders, prep, cleaning, maintenance, notes, complaints, recovery, alerts, and metrics.

This creates the foundation for:

`Observe → Understand → Predict → Recommend → Approve → Act → Verify → Learn`

The current ledger is state-diff based. The next architectural step is to promote important events to first-class domain commands with explicit actor, approval, prediction, outcome, and verification fields.

## Persistence test

`TacoCasaRepositoryPersistenceTest.write_kill_recreate_read_compare` writes a populated operational state, constructs a new repository instance to simulate process recreation, reloads the state, and compares the restored object with the original. A second test verifies the event ledger also survives recreation.

## What's faithfully ported vs. adapted

- Core operational state and business actions remain represented in the Kotlin ViewModel.
- The CLI's numbered `input()` menu is replaced with Compose touch UI and navigation.
- State survives app restarts through DataStore instead of remaining only in memory.
- Historical state changes now survive restart through the event ledger, creating operational memory for later verification and learning.
