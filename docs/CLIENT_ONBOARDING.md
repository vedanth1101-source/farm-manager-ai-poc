# Client Onboarding Workbook — Farm Manager AI Engine

This workbook provides a step-by-step checklist for adapting the offline-first NL→SQL engine to a new client's database. Follow these steps to onboard a new database schema and go live.

---

## Onboarding Checklist

- [ ] Step 1: Client Information Intake
- [ ] Step 2: Database Schema Extraction
- [ ] Step 3: Configure Base System Files
- [ ] Step 4: Map Common Fallback Queries (Deterministic Mode)
- [ ] Step 5: Update Frontend UI Chips
- [ ] Step 6: Verify and Run Integration Tests

---

## Detailed Onboarding Steps

### Step 1: Client Information Intake
Gather the core configuration metadata for the client. Fill in the placeholders below:
- **Client Name:** ________________________
- **Database Engine:** SQLite (Standard) / Other: ____________
- **Ollama Host URL:** `http://localhost:11434` (Default) / Other: ________________________
- **Target LLM Model:** `qwen2.5-coder:7b` (Recommended) / Other: ________________________

---

### Step 2: Database Schema Extraction
Export the client's database structure as clean DDL statements:
1. Export the table creation script as SQL.
2. Ensure there are no client-sensitive comments or hardcoded data in the schema file.
3. Save the resulting schema in:
   `service-template/config/schema.sql`

---

### Step 3: Configure Base System Files
Initialize the configuration properties for the engine.
1. Open `service-template/config/config.json` and fill in the client details:
   ```json
   {
     "clientName": "[CLIENT_NAME]",
     "databaseType": "sqlite",
     "ollamaModel": "qwen2.5-coder:7b",
     "ollamaUrl": "http://localhost:11434",
     "maxRows": 100
   }
   ```
2. Inspect `service-template/config/system-prompt.txt`. Ensure the rules match the client's naming styles and conventions.

---

### Step 4: Map Common Fallback Queries (Deterministic Mode)
The system must be able to resolve queries even when Ollama is offline.
1. Identify the **10 most common natural language questions** the client wants to ask.
2. Open `QueryTemplateService.java` in the backend.
3. Replace the farm-specific regex pattern matchers with the new client-specific questions.
4. Provide the exact, hand-written SQL matching each pattern.
   *Example:*
   ```java
   // Match: "List all active users"
   if (question.matches("(?i)list all.*users")) {
       return new QueryResult(question, "SELECT * FROM users WHERE status='Active' LIMIT 100", ...);
   }
   ```

---

### Step 5: Update Frontend UI Chips
Give the client quick-action buttons for common queries.
1. Open `frontend/src/App.js`.
2. Locate the chip list definition:
   ```javascript
   const sampleQuestions = [
       "How many active users are registered?",
       "Show recent transactions in the last 30 days",
       ...
   ];
   ```
3. Replace the farm questions with the new client questions from Step 4.

---

### Step 6: Verify and Run Integration Tests
Run checks to verify the system works:
1. Start the local Ollama instance and download the model.
2. Run the Spring Boot server (`mvn spring-boot:run`). It should read the new `schema.sql` at startup.
3. Start the React frontend (`npm start`).
4. **Test AI Mode:** Click a question chip. Verify the **`[AI Mode]`** badge (green) appears, along with the correct SQL and query table.
5. **Test Fallback Mode:** Stop Ollama. Click the chip again. Verify the **`[Template Mode]`** badge (blue) appears, displaying the hand-written SQL.
6. **Test Safety Layer:** Enter a query like `"DROP TABLE users;"`. Verify the execution is blocked, and falls back gracefully to template mode without running against the database.
