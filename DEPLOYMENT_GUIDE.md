# Deployment Guide — Lightweight NL→SQL Engine

This guide provides simple, step-by-step instructions for deploying the local-first, AI-powered natural language SQL database interface for a client.

---

## 1. Deployment Topology

```
                  ┌─────────────────────────┐
                  │   Client Browser (UI)   │
                  └────────────┬────────────┘
                               │
                       HTTP (Port 3000)
                               │
                               ▼
                  ┌─────────────────────────┐
                  │    Spring Boot Backend  │
                  └────────────┬────────────┘
                               │
                 ┌─────────────┴─────────────┐
                 │                           │
                 ▼                           ▼
      ┌─────────────────────┐     ┌─────────────────────┐
      │    Ollama API       │     │   SQLite Database   │
      │  (Local Port 11434) │     │    (Local File)     │
      └─────────────────────┘     └─────────────────────┘
```

The application runs entirely on local, cost-effective client hardware (Windows, macOS, or Linux). There are no cloud hosting fees, zero external APIs, and no internet connection required after initial setup.

---

## 2. Infrastructure Prerequisites

Before beginning the deployment, ensure the following are installed on the target machine:
1. **Java Development Kit (JDK 21):** Used to compile and run the backend.
2. **Node.js (v18 or higher):** Used to build and serve the React user interface.
3. **Maven:** Needed to compile the backend dependencies.
4. **Ollama:** Installed locally on the server/machine.

---

## 3. Step-by-Step Installation

### Step 1: Install & Set Up Ollama
1. Download Ollama from the official website and install it.
2. Run the Ollama application.
3. Open a terminal and download the recommended code model:
   ```bash
   ollama pull qwen2.5-coder:7b
   ```
   *(For lower-resource machines, `qwen2.5-coder:1.5b` can be used instead).*

### Step 2: Configure Client Database
1. Locate the database configuration folder: `service-template/config/`.
2. Replace `service-template/config/schema.sql` with the client's database tables.
3. Update `service-template/config/config.json` with the client details:
   ```json
   {
     "clientName": "Acme Corp",
     "databaseType": "sqlite",
     "ollamaModel": "qwen2.5-coder:7b",
     "ollamaUrl": "http://localhost:11434",
     "maxRows": 100
   }
   ```

### Step 3: Deploy the Backend Server
1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Build the server package:
   ```bash
   mvn clean package -DskipTests
   ```
3. Run the compiled JAR file:
   ```bash
   java -jar target/farm-manager-backend-1.0.0.jar
   ```
   *The backend will boot up, dynamically load the schema context, initialize the local SQLite database file, and bind to `http://localhost:8080`.*

### Step 4: Deploy the React Frontend UI
1. Navigate to the `frontend` directory:
   ```bash
   cd ../frontend
   ```
2. Install client dependencies:
   ```bash
   npm install
   ```
3. Run the UI in development mode:
   ```bash
   npm start
   ```
   *(Or for production, build the static assets using `npm run build` and serve them via an Nginx/Apache instance or Java static resource mapping).*
4. Access the user interface at `http://localhost:3000` in any web browser.

---

## 4. Basic Troubleshooting

### Issue: Ollama connection timeout (HTTP 504)
- **Cause:** The local Ollama instance is either offline, not running, or warming up the model for the first time.
- **Fix:** Start the Ollama desktop app, run `ollama run qwen2.5-coder:7b` in a terminal to verify it is responsive, and retry the request.

### Issue: SQL Safety Block Triggered
- **Cause:** The AI attempted to write a modifying query or used forbidden commands.
- **Fix:** Verify that the question does not imply data changes (e.g. "delete" or "update"). If the query was a false positive, inspect the keyword whitelist/blacklist in the backend.

### Issue: Port 8080 or 3000 Already in Use
- **Cause:** Another local service is binding to the default ports.
- **Fix:** Reconfigure the backend port in `backend/src/main/resources/application.properties` (`server.port=XXXX`) and restart.
