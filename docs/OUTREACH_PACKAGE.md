# OUTREACH PACKAGE
**Generated:** 2026-06-13  
**Target:** Matt (Manger Founder)

---

## 1. Outreach Message to Matt

**Subject:** Built a working Farm Manager AI demo for Manger — wanted to share

---

Hi Matt,

I've been following Manger's mission to help small farm operators run more efficient, data-driven operations — and I wanted to show you something concrete rather than just talk about ideas.

I built a working proof-of-concept: **Farm Manager AI**, a natural language query system for farm management data.

**What it does:**
A farm operator types a plain-English question — *"How many eggs did I collect last week?"* — and the system returns the generated SQL and a human-readable answer like *"81 eggs"*.

**The stack:**
- Spring Boot 3.2 backend with 10 pre-built NL→SQL query templates
- React 18 frontend
- SQLite database pre-loaded with realistic farm data (chickens, cows, rabbits, egg collection, milk production, health records, expenses)

**Why it matters for Manger:**
Farm operators don't want dashboards — they want answers. This pattern (natural language → SQL → plain English) can surface real business insights without requiring operators to learn new UIs.

**See it yourself:**
🔗 https://github.com/vedanth1101-source/farm-manager-ai-poc

Runs in 2 commands:
```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm start
```

I'd love to chat about how this fits into what Manger is building — happy to demo it live.

Best,  
Vedanth

---

## 2. GitHub Repository Summary

**Repository:** https://github.com/vedanth1101-source/farm-manager-ai-poc

**One-line description:**  
> Spring Boot + React proof-of-concept that translates farm management questions into SQL and returns plain-English answers.

**Key Stats:**
- 10 natural language query templates covering core farm KPIs
- 5 database tables: animals, egg_collection, milk_production, health_records, expenses
- Full-stack: Java 21 backend + React 18 frontend
- Runs locally in 2 commands, no cloud dependencies

---

## 3. Demo Summary

**Demo flow (5 minutes):**

1. Start backend: `cd backend && ./mvnw spring-boot:run`
2. Start frontend: `cd frontend && npm install && npm start`
3. Open http://localhost:3000
4. Type: **"How many eggs did I collect last week?"** → Click Ask
5. Show: Generated SQL + "81 eggs" answer
6. Try: **"Which animals need attention?"** → Show health alert output
7. Try: **"What was my feed expense last month?"** → Show dollar amount
8. Click `GET /api/questions` in browser to show all 10 supported queries

**Talking points:**
- "This is pattern 1 of 10 — each one is a real farm KPI"
- "The SQL is executed live against a SQLite database with realistic data"
- "This same pattern scales to an LLM backend when you're ready"

---

## 4. Value Proposition Summary

**Problem:** Farm operators can't quickly query their own data — they need a developer to write reports or learn complex dashboard tools.

**Solution:** Natural language interface that maps common farm questions to pre-validated SQL queries, returning instant plain-English answers.

**Why this approach:**
- ✅ **Reliable**: Template matching is 100% deterministic — no hallucinations
- ✅ **Fast**: SQLite query returns in milliseconds
- ✅ **Extensible**: Adding a new question type = adding one template + one SQL query
- ✅ **LLM-ready**: The architecture can be upgraded to use Gemini/GPT for open-ended questions when needed

**Immediate use cases for Manger:**
1. Daily KPI digest: "How many eggs today / this week / this month?"
2. Animal health alerts: "Which animals need attention?"
3. Financial tracking: "What were my total expenses last month?"
4. Production comparison: "Compare egg production month-over-month"
