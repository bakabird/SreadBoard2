
# Chapter Insights: LLM Chapter Summary & Skip Risk

> Product + UX specification (OpenAI-compatible providers, context windows, orchestration & interruption handling, UI surfaces)

---

## 1) Overview

### Background

* After a user opens a book, the app automatically splits it into chapters based on predefined rules.
* The app supports text-to-speech (TTS / voice reading).

### Goals

Add LLM-powered **Chapter Insights** so users can:

* Understand a chapter faster (**Chapter Summary**)
* Decide faster whether to skip a chapter (**Skip Risk**) using **exactly one** user-facing label (4 levels)

---

## 2) AI Provider & Model Configuration

### Integration

The AI layer may use either:

* an **official OpenAI SDK** (any language), or
* direct calls via an **OpenAI-compatible HTTP protocol**.

With configurable `base_url` and `api_key`, the app can connect to:

* OpenAI’s official service, or
* any third-party / self-hosted / local service that declares **OpenAI compatibility**.

> The product is vendor-agnostic: if a provider offers an OpenAI-compatible Base URL, it can be used.

### Configuration UX: Rules First, Then Bind Features

To avoid scattering settings across features, configuration follows a two-step structure:

1. **Rules (Profiles)**: reusable parameter bundles
2. **Feature Binding**: select which rule each feature uses

#### Rules

Under **Settings → AI → Rules**, users can create/manage multiple rules. Each rule includes:

* `Rule name` (e.g., OpenAI-Cheap, DeepSeek-Fast, Local-Ollama)
* `Base URL`
* `API Key` (optional; some local providers use a placeholder)
* `Model` (free-text allowed)
* Optional advanced options: timeout, network-level retries, concurrency limit, Wi‑Fi-only, battery policy

Each rule provides:

* `Test connection` (connectivity + authentication validation)
* `Set as default rule` (fallback when no explicit binding is available)

#### Feature Binding

Under **Settings → AI → Feature Binding**, provide two selectors:

* **Feature 1 (Summary) uses:** choose an existing rule
* **Feature 2 (Skip Risk) uses:** choose an existing rule

> The two features may use different rules (e.g., Summary on a cheaper model, Skip Risk on a more stable/strong model).

### Security & Privacy

* `API Key` must be stored using secure OS storage (never written to plaintext logs or crash reports).
* When a user selects a third-party provider, show a one-time notice:
  “Chapter text will be sent to your configured AI service.”

---

## 3) Context Windows

The two features use **different** context windows. **Never mix them.**

### Feature 1: Chapter Summary (current chapter first, optional prior context)

* **Required:** Chapter **N** full text
* **Optional enhancement:** if summaries for **N-6..N-1** already exist, include those six summaries in context to keep concepts/characters consistent and reduce repetition.

> Feature 1 does **not** generate missing prior summaries. Only already-available summaries are included.

### Feature 2: Skip Risk (surrounding summaries + current chapter)

* Summaries for **N-3, N-2, N-1**
* Chapter **N** full text
* Summaries for **N+1, N+2, N+3**

> If some chapters do not exist (book boundaries), omit them.

---

## 4) Outputs

### Feature 1 Output: Chapter Summary

* **Summary paragraph:** adaptive length, about **20%** of chapter length
  (e.g., a 1,000-word chapter → ~200 words)
* **Bullets:** 5–10 most important points

  * If the chapter introduces new concepts or new character relationships, they **must** appear in the bullets.
  * Recommended markers: **[NEW CONCEPT]** / **[NEW RELATIONSHIP]**

### Feature 2 Output: Skip Risk

* **User-facing output:** show **exactly one label** (one of four).

Recommended labels (replaceable by product tone):

1. **Filler**
2. **Low Value**
3. **Skip with Caution**
4. **Must Read**

Internal decision dimensions (for model reasoning):

* Advances the main plot/argument (progress, key turn, core conclusion)
* Introduces concepts/relationships that later chapters depend on
* Sets up later chapters and is clearly “paid off”
* Information density (new, non-replaceable information)

---

## 5) Insights Orchestration & Interruption Handling

### One Action, Two Outcomes

Although described as two features, the UX treats them as a single **Chapter Insights** flow.

When the user opens Insights for Chapter **N**:

1. Prepare the required summary set for Feature 2’s context:

* **N-3..N-1, N, N+1..N+3** (only for chapters that exist)

2. For any existing chapter whose summary is missing:

* **enqueue “Generate Chapter Summary” tasks**

3. Compute **Skip Risk** for Chapter **N** as soon as the required surrounding summaries are available:

* Skip Risk requires **(N-3..N-1 summaries) + (N full text) + (N+1..N+3 summaries)**
* Skip Risk does **not** need to wait for Chapter N’s summary to finish

