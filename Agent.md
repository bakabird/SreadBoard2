# Project Structure

- Root: Gradle Android project `legado`; shared build setup in `build.gradle`, `settings.gradle`, and version catalog under `gradle/` with wrapper files in `gradle/wrapper`.
- Docs: `README.md`, `English.md`, `api.md` (web API usage), `CHANGELOG.md`, `LICENSE`, plus app-facing help and policy markdown files under `app/src/main/assets`.
- CI & scripts: `.github/workflows` for release, test, cronet, web, and stale handling; `.github/scripts` (cronet download, web sync, Telegram bot); issue templates in `.github/ISSUE_TEMPLATE`; emulator helpers `avd.sh`/`avd.bat`.
- Android app (`app`): main module with `src/main` (Java/Kotlin under `io/legado/app` split into api, base, constant, data, exception, help, lib, model, receiver, service, ui, utils, web), resources in `res`, assets (default data, fonts, web UI bundles, help docs, update logs), Room schema snapshots in `schemas/io.legado.app.data.AppDatabase`, and `androidTest`/`test` plus a `debug` resource overlay.
- Libraries:
  - `modules/book`: Java library for EPUB/UMD/book utilities with packages `base`, `epublib`, `umdlib` and supporting resources (`dtd` specs, `log4j.properties`).
  - `modules/rhino`: Android/Kotlin library wrapping Mozilla Rhino scripting (bundles `lib/rhino-1.7.14.jar`) with its own `src/main/java`.
- Web UI (`modules/web`): Standalone Vite + Vue 3/TypeScript app (Element Plus, Pinia) for the bookshelf/source editor; organized into `src` subfolders (`api`, `components`, `hooks`, `pages/bookshelf` and `pages/source`, `router`, `store`, `utils`, `config`, `plugins`, `assets`), with public static files, Vite/TS/ESLint/Prettier configs, and helper scripts under `scripts/`.
- Other: `.api` placeholder directory; top-level `package.json` for commitizen tooling; `modules` includes a `web` module not referenced in `settings.gradle` but maintained separately for the bundled web assets.

# App Navigation Notes

- Bottom navigation menu is defined in `app/src/main/res/menu/main_bnv.xml` and used by `app/src/main/res/layout/activity_main.xml`.
- Default bottom tabs are: Bookshelf (`menu_bookshelf`), Discovery (`menu_discovery`), RSS (`menu_rss`), My (`menu_my_config`).
- `MainActivity` uses `upBottomMenu()` to toggle menu item visibility based on `AppConfig.showDiscovery` and `AppConfig.showRSS`, and keeps `ViewPager` pages aligned via `realPositions` and `bottomMenuCount`.
- Temporary hard-disable for Discovery/RSS is implemented in `MainActivity` via `showDiscoveryTab` and `showRssTab` gating both `upBottomMenu()` and `upHomePage()`.

# Build & Lint Notes

- Android Gradle Plugin in this repo requires a newer JDK; using Java 8 fails dependency resolution. Use Java 17 explicitly when running Gradle: `JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew ...`.
- `:app:lintDebug` is ambiguous; use `:app:lintAppDebug` (or the specific `lintReportAppDebug`/`lintFixAppDebug` tasks) instead.
- Lint may currently fail due to an existing manifest issue: `app/src/main/AndroidManifest.xml` references `androidx.startup.InitializationProvider` but the class is missing from dependencies.
