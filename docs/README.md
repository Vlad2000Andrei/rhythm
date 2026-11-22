# Rhythm Habit Tracker — Documentation

**Rhythm** (`com.rhythm.habittracker`) is an offline-first, native Android habit tracker built with **Kotlin** and **Jetpack Compose**. Its interface and cadence engine implement the **Minimalist Habit Tracker ("Functional Habit Architecture")** design system from Google Stitch (`Project ID: 11168505724477891257`).

---

## Documentation Map

| Guide | Purpose |
| :--- | :--- |
| **[Architecture & Data Flow](./architecture.md)** | Layered architecture, manual dependency injection (`AppContainer`), Navigation 3 routing (`DashboardRoute`, `AllHabitsRoute`, `HabitDetailRoute`, `AddEditHabitRoute`), widget deep-linking (`EXTRA_OPEN_HABIT_ID`), and reactive `StateFlow` pipelines. |
| **[Domain & Cadence Engine](./domain-and-cadence-engine.md)** | Modular domain model (`Habit`, effective-dated `HabitSchedule`, `HabitPauseWindow`, `HabitEditScope`), date-scoped scheduling (`DAILY`, `WEEKLY`, `MONTHLY`, `CUSTOM`), effective-dated schedule updates (`FUTURE_ONLY` vs. `ALL_OCCURRENCES` with smart today/tomorrow cutoff), single-occurrence rescheduling with conflict/pause blocking, override sanitization (`sanitizeRescheduleOverrides` / `hasCadenceChanged` / `hasScheduleOrTimesChanged`), Pixel-Clock pause windows (`addOrUpdatePauseWindow`, `resumeOrCancelPause`, calendar-rhythm resume, prorated `flexiblePoolDueTargetForWeek`), multi-instance daily habits, unified ISO-week maximal-run current/best streak calculation (`calculateStreak` / `calculateBestStreak`), schedule formatting (`formatHabitScheduleSummary`), multi-week heatmap generation with `||` pause bars (32-week Dashboard / 20-week Habit Detail), and pure widget telemetry aggregation (`WidgetDataBuilder`). |
| **[Persistence & Reminders](./persistence-and-reminders.md)** | Room SQLite schema (`rhythm_habits.db`, v5 modular tables `habits`, `habit_schedules`, `habit_pauses`, `habit_completions`, lossless `MIGRATION_4_5`), `HabitWithDetails` `@Transaction` queries, mutex-serialized CRUD & pause/reschedule operations (`RoomHabitRepository`), DataStore draft autosave (`habit_draft_prefs`) for new habits vs. staged in-memory editing for existing habits, persisted domains/preferences, per-widget SharedPreferences (`rhythm_widget_prefs` via `WidgetPreferencesStore`), reactive widget auto-refresh (`RhythmWidgetUpdater`), and date/pause-aware `AlarmManager` notification delivery (`MAX_MANAGED_SLOTS = 20`). |
| **[UI & Design System](./ui-and-design-system.md)** | Monochrome surface palette, 7 accent signatures, `Inter` + `JetBrains Mono` typography tokens, adaptive launcher icon matching `RhythmBrandMark`, expandable Dashboard navigation FAB (`Add Habit` / `All Habits`), collapsible Dashboard `PAUSED TODAY` strip & pause-aware reschedule blocking, All Habits Inventory (`AllHabitsScreen` w/ pause status badges & `"paused"` search), Habit Detail view (`HabitDetailScreen` w/ Pixel-Clock `PauseStatusCard`, `PauseHabitDialog`, `"PAUSED"`/`"STARTS TOMORROW"`/`"MUTED"` pills & 20-week matrix), Add/Edit Habit staged Save Flow (`UNSAVED CHANGES` / `UP TO DATE` badge, Scope & Unsaved Changes dialogs), responsive Light/Dark Android Home Screen Widgets (w/ paused matrix & toggle guard), cancelable checking color-wave animation, screen-by-screen Stitch mapping, and Compose `@Preview` rendering. |

---

## System Overview