> Only enqueue tasks for existing-but-missing summaries. Already-generated summaries are reused.

### Interruption Cleanup (No Auto-Resume)

If an Insights run is interrupted (process kill, backgrounding, network drop, OS reclaim, etc.), then on next app launch or the next time the user opens any chapter’s Insights:

* perform **cleanup + de-dup** (no background auto-resume)
* cleanup includes:

  * cancel/remove stale queue items (duplicates, stuck “running” states)
  * reset UI so it never stays permanently in “Generating…”
* previously generated summaries / skip-risk labels may be reused
* when the user triggers Insights again for a specific chapter, restart orchestration **only for that chapter** (unrelated to the previously interrupted run)

### Automatic Recovery for Context Dependencies

If a required surrounding summary (N-3..N-1 or N+1..N+3) fails to generate (e.g., network error, model refusal):

1.  **Automatic Retry**: The system must automatically attempt to recover **once** per dependency.
2.  **Cleanup**: Before retrying, the failed/invalid summary record for that specific chapter must be cleared/deleted to ensure a fresh attempt.
3.  **Persistence**: The retry attempt must use the standard generation flow (potentially with `force=true`).
4.  **Failure Propagation**: If the retry also fails, the parent "Skip Risk" task should abort and report failure, ensuring data consistency (no partial/corrupted context used).

---

## 6) UI/UX

### Entry Points

#### TOC / Chapter List (pre-chapter)

Add a compact Insights entry on each chapter row (dynamic display):

* **If Insights is ready** (at minimum, Skip Risk computed): show the **Skip Risk label** directly
* **Otherwise:** show a simple **AI icon / chip**

Interactions:

* Tap chapter title → current behavior (open chapter)
* Tap the Skip-Risk label or AI chip → open **Chapter Insights** (without leaving the list)

#### Reader (during/after reading)

Add a top-level entry for the current chapter. Recommended: an **Insights** button in the bottom row (reachable one-handed).

#### Voice Reading (TTS)

While TTS is playing, surface the same **Insights** entry (in the read-aloud dialog or as a small overlay) to help users decide “keep listening vs skip”.

---

### Primary Component: Chapter Insights Bottom Sheet

Use one bottom sheet with two distinct tabs (**never mix contexts**), opened from TOC or the reader.

> Opening Chapter Insights should immediately start orchestration (no extra “Generate” step required).

#### Header

* Title: `Chapter N · <chapter title>`
* Status: `Ready` / `Generating…` / `Needs context` / `Failed`
* Actions (optional):

  * `Close`
  * `Retry` (with confirmation)

    * Title: `Retry generating insights?`
    * Copy: `This will cancel the current run and restart. Existing results will be kept and reused.`
    * Buttons: `Cancel` / `Retry` (destructive style)

#### Tab A: Summary (Feature 1 context)

* Summary paragraph (~20% length; scrollable)
* 5–10 bullets (preserve markers: **[NEW CONCEPT]**, **[NEW RELATIONSHIP]**)

Controls:

* Auto-generated on first open
* Optional: `Refresh / Regenerate` / `Copy`

#### Tab B: Skip Risk (Feature 2 context)

* Show **one label only** (large centered chip): `Filler` / `Low Value` / `Skip with Caution` / `Must Read`
* Allowed UI actions (model output still remains one label):

  * `Skip this chapter` (jump to N+1)
  * Optional: `Keep reading` / `Keep listening`

---

## 7) Loading, Caching & Task Queue UX

### Summary States (Feature 1)

* `Generating…` → spinner + “Generating summary”
* `Ready` → show content
* `Failed` → error + `Retry`
* `Stale` (chapter changed / re-split) → “Outdated” + `Regenerate`

### Skip-Risk States (Feature 2)

* `Ready` → show the single label (and show it in the TOC entry)
* `Needs context` → “Preparing context…” and (optionally) a checklist of required surrounding summaries

### Visible AI Task Queue

Provide a global queue management surface (accessible from Chapter Insights and Settings).

* **Global: `Abandon all tasks`** (clear the queue, stop all running AI jobs, and reset related UI state)

  * Confirmation:

    * Title: `Abandon all AI tasks?`
    * Copy: `This will stop all AI tasks and clear the queue. Generated results will be kept.`
    * Buttons: `Cancel` / `Abandon` (destructive style)

---

## 8) Microcopy & Guardrails

* One-time opt-in: “This feature sends chapter text to an AI service to generate summaries and skip risk.”
* Do not show any “reasoning” text for Skip Risk in the main UI (keep the promise: **one label only**).
* Near book boundaries: silently omit non-existent chapters from any “needs context” checklist.
