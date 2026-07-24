# Reusability Report — Farm Manager AI

This report assesses the feasibility, architecture, and effort required to convert Farm Manager AI into a reusable "Natural Language to SQL" template for other industries.

---

## 1. What Parts are Generic?
The core infrastructure of this application is highly generic and domain-agnostic:
- **`OllamaQueryService` (AI Engine):** The HTTP client, model parameterization (`qwen2.5-coder:7b`), dynamic schema loader, markdown code block cleaner, and prompt structure are 100% generic.
- **SQL Safety Interceptor:** The safety validator (allowing only `SELECT` / `WITH` and blacklisting destructive keywords like `DROP`, `DELETE`) applies to any database.
- **Telemetry System (`TelemetryService`):** The atomic counters for success/fallback/failure metrics and the `/api/telemetry` endpoint are completely generic.
- **QueryResponse DTO:** The unified response envelope (`question`, `sql`, `answer`, `columns`, `rows`, `mode`) fits any query application.
- **React Frontend:** The text input, chip display, loading states, error boundaries, SQL/Answer display cards, and HTML table renderer are completely generic.

---

## 2. What Parts are Farm-Specific?
Only a few components are coupled to the agricultural domain:
- **`schema.sql`:** The database tables (`animals`, `fields`, `crops`, `transactions`) represent a farm's data.
- **`DbSeedingService.java`:** The dummy data used to populate the SQLite database.
- **`QueryTemplateService.java`:** The regex matches and fallback SQL templates are tailored to the 10 farm questions.
- **Dynamic Formatting Logic:** The specific formatting rule in `OllamaQueryService.formatResult` (appending `eggs` or `gallons` based on column names) is farm-specific.

---

## 3. What is Required to Convert This into a Reusable Template?
To package this codebase as a reusable software asset, the following refactoring steps are needed:

1. **Parameterize the ClassPath Schema:**
   - Move the schema path to `application.properties`:
     ```properties
     app.database.schema-path=classpath:database/schema.sql
     ```
2. **Abstract the Fallback Service:**
   - Define a generic `FallbackQueryService` interface that can be implemented for different client projects.
3. **Make Formatting Rules Configurable:**
   - Replace the hardcoded units ("eggs", "gallons") in the result formatter with a map-based configuration file (e.g. `formatting.json`), matching column name suffixes to unit formats.
4. **Decouple the Seeding Logic:**
   - Load seed data from a CSV or JSON file rather than hardcoding it in Java classes.

---

## 4. Estimated Effort to Adapt to a New Client Database
Adapting this architecture to a completely new client database (e.g. an **Inventory System** or **Clinic Management System**) is extremely fast:

| Task | Description | Estimated Time |
| :--- | :--- | :--- |
| **1. Update Schema** | Replace `schema.sql` with the new tables and run the app to generate the new SQLite database. | 30 minutes |
| **2. Update Seed Data** | Modify the data seed script to insert rows relevant to the new tables. | 45 minutes |
| **3. Configure Fallbacks** | Replace the 10 regex templates in `QueryTemplateService` to match standard questions in the new domain. | 60 minutes |
| **4. Align Frontend Chips** | Change the 4 sample question chips in `App.js` to match the new domain's common queries. | 15 minutes |
| **Total Effort** | **~2.5 hours** |
