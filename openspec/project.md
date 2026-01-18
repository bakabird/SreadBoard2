# Project Context

## Purpose
Legado (SreadBoard2) is an advanced, open-source book reader for Android that supports flexible book sources. It allows users to import book sources via JSON/URL, supporting custom parsing rules for searching, TOC, and content extraction. It also features a web-based management interface for easy configuration.

## Tech Stack
- **Languages**: Kotlin (primary), Java (legacy/compat), TypeScript (Web UI)
- **Android**: 
  - Jetpack (Room, ViewModel, LiveData, WorkManager, Preference)
  - View System (XML Layouts, ViewBinding)
  - Material Design Components
- **Web UI (`modules/web`)**:
  - Vite, Vue 3, TypeScript
  - Element Plus (UI Framework)
  - Pinia (State Management)
- **Core Libraries**:
  - **Scripting**: Mozilla Rhino (JavaScript execution for rule parsing)
  - **Networking**: OkHttp, NanoHttpd (local server)
  - **Parsing**: Jsoup, JsoupXpath, JsonPath, Gson
  - **Concurrency**: Kotlin Coroutines
  - **Media**: AndroidX Media3 (ExoPlayer) for TTS/Audio
  - **Image Loading**: Glide
  - **Events**: LiveEventBus
  - **Utils**: Splitties, Hutool

## Project Conventions

### Code Style
- **Kotlin**: Follow official Kotlin coding conventions.
- **Formatting**: Use standard Android Studio formatting.
- **Naming**: 
  - Classes: PascalCase
  - Functions/Variables: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Layout files: `activity_`, `fragment_`, `item_`, `dialog_`, `view_` prefixes.

### Architecture Patterns
- **MVVM**: Model-View-ViewModel architecture is used throughout the app.
  - `ViewModel` manages UI-related data and lifecycle.
  - `Repository` (implied/direct) handles data operations.
  - `Room` handles local database persistence.
- **Modules**:
  - `app`: Main Android application.
  - `modules/book`: Core book parsing and format handling (EPUB/UMD).
  - `modules/rhino`: Scripting engine wrapper.
  - `modules/web`: Web management interface.

### Testing Strategy
- **Unit Tests**: JUnit 4, Mockito (in `test` source sets).
- **Instrumentation**: Espresso (in `androidTest` source sets).

### Git Workflow
- **Commit Messages**: Follow Conventional Commits (supported by `package.json` tooling).
  - Format: `type(scope): subject`
  - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `build`, `ci`.

## Domain Context
- **Book Source (Rule)**: A JSON configuration defining how to search, retrieve TOC, and parse content from a website. Uses CSS selectors, XPath, and JSONPath, often augmented with JS (Rhino).
- **Read Aloud (TTS)**: 
  - System TTS (`TextToSpeech`)
  - HTTP TTS (online API to audio stream)
- **WebDAV**: Used for backing up and syncing reading progress and bookshelves.
- **Chapter Insights**: AI-powered chapter summaries and "skip risk" analysis.

## Important Constraints
- **JDK Version**: Build requires JDK 17+ (Java 8 is not supported for build).
- **Linting**: Use specific tasks like `:app:lintAppDebug` due to ambiguity.
- **Performance**: Heavy reliance on regex and JS evaluation; optimization in parsing rules is critical.
- **Web Assets**: Web UI is built separately and bundled into `app/src/main/assets/web`.

## External Dependencies
- **Firebase**: Analytics and Performance monitoring.
- **Maven Central / JitPack**: Source for most libraries.
- **Github Actions**: Used for CI/CD (release, test, web sync).
