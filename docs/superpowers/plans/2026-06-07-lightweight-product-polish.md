# Lightweight Product Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a demo-friendly lightweight version of five product polish features: transparent conversion stages, quality checks, enhanced history browsing, richer export options, and scene-level AI editing.

**Architecture:** Keep the backend synchronous and file-based. Add a focused quality analysis service and expose quality data through conversion responses/history items. Extend the existing AI editing service with a scene-level endpoint, while the frontend keeps all UI enhancements in the current Vue/Element Plus page.

**Tech Stack:** Spring Boot, Spring AI `ChatClient`, JUnit 5, Vue 3, TypeScript, Element Plus, browser Blob/print APIs.

---

### Task 1: Backend Quality Analysis

**Files:**
- Create: `backend/src/main/java/com/example/ainovel/script/service/QualityReport.java`
- Create: `backend/src/main/java/com/example/ainovel/script/service/QualityIssue.java`
- Create: `backend/src/main/java/com/example/ainovel/script/service/ScriptQualityService.java`
- Modify: `backend/src/main/java/com/example/ainovel/script/service/ConversionResponse.java`
- Modify: `backend/src/main/java/com/example/ainovel/script/service/ScriptConversionService.java`
- Modify: `backend/src/main/java/com/example/ainovel/script/service/ScriptHistoryItem.java`
- Modify: `backend/src/main/java/com/example/ainovel/script/service/ScriptHistoryService.java`
- Test: `backend/src/test/java/com/example/ainovel/script/service/ScriptQualityServiceTest.java`

- [x] Write tests for score deduction, missing scene metadata, validation errors, and speaker/character mismatch.
- [x] Implement `ScriptQualityService.analyze(ScriptDocument, ValidationResult)`.
- [x] Add `quality` to conversion responses and saved history items.
- [x] Run `mvn -s maven-settings.xml test`.

### Task 2: Scene-Level AI Editing

**Files:**
- Create: `backend/src/main/java/com/example/ainovel/script/controller/SceneEditRequest.java`
- Create: `backend/src/main/java/com/example/ainovel/script/service/SceneEditResponse.java`
- Modify: `backend/src/main/java/com/example/ainovel/script/controller/ScriptController.java`
- Modify: `backend/src/main/java/com/example/ainovel/script/service/AiEditingService.java`
- Test: `backend/src/test/java/com/example/ainovel/script/service/AiEditingServiceTest.java`

- [x] Add tests proving scene-level fallback returns rewritten beats for `polish-scene-dialogue`, `expand-scene-action`, and `enhance-scene-conflict`.
- [x] Add `/api/scripts/edit-scene` endpoint.
- [x] Keep AI mode prompt strict: return only a JSON array of beats; fallback if parsing fails.
- [x] Run targeted backend tests.

### Task 3: Frontend Types, Status, Quality, History

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/styles.css`

- [x] Add `QualityReport`, `QualityIssue`, `editScene` API types.
- [x] Add staged conversion messages while synchronous conversion runs.
- [x] Render quality score, issue tags, and suggestions in the right inspector.
- [x] Add history keyword search and generation-mode filter.
- [x] Run `pnpm build`.

### Task 4: Exports And Scene Editing UI

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/styles.css`

- [x] Add export actions for printable PDF and Word-compatible document.
- [x] Add per-scene `AI 场景编辑` dropdown.
- [x] Apply returned scene beats through existing `renderScript` flow.
- [x] Run `pnpm build`.

### Task 5: Final Verification

- [x] Run `mvn -s maven-settings.xml test` in `backend`.
- [x] Run `pnpm build` in `frontend`.
- [x] Run `git diff --check`.
- [x] Scan tracked and untracked source files for `sk-` style API keys.
