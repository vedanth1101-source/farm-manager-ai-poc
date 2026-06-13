# Farm Manager AI

A local-first, AI-powered natural language interface for structured farm databases. 

---

## 1. What This Is
**Farm Manager AI** is a local-first application that allows farm operators to ask natural language questions about their farm operations (livestock, crops, equipment, finances) and translates those questions dynamically into executable SQLite queries. 

The application runs entirely offline by leveraging a local **Ollama** service running the **`qwen2.5-coder:7b`** model, ensuring complete data privacy and zero dependence on cloud LLM APIs.

---

## 2. Features
- **Natural Language Farm Queries:** Ask questions like "How many eggs did I collect last week?" or "Which rabbits gained the most weight?" in plain English.
- **AI-Generated SQL:** Dynamically generates SQLite queries based on cached DB schemas.
- **Strict SQL Safety Validation:** Blocks malicious operations (`DROP`, `DELETE`, `UPDATE`, `INSERT`, etc.) and enforces read-only `SELECT` / `WITH` access.
- **Template Fallback Mode:** Seamlessly falls back to a regex-based matching system if the local Ollama instance is offline or fails to generate valid SQL.
- **Structured Data Rendering:** Formats single metrics as scalar cards and multi-row datasets as clean, interactive HTML tables.
- **In-Memory Telemetry:** Monitors AI successes, template fallbacks, and SQL syntax errors at `/api/telemetry`.
- **Dynamic Schema Loading:** Caches `schema.sql` at startup to allow changes to propagate without recompiling code.

---

## 3. Architecture
```
  [ React Frontend ]
         │
         ▼ (HTTP POST /api/query)
  [ Spring Boot Backend ]
   ┌─────┴────────────────────────┐
   ▼                              ▼
[ Ollama (qwen2.5-coder:7b) ]   [ SQLite Database (farm_manager.db) ]
   (NL -> SQL query)               (Schema & Seeding Data)
```

---

## 4. Screenshots

### 4.1 Natural Language Query Interface
*(Placeholder: Insert screenshot showing App UI, chips, input bar, and buttons)*

### 4.2 AI Mode Table Rendering
*(Placeholder: Insert screenshot showing a multi-row query result displayed in a clean, borders-aligned HTML table)*

### 4.3 Template Fallback Badge
*(Placeholder: Insert screenshot showing the Blue [Template Mode] badge and SQL query when Ollama is offline)*

---

## 5. Running Locally

### 5.1 Prerequisites
- **Java 21** (JDK installed and added to PATH)
- **Node.js** (v18+ recommended)
- **Maven** (for building the Java backend)
- **Ollama** (installed locally)

### 5.2 Step 1: Start Ollama & Load the Model
1. Start the Ollama application.
2. In your terminal, pull the required model:
   ```bash
   ollama pull qwen2.5-coder:7b
   ```

### 5.3 Step 2: Run the Backend
1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Build and run the Spring Boot application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   *The server will initialize the SQLite database using `schema.sql`, seed mock data, and start on `http://localhost:8080`.*

### 5.4 Step 3: Run the Frontend
1. Navigate to the `frontend` directory:
   ```bash
   cd ../frontend
   ```
2. Install dependencies and start the React app:
   ```bash
   npm install
   npm start
   ```
   *The client application will open automatically on `http://localhost:3000`.*

---

## 6. Future Improvements
- **Extended Read-Only Dialects:** Expand SQL validation rules for advanced CTE (Common Table Expressions) and complex window functions.
- **Dynamic Telemetry Reset:** Expose a POST `/api/telemetry/reset` endpoint to clear session stats.
- **Multiple Local Models:** Allow the user to toggle between `qwen2.5-coder:7b` and lighter models like `qwen2.5-coder:1.5b` or `gemma4:latest` for resource-constrained hardware.
