<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# PROJECT KNOWLEDGE BASE

**Generated:** 2026-02-19 14:55 Asia/Shanghai
**Commit:** cf3c507c1
**Branch:** master

## OVERVIEW

Legado is a multi-module Android repository with a separately maintained Vue/Vite web editor bundled into app assets for release builds.

## STRUCTURE

```text
SreadBoard2/
|- app/                 # Android application module
|- modules/book/        # Java EPUB/UMD utility library
|- modules/rhino/       # Kotlin Rhino scripting wrapper
|- modules/web/         # Vue 3 + Vite + TypeScript frontend
|- .github/             # CI workflows and release scripts
|- openspec/            # Spec/proposal workflow documents
|- AGENTS.md            # root guide (this file)
```

## HIERARCHY

- `app/AGENTS.md`
- `modules/AGENTS.md`
- `modules/web/AGENTS.md`
- `modules/book/AGENTS.md`
- `modules/rhino/AGENTS.md`
- `.github/AGENTS.md`
- `openspec/AGENTS.md`

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Android startup flow | `app/src/main/AndroidManifest.xml`, `app/src/main/java/io/legado/app/App.kt` | Launcher + app init |
| Reader UI/features | `app/src/main/java/io/legado/app/ui/` | Most user-facing screens |
| Data and DB | `app/src/main/java/io/legado/app/data/`, `app/schemas/` | Room entities/dao/schema snapshots |
| Read-aloud services | `app/src/main/java/io/legado/app/service/` | Local TTS + HTTP TTS |
| Web editor source | `modules/web/src/` | Build from here, do not hand-edit bundled output |
| Web bundled assets | `app/src/main/assets/web/vue/` | Generated from `modules/web` build |
| EPUB/UMD internals | `modules/book/src/main/java/` | Parsing and domain library |
| Rhino engine integration | `modules/rhino/src/main/java/` | Script engine and wrappers |
| CI/release automation | `.github/workflows/`, `.github/scripts/` | Build/release/distribution flow |

## CODE MAP

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `App` | `Application` | `app/src/main/java/io/legado/app/App.kt` | global initialization |
| `MainActivity` | Activity | `app/src/main/java/io/legado/app/ui/main/MainActivity.kt` | app shell/navigation |
| `ReadBookActivity` | Activity | `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt` | reader runtime |
| `InsightManager` | Singleton | `app/src/main/java/io/legado/app/model/ai/InsightManager.kt` | chapter AI task orchestration |
| `TTSReadAloudService` | Service | `app/src/main/java/io/legado/app/service/TTSReadAloudService.kt` | system TTS playback |
| `HttpReadAloudService` | Service | `app/src/main/java/io/legado/app/service/HttpReadAloudService.kt` | network audio playback |

## CONVENTIONS

- Android build requires Java 17; run Gradle with `JAVA_HOME=$(/usr/libexec/java_home -v 17)`.
- Use `:app:lintAppDebug` instead of `:app:lintDebug` (ambiguous task name).
- Version and plugin management is centralized in `gradle/libs.versions.toml` and `gradle.properties`.
- Web module uses `pnpm` (Node >=20, pnpm >=9) and its own lint/typecheck pipeline.
- `modules/web` is not included in `settings.gradle`; it is built separately and synced to app assets.

## ANTI-PATTERNS (THIS PROJECT)

- Do not edit bundled web output in `app/src/main/assets/web/vue/` by hand.
- Do not change mirror repository lines in `settings.gradle` marked "do not commit changes".
- Do not use legacy Java versions for Gradle tasks.
- Do not remove Rhino copyright headers (`DO NOT ALTER OR REMOVE COPYRIGHT NOTICES...`).

## COMMANDS

```bash
# Android
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:assembleDebug
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:testDebugUnitTest --no-daemon
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :app:lintAppDebug --no-daemon

# Web module
cd modules/web && pnpm i
cd modules/web && pnpm type-check
cd modules/web && pnpm build
```

## NOTES

- OpenSpec change planning lives in `openspec/`; keep the managed block at top of this file intact.
- CI builds web assets in `modules/web` then commits synced output under `app/src/main/assets/web/vue/`.
- For module-specific guidance, read the nearest nested `AGENTS.md` before making changes.
