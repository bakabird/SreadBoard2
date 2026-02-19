# WEB MODULE GUIDE

## OVERVIEW

`modules/web` is a standalone Vue 3 + Vite + TypeScript project used for bookshelf/source editing UI. Build artifacts are synced into Android assets.

## STRUCTURE

```text
modules/web/
|- src/
|  |- api/ router/ store/ utils/
|  |- components/ pages/ config/ plugins/
|- scripts/sync.js
|- package.json
|- vite.config.ts
```

## WHERE TO LOOK

- App bootstrap: `modules/web/src/main.ts`, `modules/web/src/App.vue`
- Routing: `modules/web/src/router/`
- State: `modules/web/src/store/`
- API clients: `modules/web/src/api/`
- Build sync behavior: `modules/web/scripts/sync.js`

## CONVENTIONS

- Use Node >=20 and pnpm >=9 (defined in `package.json`).
- Keep lint/typecheck green before syncing assets.
- Source of truth is `modules/web/src/`; generated bundle lives in Android assets.

## ANTI-PATTERNS

- Do not hand-edit files in `app/src/main/assets/web/vue/`.
- Do not rely on Gradle to build this module.

## COMMANDS

```bash
cd modules/web && pnpm i
cd modules/web && pnpm type-check
cd modules/web && pnpm build
cd modules/web && pnpm lint:fix
```
