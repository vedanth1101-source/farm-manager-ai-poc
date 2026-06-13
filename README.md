# 🌾 Farm Manager AI

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://reactjs.org/)
[![SQLite](https://img.shields.io/badge/SQLite-3.45-lightblue?logo=sqlite)](https://www.sqlite.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **A proof-of-concept AI farm management assistant** that translates plain-English questions into SQL queries and returns human-readable answers — built for Manger, a livestock and farm productivity platform.

---

## 🎯 What It Does

Farm Manager AI lets farm operators ask natural language questions about their livestock, eggs, milk, health records, and expenses. The Spring Boot backend matches the question against 10 pre-built templates, executes the appropriate SQLite query, and returns both the generated SQL and a plain-English answer.

```
"How many eggs did I collect last week?"
→  SQL: SELECT SUM(quantity) AS total_eggs FROM egg_collection WHERE ...
→  Answer: 81 eggs
```

---

## 🏗️ Architecture

```
farm-manager-ai/
├── backend/                          # Java 21 / Spring Boot 3.2.5
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/farmmanager/
│       │   ├── FarmManagerApplication.java   # Entry point
│       │   ├── controller/
│       │   │   └── QueryController.java      # REST endpoints
│       │   ├── dto/
│       │   │   └── QueryRequest.java         # Request model
│       │   └── service/
│       │       ├── QueryTemplateService.java # NL→SQL matching engine
│       │       ├── DatabaseInitializer.java  # SQLite setup
│       │       └── DbSeedingService.java     # Seed data loader
│       └── resources/
│           ├── application.properties
│           └── database/
│               ├── schema.sql                # Table definitions
│               └── seed.sql                 # Sample farm data
└── frontend/                         # React 18 / Create React App
    ├── package.json
    └── src/
        ├── App.js                    # Main query UI component
        ├── services/api.js           # Axios API client
        └── index.js
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/questions` | Returns the 10 supported natural-language questions |
| `POST` | `/api/query` | Accepts `{ "question": "..." }`, returns SQL + answer |

**POST `/api/query` — Sample Response:**
```json
{
  "question": "How many eggs did I collect last week?",
  "sql": "SELECT SUM(quantity) AS total_eggs FROM egg_collection WHERE collection_date >= date('now', '-7 days')",
  "answer": "81 eggs",
  "columns": ["total_eggs"],
  "rows": [{ "total_eggs": 81 }]
}
```

---

## 💬 Supported Questions

The engine matches **10 pre-built templates** using regex pattern matching:

1. How many eggs did I collect **last week**?
2. How many eggs did I collect **this month**?
3. Which **chickens** produced the most eggs?
4. What was my **feed expense** last month?
5. What were my **total expenses** last month?
6. Which **rabbits** gained the most weight?
7. Show **health incidents** in the last 30 days.
8. Which animals **need attention**?
9. Show **milk production** this month.
10. Compare **egg production month-over-month**.

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| JDK | 21+ |
| Maven | 3.8+ (or use included `mvnw`) |
| Node.js | 18+ |
| npm | 9+ |

---

## 🚀 Setup & Running

### 1. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
# or: mvn spring-boot:run
# Backend runs on http://localhost:8080
```

The backend auto-creates and seeds a SQLite database (`farm_manager.db`) on first run.

### 2. Start the Frontend

```bash
cd frontend
npm install        # first-time only
npm start          # opens http://localhost:3000
```

### 3. Use the UI

Open `http://localhost:3000`, type a question (or click a suggestion), and click **Ask**.

---

## 🏗️ Build for Production

```bash
# Backend JAR
cd backend
mvn package
java -jar target/farm-manager-backend-1.0.0.jar

# Frontend static bundle
cd frontend
npm run build
# Serve the ./build directory with any static file server
```

---

## 🗄️ Database Schema

The SQLite database contains 5 tables populated with realistic sample data:

| Table | Description |
|-------|-------------|
| `animals` | Chickens, cows, rabbits with weight tracking |
| `egg_collection` | Daily egg counts per animal |
| `milk_production` | Daily milk yield per cow (gallons) |
| `health_records` | Diagnoses, treatments, costs per animal |
| `expenses` | Farm expense ledger by category |

See [`backend/src/main/resources/database/schema.sql`](backend/src/main/resources/database/schema.sql) and [`seed.sql`](backend/src/main/resources/database/seed.sql) for full definitions.

---

## 📄 License

MIT — see [LICENSE](LICENSE) for details.
