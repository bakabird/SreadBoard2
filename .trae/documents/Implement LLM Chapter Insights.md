# Implementation Plan: Chapter Insights & LLM Integration

This plan details the steps to implement the LLM-powered Chapter Insights feature, including database changes, logic implementation, and UI updates.

## 1. Data Layer (Database & Models)

We need to store AI configurations and the generated insights.

### 1.1 New Entities
*   **`AIRule`**: Stores provider configurations.
    *   Fields: `id`, `name`, `baseUrl`, `apiKey`, `model`, `concurrentLimit`, etc.
*   **`ChapterInsight`**: Stores generated results.
    *   Fields: `bookUrl`, `chapterIndex`, `summary` (text), `skipRiskLabel` (int/enum), `status` (generating/ready/failed), `timestamp`.
*   **`AIFeatureBinding`**: Stores which rule is used for which feature.
    *   Fields: `featureId` (Summary/SkipRisk), `ruleId`.

### 1.2 Database Update
*   Modify `io/legado/app/data/AppDatabase.kt` to include `AIRule` and `ChapterInsight` entities.
*   Create `AIRuleDao` and `ChapterInsightDao`.

## 2. Logic Layer (AI Service & Orchestration)

### 2.1 AI Client
*   Create `io/legado/app/model/ai/AIClient.kt`.
*   Implement OpenAI-compatible HTTP client (using `OkHttp` or `Retrofit` if available, or raw HTTP).
*   Handle authentication and configurable base URLs.

### 2.2 Insight Orchestrator
*   Create `io/legado/app/model/ai/InsightManager.kt` (Singleton).
*   **Queue Management**: Handle `GenerateTask` queue.
*   **Context Gathering**:
    *   For Summary: Fetch Chapter N text.
    *   For Skip Risk: Fetch Summaries N-3..N+3 (if available).
*   **Interruption Handling**: Method to clean up stale tasks on app launch.

## 3. UI: Configuration

### 3.1 AI Settings Screen
*   Create `io/legado/app/ui/config/AIConfigActivity.kt`.
*   **Rules Management**: List, Add, Edit, Delete AI Rules.
*   **Feature Binding**: Selectors for "Summary Provider" and "Skip Risk Provider".
*   **Test Connection**: Button to verify API settings.

## 4. UI: Chapter Insights (User Facing)

### 4.1 Insights Bottom Sheet
*   Create `io/legado/app/ui/book/insights/InsightsBottomSheet.kt`.
*   **Tabs**: Summary (Feature 1) and Skip Risk (Feature 2).
*   **States**: Handle Loading, Success, Error views.
*   **Actions**: "Regenerate", "Skip Chapter".

### 4.2 Reader Integration
*   Modify `io/legado/app/ui/book/read/ReadMenu.kt` or `ReadBookActivity.kt`.
*   Add **Insights Button** to the bottom bar or menu.

### 4.3 Table of Contents (TOC) Integration
*   Modify `io/legado/app/ui/book/toc/ChapterListAdapter.kt`.
*   Fetch and display **Skip Risk Label** (e.g., "Filler", "Must Read") next to chapter titles.

## 5. Implementation Steps

1.  **Database**: Create entities and update Room DB.
2.  **Settings UI**: Build the configuration screens to allow adding API keys.
3.  **AI Logic**: Implement the client and manager to fetch data.
4.  **Reader UI**: Connect the UI to the Logic layer.
5.  **TOC UI**: Display the calculated risk labels.
