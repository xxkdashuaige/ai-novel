# AI 小说转剧本工具 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot + Spring AI backend and Vue 3 + Element Plus frontend that converts 3+ chapter novel text into structured YAML scripts.

**Architecture:** The backend exposes REST APIs for conversion, validation, and Schema documentation. Rule-based generation is the default deterministic path, while Spring AI is an optional enhancement behind configuration. The frontend is a single-page workspace for input, conversion, preview, and download.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring AI 1.1.x, Jackson YAML, Vue 3, Vite, TypeScript, Element Plus.

---

### Task 1: Backend DTOs and Rule-Based Generator

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/ainovel/script/model/*.java`
- Create: `backend/src/main/java/com/example/ainovel/script/service/RuleBasedScriptGenerator.java`
- Test: `backend/src/test/java/com/example/ainovel/script/service/RuleBasedScriptGeneratorTest.java`

- [x] Write tests that prove at least three chapters are required and dialogue/action beats are extracted.
- [x] Implement DTOs and the rule-based parser.
- [x] Run Maven tests when dependencies are available.

### Task 2: YAML and Schema Validation

**Files:**
- Create: `backend/src/main/java/com/example/ainovel/script/service/YamlScriptService.java`
- Create: `backend/src/main/java/com/example/ainovel/script/service/ScriptValidationService.java`
- Test: `backend/src/test/java/com/example/ainovel/script/service/ScriptValidationServiceTest.java`

- [x] Write tests for valid and invalid scripts.
- [x] Implement YAML serialization and validation.

### Task 3: REST API and Spring AI Adapter

**Files:**
- Create: `backend/src/main/java/com/example/ainovel/AiNovelApplication.java`
- Create: `backend/src/main/java/com/example/ainovel/script/controller/ScriptController.java`
- Create: `backend/src/main/java/com/example/ainovel/script/service/SpringAiScriptGenerator.java`
- Create: `backend/src/main/resources/application.yml`

- [x] Add conversion, validation, and Schema endpoints.
- [x] Make Spring AI optional with rule-based fallback.

### Task 4: Vue 3 Frontend

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/api.ts`
- Create: `frontend/src/styles.css`

- [x] Build an Element Plus workspace for text input, YAML result, JSON preview, validation, and download.

### Task 5: Documentation

**Files:**
- Create: `README.md`
- Create: `docs/schema.md`

- [x] Document startup commands, API usage, Spring AI configuration, and YAML Schema design.
