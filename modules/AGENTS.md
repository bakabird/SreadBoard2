# MODULES GUIDE

## OVERVIEW

`modules/` contains reusable libraries plus a standalone web frontend that is built separately from the Android Gradle graph.

## STRUCTURE

```text
modules/
|- book/   # Java EPUB/UMD parser/writer library
|- rhino/  # Kotlin Rhino scripting wrapper library
|- web/    # Vue 3 + Vite + TypeScript editor frontend
```

## WHERE TO LOOK

- EPUB/UMD internals: `modules/book/src/main/java/me/ag2s/`
- Rhino interop: `modules/rhino/src/main/java/com/script/`
- Web app source: `modules/web/src/`
- Web build/sync script: `modules/web/scripts/sync.js`

## CONVENTIONS

- Keep module boundaries clean: parser/runtime code here, Android app logic in `app/`.
- `modules/web` is not included in `settings.gradle`; build it with pnpm, not Gradle tasks.
- Java/Kotlin library modules target Java 17 toolchain.

## ANTI-PATTERNS

- Do not couple `book` or `rhino` directly to `app` UI/state classes.
- Do not edit generated web bundle output under `app/src/main/assets/web/vue/` from here.
