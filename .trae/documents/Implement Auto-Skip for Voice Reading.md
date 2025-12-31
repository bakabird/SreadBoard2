I will implement the **Auto-Skip** functionality for Voice Reading, integrated with the existing AI Insights system.

### 1. **Core Logic: Stricter Skip Risk & Event Notification**
**File:** `app/src/main/java/io/legado/app/model/ai/InsightManager.kt`
- **Refine Prompt**: Update the prompt in `generateSkipRisk` to raise the threshold for "Must Read" and ensure the default tendency is conservative (avoid false positives for "Must Read").
- **Add Notification**: Post an `EventBus.INSIGHT_UPDATED` event when a Skip Risk or Chapter Summary is successfully generated. This allows the TTS service to react immediately when data becomes available.

### 2. **Settings: Auto-Skip Configuration**
**Files:** 
- `app/src/main/java/io/legado/app/constant/PreferKey.kt`
- `app/src/main/res/xml/pref_config_aloud.xml`
- `app/src/main/res/values/strings.xml`

- **Add Preferences**:
  - `readAloudAutoSkip` (Switch): Enable/Disable Auto-Skip.
  - `readAloudSkipConditions` (MultiSelect): Select which risk levels to skip (e.g., "Filler", "Low Value").
- **UI Update**: Add these controls to the Read Aloud Configuration dialog so users can set rules *before* reading.

### 3. **TTS Service: Auto-Skip Orchestration**
**File:** `app/src/main/java/io/legado/app/service/BaseReadAloudService.kt`

- **Intercept `newReadAloud`**:
  - Before starting to read a chapter, check if `readAloudAutoSkip` is enabled.
  - **Check Skip Risk**:
    - **If Missing**: 
      - Pause reading (`pauseReadAloud(abandonFocus = false)`).
      - Show a Toast: "Waiting for AI skip risk analysis...".
      - Enter a "Waiting" state and listen for `INSIGHT_UPDATED` events.
    - **If Exists**:
      - Compare with `readAloudSkipConditions`.
      - **If Should Skip**:
        - Check for **Chapter Summary**.
          - **If Summary Missing**: Pause and wait (similar to above).
          - **If Summary Exists**: 
            - Replace the main content with the **Chapter Summary**.
            - Log/Toast: "Skipping chapter based on AI Insight."
            - Proceed to read the summary.
      - **If Should Read**: Proceed with original content.

- **Resume Logic**:
  - When an `INSIGHT_UPDATED` event is received for the current chapter:
    - If in "Waiting" state, re-evaluate the skip logic and resume reading automatically.

### 4. **Verification**
- **Test Case 1 (Normal)**: Auto-Skip disabled -> Reads full text.
- **Test Case 2 (Skip)**: Auto-Skip enabled, Chapter is "Filler", Summary exists -> Reads Summary.
- **Test Case 3 (Wait)**: Auto-Skip enabled, Risk/Summary missing -> Pauses, waits for generation, then resumes with correct content (Summary or Full Text).
- **Test Case 4 (No Skip)**: Auto-Skip enabled, Chapter is "Must Read" -> Reads full text.