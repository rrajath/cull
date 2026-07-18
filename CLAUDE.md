# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Cull** (`com.rrajath.occullt`) is an Android photo-culling app built with Kotlin and Jetpack Compose. Users review photos from local device storage and/or a self-hosted Immich server, pin a reference photo, long-press to compare, and swipe down to mark photos for deletion.

- **Min SDK:** Android 14 (API 34), **Target SDK:** Android 15 (API 36)
- **Build system:** Gradle with Kotlin DSL; single `:app` module
- **No DI framework** — manual `ViewModelFactory` pattern throughout

## Commands

```bash
# Build
./gradlew build

# Unit tests (JVM)
./gradlew test

# Run a single unit test
./gradlew test --tests "com.rrajath.occullt.feature.stacks.StacksViewModelTest"

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rrajath.occullt.core.database.ImmichAssetMappingDbTest

# Lint
./gradlew lint

# Debug APK
./gradlew assembleDebug
```

## Architecture

**MVVM + Clean Architecture** with three layers:

```
core/
  database/        # SQLite (no Room) for Immich↔local asset mapping
  datastore/       # DataStore preferences + PhotoCache (in-memory)
  model/           # UnifiedPhotoItem, PhotoSource, enums
  network/         # ImmichApi (OkHttp3), ImmichRepository, LocalPhotoRepository
feature/
  home/            # Entry screen, source switcher
  library/         # LazyVerticalGrid, LibraryViewModel
  viewer/          # Full-screen viewer, ViewerViewModel
  stacks/          # Time-window photo groups, StacksViewModel
  settings/        # Source mode, folder picker, Immich config
navigation/        # OcculltNavHost, type-safe Route definitions, SessionViewModel
ui/
  component/       # GiantButton, CircleIcon, SourceSwitcher, ContinuePill, etc.
  icon/            # CullIcons custom icon set
  theme/           # Color.kt (Catppuccin palette), Type.kt, Theme.kt
```

### Key Models

- **`UnifiedPhotoItem`** — unified local + Immich photo representation; fields include `source`, `immichAssetId`, `isOnDevice`, `isOnImmich`, thumbnail/preview/original URLs
- **`SourceMode`** — `Local` | `Immich` | `Hybrid`

### Repositories

| Repository | Responsibility |
|---|---|
| `LocalPhotoRepository` | Device photos via SAF (`DocumentFile`) |
| `ImmichRepository` | Immich API; wraps `ImmichApi` |
| `UnifiedPhotoRepository` | Merges both sources |
| `SettingsRepository` | DataStore for all 14 persisted prefs |
| `PhotoCache` | In-memory list cache for Stacks feature |

### ViewModels

- **`LibraryViewModel`** — photo loading, pagination (7-day rolling window for Immich), mark/pin state, date filters; factory takes `(context, settingsRepository)`
- **`StacksViewModel`** — groups photos by configurable time window (default 2 min); uses `PhotoStackCache` (stack index → photos)
- **`ViewerViewModel`** — full-screen viewer state: current index, HUD visibility, pin/mark, delete dialog
- **`SessionViewModel`** — nav-scoped; persists last photo index & folder URI; `reloadTrigger: StateFlow` broadcasts reloads

### Navigation

Type-safe routes via `@Serializable` data objects/classes in `navigation/Route.kt`:
- `Home`, `Library`, `Viewer(photoIndex, folderUri)`, `Settings`, `Stacks`, `StackGrid(stackIndex)`
- All wired in `OcculltNavHost`; `SessionViewModel` scoped to the nav host

## State Persistence (DataStore)

All keys in `SettingsRepository.kt`:

| Key | Type | Default |
|---|---|---|
| `source_mode` | SourceMode | Hybrid |
| `library_folder_uri` | String | — |
| `immich_url` | String | — |
| `immich_api_key` | String | — |
| `long_press_threshold` | Int (ms) | 220 |
| `dry_run` | Boolean | false |
| `mirror_deletes` | Boolean | false |
| `dark_theme` | Boolean | true |
| `accent_hue` | Int (0–7) | 0 |
| `last_photo_index` | Int | — |
| `grouping_window_minutes` | Int | 2 |

All exposed as `Flow<T>`.

## Feature Details

### Deletion
- **Local**: `MediaStore.createTrashRequest()` (API 30+) — never hard-deletes
- **Immich**: `DELETE /api/assets` with `{ids: [...], force: false}`
- **Dry-run mode**: skips actual deletion; shows toast
- Confirmation dialog shows counts: N on device / N on Immich / N on both

### Long-Press Compare
- Hold threshold configurable (default 220 ms); shows pinned photo full-screen
- Suppresses system context menu
- Zoom/pan of current photo mirrors to pinned photo

### Immich API (`ImmichApi.kt`)
- Auth: `x-api-key` header injected by OkHttp interceptor in `OcculltApplication`; `ImmichKeyGate` only allows it when the request's scheme/host/port match the configured Immich URL and the path is an `/api/assets/` endpoint
- Cleartext HTTP is blocked app-wide (`network_security_config.xml`) — server URLs must be `https://`
- Pagination: offset-based (`page` + `size`), no cursor
- Search: `POST /api/search/metadata` → results at `assets.items` (nested)
- JSON parsed manually via `JsonElement`/`JsonObject` (no Moshi/Gson)

### Application Setup (`OcculltApplication`)
- Initializes Coil 3 singleton with OkHttp fetcher (memory cache 25%, disk 2%)
- Stores Immich credentials (API key + base URL) globally (companion object); updated when settings change

## Testing

### Unit tests (`/src/test/`)
Uses `mockk` + `kotlinx.coroutines.test`. Key test files:
- `ImmichApiTest` — API response parsing
- `ImmichRepositoryTest` — photo fetching, URL gen, deletion
- `ImmichApiRequestTest` — MockWebServer-backed request tests (URL encoding, auth header, delete body)
- `ImmichKeyGateTest` — API-key host-scoping rules
- `SettingsExportTest` — settings-import sanitization
- `StacksViewModelTest` — grouping algorithm
- `ViewerViewModelTest` — state transitions

### Instrumented tests (`/src/androidTest/`)
Uses `MockWebServer` for HTTP, real DataStore/SQLite:
- `ImmichAssetMappingDbTest` — SQLite CRUD
- `SettingsRepositoryTest` — DataStore read/write
- `ImmichApiMockWebServerTest` — full HTTP stack

## Key Files

| File | Purpose |
|---|---|
| `OcculltApplication.kt` | Coil setup, Immich API key storage, OkHttp interceptor |
| `navigation/OcculltNavHost.kt` | Navigation graph |
| `navigation/Route.kt` | All route definitions |
| `core/datastore/SettingsRepository.kt` | All persistent state |
| `core/network/ImmichApi.kt` | Immich HTTP client |
| `core/database/ImmichAssetMappingDb.kt` | Local↔Immich filename mapping (singleton, double-checked locking) |
| `ui/theme/Color.kt` | Catppuccin-based palette + `ExtendedColorScheme` |
| `core/network/ImmichKeyGate.kt` | Host-scoping gate for API-key injection |
| `gradle/libs.versions.toml` | All dependency versions |
| `docs/SECURITY_AUDIT.md` | Security audit findings and remediation status |
