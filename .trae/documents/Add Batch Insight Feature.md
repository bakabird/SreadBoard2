# Add "Batch Insight" Feature

I will implement the ability to batch generate AI insights (specifically "Skip Risk" analysis) for upcoming chapters. This involves updates to the UI, the logic manager, and the layout.

## 1. UI Layout Update
**File:** `app/src/main/res/layout/dialog_chapter_insights.xml`
- Add a "Batch" button (`TextView`) to the header of the Insights dialog, positioned next to the existing "Queue" button.

## 2. Logic Implementation
**File:** `app/src/main/java/io/legado/app/model/ai/InsightManager.kt`
- Add a new function `generateBatchSkipRisk(book: Book, startIndex: Int, count: Int)`.
- This function will:
    - Launch a background coroutine (`Dispatchers.IO`).
    - Verify the total chapter count to prevent out-of-bounds errors.
    - Iterate from the current chapter index forward by `count`.
    - Call the existing `generateSkipRisk` for each chapter (which handles deduplication and dependency resolution).

## 3. UI Interaction Logic
**File:** `app/src/main/java/io/legado/app/ui/book/insights/InsightsBottomSheet.kt`
- In `onViewCreated`, set up a click listener for the new "Batch" button.
- Implement `showBatchAnalyzeDialog()` which displays a selection dialog with options:
    - "Next 10 Chapters"
    - "Next 20 Chapters"
    - "Next 50 Chapters"
    - "Custom Input..."
- Implement `showCustomBatchDialog()` to allow the user to input a specific number of chapters.
- Connect these dialog actions to `InsightManager.generateBatchSkipRisk`.
