# APP MODULE GUIDE

## OVERVIEW

`app/` is the Android application module. It owns runtime behavior, UI, services, Room persistence, and embedded web/help assets.

## STRUCTURE

```text
app/
|- src/main/java/io/legado/app/
|  |- ui/        # screens, fragments, adapters
|  |- model/     # reading, AI, orchestration
|  |- data/      # Room DB, entities, DAOs, migrations
|  |- service/   # foreground/background Android services
|  |- help/      # bootstrap/default data/config helpers
|- src/main/res/ # layouts, menus, preferences, drawables
|- src/main/assets/
|  |- web/       # bundled web/help content
|  |- defaultData/
|- schemas/io.legado.app.data.AppDatabase/
```

## WHERE TO LOOK

- App startup: `app/src/main/AndroidManifest.xml`, `app/src/main/java/io/legado/app/App.kt`
- Main shell/navigation: `app/src/main/java/io/legado/app/ui/main/MainActivity.kt`
- Reader runtime: `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`
- Room database: `app/src/main/java/io/legado/app/data/AppDatabase.kt`
- Migrations: `app/src/main/java/io/legado/app/data/DatabaseMigrations.kt`
- Read aloud: `app/src/main/java/io/legado/app/service/BaseReadAloudService.kt`

## CONVENTIONS

- Keep long-running work in foreground services with proper notification channels.
- Persist schema changes with Room migrations and updated snapshots in `app/schemas/...`.
- Use existing event flow (`EventBus`, LiveEventBus patterns) instead of ad-hoc global state.
- Feature defaults/import templates belong in `app/src/main/assets/defaultData/`.

## ANTI-PATTERNS

- Do not hand-edit generated web bundle files in `app/src/main/assets/web/vue/`.
- Do not introduce DB schema changes without migration handling and schema snapshot updates.
- Do not bypass service lifecycle cleanup (wake locks, media session, receivers).

## COMMANDS

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:assembleDebug
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --no-daemon
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:lintAppDebug --no-daemon
```
