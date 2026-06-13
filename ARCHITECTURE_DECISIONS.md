# Architecture Decision Records (ADR) — Farm Manager AI

This document outlines the core architectural and design decisions implemented in Farm Manager AI.

---

## 1. Decoupled Service Orchestration

### Context
A unified query service mixing template parsing and LLM API client logic causes code bloat, violates the Single Responsibility Principle, and makes testing template fallbacks difficult.

### Decision
- **QueryTemplateService** remains 100% untouched as a pure template-matching service.
- **OllamaQueryService** is introduced as a standalone service handling schema loading, prompt engineering, Ollama HTTP calls, SQL safety validation, and raw SQL execution.
- **QueryController** coordinates the execution:
  ```
  User Question 
      ↓
  QueryController
      ↓
  Try: OllamaQueryService ──[Success]──→ Return mode="ai"
      ↓ [Connection Timeout OR SQL Error]
  Catch: QueryTemplateService ──────────→ Return mode="template"
  ```

---

## 2. Strictly Enforced SQL Safety Interceptor

### Context
Large Language Models (LLMs) can occasionally hallucinate destructive queries (e.g. `DROP`, `DELETE`) or attempt schema modifications when prompted with malicious user inputs.

### Decision
Before any AI-generated SQL query is executed by `JdbcTemplate`, it must pass a strict security validator in `OllamaQueryService`:
1. **White-listed Commands:** The query must start only with `SELECT` or `WITH` (ignoring leading whitespace and case).
2. **Forbidden Keywords:** The query is scanned against a blacklist using word-boundary regex patterns (`\bKEYWORD\b`). Standalone matches of any of the following strictly reject query execution:
   - `DROP`, `DELETE`, `UPDATE`, `INSERT`, `ALTER`, `TRUNCATE`, `PRAGMA`, `ATTACH`

If validation fails, a `SecurityException` is thrown, which triggers the controller to bypass execution and fall back safely to the template system.

---

## 3. Dynamic Schema Loading & Caching

### Context
Hardcoding the SQL database schema as Java String literals makes the LLM query service brittle, prevents schema updates, and restricts reusability for other business domains.

### Decision
On application startup, `OllamaQueryService` dynamically reads the database schema file `database/schema.sql` from the classpath using Spring's `ClassPathResource`. The schema content is loaded once and cached in an in-memory string. This cache is inserted directly into the system prompt for Ollama, ensuring that any schema modifications are instantly propagated to the LLM without rebuilding the Java source code.

---

## 4. Response Encapsulation (QueryResponse DTO)

### Context
Directly returning the internal `QueryResult` of the template service makes it difficult to add metadata (such as the execution `mode`) without modifying the legacy template classes.

### Decision
We introduced a unified `QueryResponse` Data Transfer Object (DTO) in `com.farmmanager.dto`. The controller maps output fields from either `OllamaResult` (AI mode) or `QueryResult` (template mode) into this unified response, automatically appending the `mode` attribute. This keeps the backend decoupled and allows Jackson to serialize the exact structure needed by the frontend.

---

## 5. In-Memory Telemetry

### Context
We need to monitor whether the local AI engine is successfully answering queries or repeatedly falling back to templates, without adding complex external APM or monitoring infrastructure.

### Decision
We created a lightweight `TelemetryService` utilizing `AtomicInteger` counters in memory. It tracks:
- `aiSuccessCount`: Successfully generated and executed SQL queries.
- `templateFallbackCount`: Queries that fell back to regex templates due to Ollama timeouts, connection errors, or SQL validation failures.
- `sqlFailureCount`: AI queries that successfully generated SQL but threw database/SQL syntax errors during execution.

These metrics are exposed at a new REST endpoint GET `/api/telemetry` for health checks.
