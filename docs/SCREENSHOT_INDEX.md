# Screenshot Index — Farm Manager AI

This document indexes all valid screenshots used to demonstrate the features of the Farm Manager AI project.

---

### 1. `screenshots/ai-query.png`
- **Purpose:** Demonstrates the primary user interface processing a natural language query in real-time.
- **Feature Demonstrated:** Local AI natural language query processing. Shows the query input bar, sample query chips, the generated SQL display, and the computed plain-text answer card, marked with the green **`[AI Mode]`** badge.

### 2. `screenshots/list-animals.png`
- **Purpose:** Demonstrates the dynamic result rendering of multi-record query results.
- **Feature Demonstrated:** Zebra-striped HTML table rendering. Shows the user requesting a multi-row query, and the React frontend formatting the resulting dataset in a responsive, border-aligned table instead of raw text.

### 3. `screenshots/template-fallback.png`
- **Purpose:** Demonstrates how the application handles failures or offline states of the local AI service.
- **Feature Demonstrated:** Deterministic template fallback mode. Shows the system running when Ollama is offline or times out; the backend catches the connection error, triggers a regex template match, and returns the result with a blue **`[Template Mode]`** badge.

### 4. `screenshots/telemetry.png`
- **Purpose:** Demonstrates the real-time telemetry monitoring of backend queries.
- **Feature Demonstrated:** REST telemetry endpoint `/api/telemetry` outputting JSON metric counters (`aiSuccessCount`, `templateFallbackCount`, `sqlFailureCount`).