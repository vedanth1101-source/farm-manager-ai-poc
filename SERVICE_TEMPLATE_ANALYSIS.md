# Service Template Analysis — Farm Manager AI

This analysis outlines the blueprint for converting the Farm Manager AI project into a lightweight, reusable "Natural Language to SQL" template for other industries.

---

## 1. Reusable / Generic Components

The majority of the codebase is completely domain-agnostic and can be reused directly:

- **`OllamaQueryService` (Backend Engine):**
  - **Dynamic Schema Context Loading:** Reads `schema.sql` at startup from classpath and caches it in memory.
  - **Ollama HTTP Integration:** Connects to `http://localhost:11434/api/generate` with parameterization.
  - **SQL Sanitizer:** Strips markdown fenced blocks and clean trailing semicolons/comments.
- **SQL Safety Validation Interceptor:**
  - Enforces read-only commands (starting with `SELECT` or `WITH`).
  - Blacklists data-modifying keywords (`DROP`, `DELETE`, `UPDATE`, `INSERT`, etc.) using regex word boundary patterns.
- **Unified Query DTOs (`QueryRequest`, `QueryResponse`):**
  - Standardized JSON schemas for requests and responses, passing metadata (like the active mode badge `"ai"` or `"template"`).
- **In-Memory Telemetry Service (`TelemetryService`):**
  - Thread-safe counters (`aiSuccessCount`, `templateFallbackCount`, `sqlFailureCount`) and a REST GET `/api/telemetry` endpoint.
- **React Frontend UI Container (`App.js` & `App.css`):**
  - **Dynamic HTML Table Generator:** Dynamically reads columns and rows from backend results and formats them as a responsive HTML table.
  - **Scalar Display Card:** Displays single cell metric values with modern aesthetic tokens.
  - **Badge Rendering:** Displays active badges representing model sources.

---

## 2. Farm-Specific / Coupled Components

Only a small subset of files are coupled to the agriculture domain:

- **Database Configuration:**
  - `schema.sql` (located under `backend/src/main/resources/database/`): Contains the specific relational tables for animals, crops, fields, and transactions.
  - `seed.sql`: Seed data for the SQLite file.
- **Fallback Template Logic (`QueryTemplateService`):**
  - The 10 regex patterns and handwritten SQL queries are strictly tailored to agricultural questions.
- **Result Unit Formatting Helper:**
  - Hardcoded formatting logic in `OllamaQueryService.formatResult` (appending "eggs" or "gallons" based on column names) is specific to farm measurements.

---

## 3. Minimum Adaptation Effort for New Clients

Adapting the current repository structure for a new business database (e.g. an Inventory System, School, or Clinic database) is extremely fast. 

### Effort Estimation Table

| Adaptation Step | Target Files | Description | Estimated Time |
| :--- | :--- | :--- | :--- |
| **1. Update Schema** | `service-template/config/schema.sql` | Replace default DDL with the new database structures. | 15 mins |
| **2. Update Config** | `service-template/config/config.json` | Configure client name, LLM model parameters, and Ollama URLs. | 10 mins |
| **3. Re-map Fallbacks** | `QueryTemplateService.java` | Replace the 10 regex match patterns and hand-coded SQL templates for standard fallback queries. | 60 mins |
| **4. Align Frontend UI** | `frontend/src/App.js` | Update the 4 sample question chips to match common client queries. | 15 mins |
| **5. Align Result Formatter** | `OllamaQueryService.java` | Replace farm unit formatting ("eggs", "gallons") with new units or remove custom suffix logic. | 20 mins |
| **Total Adaptation Effort** | | **~2 Hours** | |

---

## 4. Recommended Future Architecture

To transform this Proof of Concept into an enterprise-grade, highly-reusable software template, we recommend implementing the following architectural upgrades:

### 1. Externalized System Configurations
- **Current Issue:** Database paths, Ollama hosts, model configurations, and unit suffixes are hardcoded in Java classes and React states.
- **Target Architecture:** Map all configuration parameters to `application.properties` or environment variables (`.env`).
  ```properties
  # Spring configuration properties
  ollama.service.url=http://localhost:11434
  ollama.service.model=qwen2.5-coder:7b
  ollama.service.timeout-seconds=15
  database.schema.path=classpath:config/schema.sql
  database.unit.mappings=egg=eggs,milk=gallons,yield=gallons,expense=$,revenue=$
  ```

### 2. AST-Based SQL safety Validation
- **Current Issue:** The SQL validation scanner uses basic regex word boundaries. This can cause false positives on column names containing blacklisted words (e.g. a column called `last_update` triggering the `UPDATE` keyword check) or bypasses on obfuscated queries.
- **Target Architecture:** Integrate a lightweight SQL Abstract Syntax Tree (AST) parsing library (e.g., **JSQLParser** for Java). Parse the generated query. Traverse the syntax tree and verify that every node represents a safe `Select` or `With` operation, guaranteeing complete runtime safety.

### 3. Dynamic Registry for Fallback Templates
- **Current Issue:** Fallback template matchers are hardcoded as nested `if-else` blocks in a Java service.
- **Target Architecture:** Define fallback templates in an external JSON configuration file (e.g. `fallback-templates.json`). On startup, load this file into a lookup map, removing the need to modify Java code when adapting questions.
  ```json
  [
    {
      "pattern": "(?i)list all.*users",
      "sql": "SELECT * FROM users WHERE status='Active' LIMIT 100",
      "answerTemplate": "There are {count} active users registered."
    }
  ]
  ```
