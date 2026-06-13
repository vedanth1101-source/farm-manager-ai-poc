# Case Study: Local AI-Powered Natural Language Interface

## 1. Problem
Small-to-mid farm operators (like livestock managers, crop growers, and smallholders) collect critical operational data on a daily basis. However, extracting actionable insights (such as egg collection trends, weight gains, and milk yields) traditionally requires writing SQL queries, navigating complex database administration tools, or relying on expensive, cloud-dependent analytics software. 

Because farms are often located in remote areas with poor or intermittent internet connectivity, any cloud-based solution (such as OpenAI, Anthropic, or cloud database syncs) is highly unreliable. Farm operators need a system that:
1. Translates natural language questions directly into query results.
2. Respects the **offline-first** constraint of agricultural software.
3. Operates securely on local, cost-effective hardware.

---

## 2. Origin Story
The idea for **Farm Manager AI** originated from a real-world case study on Hacker News. A solo founder, **krogenx** (Matt), posted a Show HN introducing **Manger**, a niche livestock management mobile app.

In the comments of his launch post, Matt highlighted his upcoming product roadmap:
> *"local AI integration where you can simply ask 'how many eggs did I collect last week?'"*

Since his mobile app ran locally on SQLite and prioritized offline data storage, Matt was looking for a way to translate natural language queries to SQL locally without using internet APIs. This project was built to solve Matt's exact product challenge, proving that a lightweight, local LLM can successfully act as a deterministic database copilot.

---

## 3. Architecture
The application uses a clean, decoupled local architecture:
- **Frontend (React):** A simple, responsive Web UI that handles user input, issues query requests, and dynamically renders HTML tables for multi-row data or clean scalar cards for single values.
- **Backend (Spring Boot 3.2.5 & Java 21):** Coordinates request routing, manages the database connection, performs SQL safety validation, and tracks system telemetry.
- **Database (SQLite):** A local, file-based relational database (`farm_manager.db`) containing seeded operational tables (animals, fields, transactions, egg_collections).
- **Local AI Engine (Ollama):** Runs the open-source **`qwen2.5-coder:7b`** model locally on port 11434, performing fast natural language to SQLite SQL query translation.

---

## 4. Challenges

### Challenge 1: SQL Safety Validation
Since LLMs can hallucinate or be tricked via prompt injection (e.g., asking "Delete the animals table"), we could not directly execute AI-generated SQL. 
- **Solution:** We built an interceptor that validates queries before execution. It enforces that queries must start with `SELECT` or `WITH`, and scans the text for forbidden keywords (`DROP`, `DELETE`, `UPDATE`, `INSERT`, etc.) using word-boundary regex patterns.

### Challenge 2: Fallback Design
If the local Ollama instance is offline or loading, the system must not crash.
- **Solution:** We designed an orchestration layer in `QueryController`. The controller wraps query execution. If the HTTP call to Ollama fails or throws a timeout, it catches the exception, increments a fallback counter, and falls back to a regex-based `QueryTemplateService` to return a reliable template answer.

### Challenge 3: Schema Prompting
The LLM needs to know the database structure to generate accurate queries, but hardcoding schema strings in Java is fragile.
- **Solution:** We designed a dynamic schema caching mechanism. On startup, the service dynamically reads the project's raw `schema.sql` file and caches it. The schema is inserted directly into the prompt on every LLM call, ensuring that schema changes are automatically picked up.

---

## 5. Results
- **AI Mode Working:** The local `qwen2.5-coder:7b` model successfully generates syntactically correct SQLite statements for all 10 standard farm questions (e.g. `SELECT SUM(quantity) FROM egg_collection WHERE...`).
- **Safety Layer Validated:** Tested malicious prompts (e.g. `DROP TABLE fields;`). All were intercepted, blocked, and redirected to fallback template mode, keeping the database intact.
- **Telemetry Active:** Exposes dynamic metrics in real time (e.g., `aiSuccessCount` and `templateFallbackCount`), allowing verification that the AI engine is operating as expected.

---

## 6. Lessons Learned & Reusable Patterns
This project demonstrates a highly reusable blueprint for local AI-to-database interfaces. By replacing the farm database schema and updating the template matchers, the exact same architecture can be adapted to:
- **Inventory Systems:** Ask "Show me products with stock under 10" or "What was my highest selling item this week?".
- **Warehouse Management:** Ask "List all orders ready for shipping" or "Which shelf has empty space?".
- **School Administration:** Ask "What is the average grade in Math?" or "Show students with attendance below 85%".
- **Clinic Systems:** Ask "List patients scheduled for today" or "How many prescriptions were filled last month?".
