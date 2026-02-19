# CI AND AUTOMATION GUIDE

## OVERVIEW

`.github/` contains workflow automation for web builds, app test/release pipelines, scheduled maintenance, and helper scripts for distribution channels.

## STRUCTURE

```text
.github/
|- workflows/
|  |- web.yml         # build web module and sync assets
|  |- test.yml        # app build/test and distribution paths
|  |- release.yml     # manual release pipeline
|  |- cronet.yml      # scheduled Cronet update automation
|  |- autoupdatefork.yml
|  |- stale.yml
|- scripts/
|  |- cronet.sh       # Cronet version/proguard updater
|  |- lzy_web.py      # Lanzou uploader
|  |- tg_bot.py       # Telegram uploader
```

## WHERE TO LOOK

- Web CI: `.github/workflows/web.yml`
- Android build/test CI: `.github/workflows/test.yml`
- Release automation: `.github/workflows/release.yml`
- Scheduled Cronet updates: `.github/workflows/cronet.yml`, `.github/scripts/cronet.sh`

## CONVENTIONS

- Keep workflow triggers/path filters aligned with module boundaries (`modules/web/**`, app/release files).
- Secrets-gated steps (Lanzou, Telegram, Play upload, signing) must fail safely when unavailable.
- Web assets synced by CI are generated outputs; source remains in `modules/web/`.

## ANTI-PATTERNS

- Do not hardcode credentials/tokens in workflows or scripts.
- Do not remove trigger guards that prevent unintended release/distribution actions.
