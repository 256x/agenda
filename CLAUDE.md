# CLAUDE.md — LiteralAgenda

Minimalist text-based agenda app with Git sync. v1.3.1, minSdk 26.

## Build & Test

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
JAVA_HOME=/opt/android-studio/jbr ./gradlew testDebugUnitTest
JAVA_HOME=/opt/android-studio/jbr ./gradlew lint
```

Deploy via Android Studio only. Do not use `gradlew installDebug`.

## Architecture

**Stack:** Kotlin + Jetpack Compose + Navigation Compose + Hilt + ViewModel/StateFlow. AGP, minSdk 26, targetSdk 35.

**Layer structure:**

- `data/` — Repository classes + API interfaces. Data exposed as `Flow`.
  - `EventRepository` — local event storage (plain text files, one file per event)
  - `GitHubRepository` / `GiteaRepository` — Git sync via `GitForge` interface
  - `ICalImporter` — iCal import
  - `SettingsRepository` — DataStore preferences
- `ui/` — Screens + ViewModels (flat, no subdirectory)
  - `MainScreen` / `MainViewModel` — event list + calendar view
  - `EditScreen` / `EditViewModel` — create/edit event
  - `EventDetailScreen` — read-only detail
  - `SettingsScreen` / `SettingsViewModel`

**Navigation:** `NavController` + `NavHost` with string routes (`"main"`, `"edit"`, etc.). `AgendaNavigation()` in `Navigation.kt` is the single entry point.

**DI:** Hilt. ViewModels use `@HiltViewModel` + `@Inject`. Repositories are `@Singleton`.

**Git sync:** `GitForge` interface abstracts GitHub/Gitea. Switching provider = change one SettingsRepository value.

## CLI scripts (`scripts/`)

- `new-event.sh` — create event from terminal
- `search-event.sh` — search events
- `sync-agenda.sh` — manual git sync
- `literalagenda.lua` / `literalagenda.vim` — editor integrations