```mermaid
flowchart LR
    subgraph UI["UI Layer (Jetpack Compose + Material 3)"]
        Dash["DashboardScreen"]
        AllHabits["AllHabitsScreen"]
        Detail["HabitDetailScreen"]
        AddEdit["AddEditHabitScreen"]
    end

    subgraph VM["Presentation Layer (ViewModels)"]
        DashVM["DashboardViewModel"]
        AllVM["AllHabitsViewModel"]
        DetailVM["HabitDetailViewModel"]
        AddEditVM["AddEditHabitViewModel"]
    end

    subgraph Domain["Domain Layer (Pure Kotlin)"]
        Eval["CadenceEvaluator"]
        Models["HabitModels"]
    end

    subgraph Data["Data & Platform Layer"]
        Repo["RoomHabitRepository"]
        RoomDB[("Room SQLite\nrhythm_habits.db")]
        DraftStore[("Preferences DataStore\nhabit_draft_prefs")]
        Scheduler["AlarmHabitReminderScheduler\n+ HabitReminderReceiver"]
    end

    Dash <--> DashVM
    AllHabits <--> AllVM
    Detail <--> DetailVM
    AddEdit <--> AddEditVM

    DashVM --> Eval
    DashVM --> Repo
    AllVM --> Eval
    AllVM --> Repo
    DetailVM --> Eval
    DetailVM --> Repo
    DetailVM --> Scheduler
    AddEditVM --> Eval
    AddEditVM --> Repo
    AddEditVM --> DraftStore
    AddEditVM --> Scheduler

    Repo <--> RoomDB
    Scheduler -. "Queries completion & schedules next day" .-> Repo
```

---

## Tech Stack & Build Configuration

| Aspect | Value / Version | Source |
| :--- | :--- | :--- |
| **Application ID / Namespace** | `com.rhythm.habittracker` | [app/build.gradle.kts](../app/build.gradle.kts) |
| **SDK Levels** | `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36` | [app/build.gradle.kts](../app/build.gradle.kts) |
| **Android Gradle Plugin (AGP)** | `9.0.1` (built-in Kotlin `2.3.20`, JDK 17 toolchain) | [gradle/libs.versions.toml](../gradle/libs.versions.toml) |
| **Jetpack Compose BOM** | `2026.03.01` (`material3`, `ui`, `material-icons-extended`) | [gradle/libs.versions.toml](../gradle/libs.versions.toml) |
| **Navigation** | AndroidX Navigation 3 (`1.0.1`) + `lifecycle-viewmodel-navigation3` (`2.10.0`) | [Navigation.kt](../app/src/main/kotlin/com/rhythm/habittracker/Navigation.kt) |
| **Database** | AndroidX Room (`2.8.5`) with KSP (`2.3.14`) | [HabitDatabase.kt](../app/src/main/kotlin/com/rhythm/habittracker/data/local/HabitDatabase.kt) |
| **Draft Storage** | AndroidX DataStore Preferences (`1.3.0`) + `kotlinx-serialization-json` (`1.13.0`) | [DraftHabitStore.kt](../app/src/main/kotlin/com/rhythm/habittracker/data/local/DraftHabitStore.kt) |

---

## Common Developer Commands

### 1. Build Debug APK & Run Unit Tests
```powershell
.\gradlew.bat assembleDebug testDebugUnitTest compileDebugAndroidTestKotlin
```
- **Output APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Unit Test Reports**: `app/build/test-results/testDebugUnitTest/*.xml`

### 2. List & Render Headless Compose Previews
Using the Android CLI (`android.exe`):
```powershell
# List all @Preview functions in :app
C:\Users\cursa\AppData\AndroidCLI\android.exe preview list "?:app"

# Render a specific @Preview to PNG without an emulator
C:\Users\cursa\AppData\AndroidCLI\android.exe preview render "?:app" `
  --output=build/previews `
  --fqn=com.rhythm.habittracker.ui.dashboard.DashboardScreenKt.DashboardScreenPreview
```

### 3. Install Over Wireless ADB
For an already-paired phone using Wireless Debugging (`_adb-tls-connect._tcp`), always pass `--user 0` so Android installs only into your Personal profile (never into a Work profile like `User 10`), and start the ADB server + wait ~4 seconds in the same command:
> **Warning**: Never run `.\gradlew.bat connectedDebugAndroidTest` directly against a personal phone with live data—AGP's instrumented test runner automatically uninstalls `com.rhythm.habittracker` at teardown, wiping unbacked-up local database changes. Run `testDebugUnitTest` locally or use a dedicated AVD/emulator for instrumented tests.

```powershell
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe start-server; Start-Sleep -Seconds 4; C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe -s adb-48201FDAS0004G-PnRogK._adb-tls-connect._tcp install --user 0 -r app\build\outputs\apk\debug\app-debug.apk
```
If pairing a new device for the first time:
```powershell
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe pair <hostname-or-ip>:<pairing-port> <6-digit-code>
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe connect <device-ip>:<connect-port>
C:\Users\cursa\AppData\Local\Android\Sdk\platform-tools\adb.exe -s <device-ip>:<connect-port> install --user 0 -r app\build\outputs\apk\debug\app-debug.apk
```
