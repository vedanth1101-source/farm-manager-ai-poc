# Opportunity Acquisition Architecture

**Version:** 1.0
**Author:** Vedanth
**Date:** 2026-06-13

> *"The bottleneck is no longer coding. The bottleneck is finding high-quality opportunities."*

This document defines the complete system architecture for discovering, qualifying, ranking, and strategizing outreach for high-value technical opportunities.

---

## Table of Contents

1. [System Design](#1-system-design)
2. [Source Strategy](#2-source-strategy)
3. [Agent Architecture](#3-agent-architecture)
4. [Opportunity Scoring Model](#4-opportunity-scoring-model)
5. [Opportunity Portfolio Management](#5-opportunity-portfolio-management)
6. [Target Data Model](#6-target-data-model)
7. [Database Design](#7-database-design)
8. [Auditability](#8-auditability)
9. [Systemic Risks](#9-systemic-risks)
10. [MVP Build Plan](#10-mvp-build-plan)
11. [Success Metrics](#11-success-metrics)

---

# 1. SYSTEM DESIGN

The system operates as a six-stage pipeline. Each stage filters, enriches, and scores data flowing from raw internet signals to actionable outreach strategies.

## Pipeline Overview

```
┌───────────────────────────────────────────────────────────────────────┐
│                    OPPORTUNITY ACQUISITION ENGINE                     │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐            │
│  │   STAGE 1    │    │   STAGE 2    │    │   STAGE 3    │            │
│  │   Signal     │───▶│   Noise      │───▶│   Pain       │            │
│  │   Collection │    │   Filtering  │    │   Detection  │            │
│  └──────────────┘    └──────────────┘    └──────────────┘            │
│         │                   │                   │                     │
│    ~500 signals/day    ~80 pass filter     ~25 have real pain        │
│                                                                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐            │
│  │   STAGE 4    │    │   STAGE 5    │    │   STAGE 6    │            │
│  │  Opportunity  │───▶│  Artifact    │───▶│  Outreach    │            │
│  │  Qualification│    │  Recommend.  │    │  Strategy    │            │
│  └──────────────┘    └──────────────┘    └──────────────┘            │
│         │                   │                   │                     │
│    ~8 qualified         ~5 buildable       ~3 actionable/week        │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

## Stage Definitions

### Stage 1: Signal Collection
**Input:** Raw feeds from Tier 1/2/3 sources.
**Process:** Scheduled scrapers and API pollers pull posts, comments, issues, and discussions matching keyword heuristics.
**Output:** Raw signal records stored in `signals` table.
**Volume:** ~500 raw signals per day across all sources.

### Stage 2: Noise Filtering
**Input:** Raw signals from Stage 1.
**Process:** The Intent Gatekeeper agent rejects hobby projects, student assignments, dead repositories, non-commercial discussions, and duplicate signals. Uses rule-based filters first, then local LLM classification for ambiguous cases.
**Output:** Filtered signals promoted to candidate opportunities.
**Volume:** ~80 signals pass per day (~16% pass rate).

### Stage 3: Pain Detection
**Input:** Filtered candidate signals.
**Process:** The Pain Analyst agent extracts explicit pain statements and infers implicit pain from operational symptoms. Each signal is tagged with a pain classification (explicit, implicit, or none) and a pain summary.
**Output:** Pain-annotated opportunity candidates.
**Volume:** ~25 signals exhibit real, addressable pain per day.

### Stage 4: Opportunity Qualification
**Input:** Pain-annotated candidates.
**Process:** Each candidate is scored on the 6-dimension scoring model (Section 4). Candidates scoring below the qualification threshold (≥7/12) are deprioritized. Ability-to-pay signals, founder reachability, and timing freshness are verified.
**Output:** Qualified opportunities stored in `opportunities` table with full score breakdowns.
**Volume:** ~8 qualified opportunities per day.

### Stage 5: Artifact Recommendation
**Input:** Qualified opportunities.
**Process:** The Artifact Strategist agent maps each opportunity's pain to a buildable artifact from the technology stack (Spring Boot API, React dashboard, Python automation, local AI workflow, MCP integration). Estimates build time and reuse percentage from existing templates (Farm Manager AI, service-template).
**Output:** Artifact recommendations with estimated hours and reuse scores.
**Volume:** ~5 opportunities have feasible artifacts.

### Stage 6: Outreach Strategy Generation
**Input:** Qualified opportunities with artifact recommendations.
**Process:** The Outreach Strategist agent generates value-first messages, technical questions, and personalized talking points. No generic templates. No begging. Each message demonstrates understanding of the founder's specific pain.
**Output:** Draft outreach stored in `outreach` table. Decision tag (BUILD / WAIT / MONITOR / IGNORE) assigned.
**Volume:** ~3 actionable outreach strategies per week.

## Data Flow Diagram

```
  HN API ──┐
  GitHub ───┤
  Reddit ───┼──▶ [ Signal Collector ] ──▶ signals table
  IH ───────┤                                  │
  PH ───────┘                                  ▼
                                    [ Intent Gatekeeper ]
                                           │
                                    (reject ~84%)
                                           │
                                           ▼
                                    [ Pain Analyst ]
                                           │
                                   (tag pain type)
                                           │
                                           ▼
                                    [ Scoring Engine ]
                                           │
                                    (score 0-12)
                                           │
                                           ▼
                                  opportunities table
                                           │
                              ┌────────────┴────────────┐
                              ▼                         ▼
                    [ Artifact Strategist ]   [ Outreach Strategist ]
                              │                         │
                              ▼                         ▼
                       artifacts table            outreach table
                              │                         │
                              └────────────┬────────────┘
                                           ▼
                                   [ Portfolio Manager ]
                                           │
                                    BUILD / WAIT /
                                   MONITOR / IGNORE
                                           │
                                           ▼
                                   outputs/traces/
```

---

# 2. SOURCE STRATEGY

Sources are tiered by signal quality, noise level, and opportunity density.

## Tier 1 — High Signal, High ROI

### Hacker News (Show HN)

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Excellent.** Show HN posts are founders launching real products, often solo or small-team. Posts contain product URLs, GitHub links, and direct pain statements in comments. |
| **Noise Level** | **Low-Medium.** Most Show HN posts represent genuine builder intent. Occasional hobby projects can be filtered by engagement count and comment sentiment. |
| **Opportunity Density** | **High.** 10-20 Show HN posts per day. ~30% represent commercial or commercially-viable products. |
| **Accessibility** | **Excellent.** Public API via Algolia (`hn.algolia.com/api`). All posts and comments are publicly indexed. User profiles often link to personal sites, GitHub, and contact information. |
| **Ability-to-Pay Indicators** | Funded startups mention investors. Solo founders with paid products (SaaS, apps) indicate revenue. Job postings in "Who is Hiring?" threads confirm budget. Product pricing pages confirm commercial viability. |

### Hacker News (Ask HN)

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Good.** Ask HN threads often surface operational pain ("How do you handle X?", "What tool do you use for Y?"). These reveal real bottlenecks. |
| **Noise Level** | **Medium.** Mixed with career advice, opinion polls, and philosophical discussions. Requires keyword filtering. |
| **Opportunity Density** | **Medium.** 5-10 relevant Ask HN threads per day contain actionable business pain. |
| **Accessibility** | **Excellent.** Same HN API. |
| **Ability-to-Pay Indicators** | Context-dependent. Users mentioning team sizes, revenue, or enterprise usage signal budget. |

### GitHub Issues & Discussions

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Excellent for implicit pain.** Feature requests, bug reports labeled "help wanted" or "good first issue", and discussion threads asking for integrations reveal unmet product needs. |
| **Noise Level** | **High.** Most issues are routine bugs. Requires aggressive filtering on labels, star count, and repository activity. |
| **Opportunity Density** | **Medium.** Across popular repositories, ~50 actionable issues/discussions surface per week. |
| **Accessibility** | **Excellent.** GitHub REST and GraphQL APIs. Rate limits manageable with authenticated tokens (~5,000 requests/hour). |
| **Ability-to-Pay Indicators** | Repository star count (>500 suggests traction). Organization-owned repos (vs personal). Linked company websites. Sponsorship tiers visible on profiles. |

### Reddit (r/SaaS, r/startups, r/smallbusiness, r/selfhosted, r/webdev)

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Good.** Founders frequently post about operational struggles, tool comparisons, and automation needs. |
| **Noise Level** | **High.** Mixed with memes, career questions, and off-topic discussions. Requires subreddit-specific keyword filtering. |
| **Opportunity Density** | **Medium.** ~20 relevant posts per week across target subreddits. |
| **Accessibility** | **Good.** Reddit API (with authentication). Rate limits are stricter but manageable for daily polling. |
| **Ability-to-Pay Indicators** | Revenue mentions ("$X MRR"), team size mentions, SaaS pricing discussions, paid tool comparisons. |

### IndieHackers

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Excellent.** Every post is from a builder. Revenue numbers are frequently shared. Pain is discussed openly. |
| **Noise Level** | **Low.** Community is self-selecting for commercial builders. |
| **Opportunity Density** | **Medium.** ~10-15 actionable posts per week. |
| **Accessibility** | **Medium.** No official API. Requires scraping or RSS monitoring. |
| **Ability-to-Pay Indicators** | **Best of any source.** Revenue milestones, pricing experiments, and customer acquisition costs are openly discussed. |

## Tier 2 — Medium Signal

### Product Hunt Discussions

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Good.** Launch discussions reveal product gaps and user feedback. |
| **Noise Level** | **Medium.** Heavy self-promotion. Filter by comment quality and upvote ratios. |
| **Opportunity Density** | **Low-Medium.** ~5-10 relevant discussions per week. |
| **Accessibility** | **Good.** GraphQL API available. |
| **Ability-to-Pay Indicators** | Products on PH are typically commercial or seeking commercialization. Pricing pages are usually linked. |

### Public Roadmaps & Feature Request Boards

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Excellent for targeted opportunities.** Voted feature requests represent validated user demand. |
| **Noise Level** | **Very Low.** Users only post when they genuinely need a feature. |
| **Opportunity Density** | **Low.** Requires per-product discovery. ~3-5 actionable items per week when monitoring 10-20 products. |
| **Accessibility** | **Variable.** Some use Canny, UserVoice, or GitHub Projects. No universal API. |
| **Ability-to-Pay Indicators** | Companies maintaining public roadmaps typically have revenue and engineering budgets. |

### Changelogs

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Medium.** Changelogs reveal product direction. Gaps in changelog coverage can signal pain. |
| **Noise Level** | **Low.** Changelogs are factual. |
| **Opportunity Density** | **Low.** Useful for monitoring specific targets, not discovery. |
| **Accessibility** | **Variable.** RSS feeds, changelog pages. |
| **Ability-to-Pay Indicators** | Active changelogs imply ongoing investment. |

## Tier 3 — Low Signal, High Effort

### Discord & Slack Communities

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Variable.** Real-time conversations can surface urgent pain, but context is fragmented. |
| **Noise Level** | **Very High.** Chat volume drowns signal. |
| **Opportunity Density** | **Low per community, aggregates up.** ~2-3 actionable signals per community per week. |
| **Accessibility** | **Poor for automation.** Bot integration required per server. Many communities prohibit scraping. |
| **Ability-to-Pay Indicators** | Weak. Community members rarely discuss revenue openly in chat. |

### Developer Forums (Dev.to, Hashnode, StackOverflow)

| Dimension | Assessment |
|:---|:---|
| **Signal Quality** | **Medium.** Technical questions can reveal product-level pain, but most are individual learning queries. |
| **Noise Level** | **High.** Dominated by tutorials and beginner questions. |
| **Opportunity Density** | **Low.** ~3-5 relevant posts per week. |
| **Accessibility** | **Good.** APIs available for all three. |
| **Ability-to-Pay Indicators** | Weak. Primarily individual developers, not companies. |

## Source Priority Matrix

```
                     HIGH SIGNAL
                         │
         IndieHackers ●  │  ● Show HN
                         │
    Public Roadmaps ●    │      ● Ask HN
                         │
         PH Discuss ●    │         ● GitHub Issues
                         │
   LOW EFFORT ───────────┼─────────── HIGH EFFORT
                         │
        Changelogs ●     │      ● Reddit
                         │
       Dev Forums ●      │         ● Discord/Slack
                         │
                     LOW SIGNAL
```

**MVP Focus:** Show HN + Ask HN + GitHub Issues + IndieHackers. These four sources deliver the highest signal-to-effort ratio.

---

# 3. AGENT ARCHITECTURE

Five specialized agents operate sequentially through the pipeline. Each agent has a single responsibility, defined inputs/outputs, and produces auditable reasoning traces.

## Agent 1: Signal Collector

**Responsibility:** Discover and ingest raw signals from configured sources.

**Implementation:**
- Python-based scheduled scrapers using `requests` and `beautifulsoup4`.
- HN Algolia API polling every 30 minutes for Show HN and Ask HN posts.
- GitHub API polling for issues labeled `help-wanted`, `feature-request`, `enhancement` on repositories with >200 stars.
- RSS/scraping fallback for IndieHackers.

**Input:** Source configuration (URLs, API keys, keywords, polling intervals).

**Output:** Raw signal records inserted into `signals` table.

**Keyword Heuristics (Seed List):**
```
EXPLICIT_PAIN_KEYWORDS = [
    "need help with", "looking for", "anyone built",
    "how do you handle", "struggling with", "broken",
    "automation", "manual process", "time-consuming",
    "expensive API", "replace OpenAI", "local AI",
    "onboarding", "integration", "workflow",
    "database query", "natural language", "dashboard"
]

IMPLICIT_PAIN_KEYWORDS = [
    "manually", "every week", "hours spent",
    "copy paste", "spreadsheet", "excel",
    "tickets per day", "support volume",
    "API costs", "billing surprise",
    "configuration nightmare", "setup complexity"
]
```

**Rate Limiting:** Respects API rate limits. Implements exponential backoff. Caches already-seen signal IDs to prevent duplicates.

---

## Agent 2: Intent Gatekeeper

**Responsibility:** Reject signals that do not represent viable commercial opportunities.

**Implementation:** Rule-based first pass + local LLM classification for ambiguous cases.

**Rejection Rules (Hard Filters):**

| Rule | Trigger | Rationale |
|:---|:---|:---|
| Hobby Filter | Post mentions "just for fun", "learning project", "toy project" | No commercial intent |
| Student Filter | Post mentions "homework", "assignment", "course project" | No ability to pay |
| Dead Repository | GitHub repo with 0 commits in last 6 months | No active development |
| Low Engagement | HN post with <5 points after 6 hours | Insufficient community validation |
| Duplicate | Signal URL already processed | Avoid re-scoring |
| Non-English | Detected non-English primary language | Outside reachability scope (MVP) |

**LLM Classification Prompt (for ambiguous signals):**
```
You are an opportunity intent classifier.

Given the following signal, classify it as:
- COMMERCIAL: The author is building or operating a product/service with revenue intent.
- HOBBY: The author is experimenting without commercial goals.
- UNKNOWN: Insufficient information to determine intent.

Signal:
[SIGNAL_TEXT]

Classification:
```

**Output:** Each signal is tagged `PASS`, `REJECT`, or `REVIEW`. Rejection reason is logged in the audit trace.

---

## Agent 3: Pain Analyst

**Responsibility:** Extract, classify, and summarize pain from signals that pass the Intent Gatekeeper.

**Pain Taxonomy:**

```
PAIN TYPES
├── EXPLICIT PAIN
│   ├── Direct Request     ("We need X")
│   ├── Tool Search        ("Looking for a tool that does Y")
│   └── Frustration        ("Our current process is broken")
│
├── IMPLICIT PAIN
│   ├── Operational Friction  ("We manually do X every week")
│   ├── Cost Pressure         ("Spending $Y/month on Z API")
│   ├── Scale Bottleneck      ("Can't handle volume above N")
│   └── Complexity Burden     ("Setup takes 3 days for new clients")
│
└── NO PAIN
    └── Informational / Opinion / Showcase (no actionable problem)
```

**LLM Analysis Prompt:**
```
You are an opportunity pain analyst.

Analyze the following signal for business pain.

Signal:
[SIGNAL_TEXT]

Extract:
1. PAIN_TYPE: explicit | implicit | none
2. PAIN_SUMMARY: One sentence describing the core problem.
3. EVIDENCE: Direct quotes from the signal supporting your classification.
4. OPERATIONAL_BOTTLENECK: What manual, costly, or broken process exists?
5. INFERRED_SOLUTION_CATEGORY: database | automation | AI | dashboard | integration | other
6. CONFIDENCE: high | medium | low

If pain type is "none", set all other fields to null.

Output as JSON.
```

**Output:** Pain-annotated signals stored with `pain_type`, `pain_summary`, `evidence`, `bottleneck`, `solution_category`, and `confidence`.

---

## Agent 4: Artifact Strategist

**Responsibility:** Map qualified pain to buildable artifacts from the existing technology stack.

**Artifact Catalog:**

| Artifact Type | Stack | Typical Build Time | Reuse From Templates |
|:---|:---|:---|:---|
| NL→SQL Query Interface | Spring Boot + Ollama + SQLite + React | 4-6 hours | 80% (Farm Manager AI) |
| REST API Backend | Spring Boot + JdbcTemplate + SQLite | 3-5 hours | 60% |
| Data Dashboard | React + TypeScript + Tailwind + REST API | 4-8 hours | 40% |
| Python Automation Pipeline | Python + Local LLM + File I/O | 2-4 hours | 50% |
| AI Classification Service | Python + Ollama + qwen2.5-coder:7b | 3-5 hours | 70% |
| MCP Tool Integration | Python + MCP SDK | 2-4 hours | 60% |
| Local LLM Replacement Workflow | Ollama + Python + API wrapper | 3-6 hours | 65% |

**Decision Logic:**
```
IF pain.solution_category == "database" AND pain involves "natural language":
    → Recommend NL→SQL Interface (reuse: 80%)

IF pain.solution_category == "automation" AND pain involves "manual process":
    → Recommend Python Automation Pipeline (reuse: 50%)

IF pain.solution_category == "AI" AND pain involves "API costs" or "local":
    → Recommend Local LLM Replacement Workflow (reuse: 65%)

IF pain.solution_category == "dashboard":
    → Recommend Data Dashboard (reuse: 40%)

IF pain.solution_category == "integration":
    → Recommend MCP Tool Integration (reuse: 60%)
```

**Output:** Artifact recommendation with estimated build hours, reuse percentage, and technology stack.

---

## Agent 5: Outreach Strategist

**Responsibility:** Generate value-first, personalized outreach messages and technical questions.

**Outreach Principles:**
1. **Lead with value, not with a request.** Show that you understand their specific problem.
2. **Demonstrate competence through specificity.** Reference their exact product, feature gap, or pain point.
3. **Offer a tangible artifact.** Mention what you could build, with an estimated timeline.
4. **Ask a smart technical question.** Show you've done research. This earns credibility.
5. **Never beg for a job.** Position as a peer solving an interesting problem.

**Message Template Structure:**
```
SUBJECT: [Specific reference to their product/post]

HOOK: [1-2 sentences showing you understand their problem]

VALUE: [What you built or could build. Reference your stack and proof (Farm Manager AI).]

QUESTION: [1 smart technical question about their architecture, constraints, or roadmap]

CLOSE: [Offer to share your work. No pressure. No "please hire me."]
```

**Anti-Patterns (Hard Reject):**
- "I'm looking for work" → REJECT
- "Please consider my application" → REJECT
- "I can do anything you need" → REJECT (too vague)
- Generic messages sent to multiple founders → REJECT
- Messages longer than 200 words → TRIM

**Output:** Draft outreach message, 2-3 suggested technical questions, and outreach channel recommendation (email, HN reply, GitHub issue comment, LinkedIn DM).

---

# 4. OPPORTUNITY SCORING MODEL

Every qualified opportunity is scored on a strict 0-12 quantitative model across 6 dimensions.

## Scoring Dimensions

### Dimension 1: Pain Severity & Clarity (0-2)

| Score | Criteria |
|:---|:---|
| **0** | No clear pain detected. Signal is informational or opinion-based. |
| **1** | Implicit pain detected. Operational friction exists but is not directly stated. Requires inference. |
| **2** | Explicit pain stated clearly. Founder/team directly describes a problem they need solved. Direct quotes available as evidence. |

### Dimension 2: Ability to Pay (0-2)

| Score | Criteria |
|:---|:---|
| **0** | No evidence of revenue, funding, or budget. Individual hobbyist or unfunded side project. |
| **1** | Indirect indicators of commercial viability. Product exists with users. Company website present. Small team mentioned. |
| **2** | Strong evidence of ability to pay. Revenue mentioned. Funded startup. Pricing page exists. Job postings indicate hiring budget. Enterprise customers referenced. |

### Dimension 3: Accessibility & Reachability (0-2)

| Score | Criteria |
|:---|:---|
| **0** | No contact information discoverable. Anonymous or pseudonymous author. No linked profiles. |
| **1** | Partial contact path. GitHub profile with email. LinkedIn profile found. Personal website without direct contact. |
| **2** | Direct contact path available. Email discoverable. Active on HN/Twitter/LinkedIn. Responded to community comments. History of engaging with outreach. |

### Dimension 4: Timing & Freshness (0-2)

| Score | Criteria |
|:---|:---|
| **0** | Signal is >30 days old. Product launch was months ago. Conversation has gone cold. |
| **1** | Signal is 7-30 days old. Product is active but not in a launch moment. |
| **2** | Signal is <7 days old. Product was just launched. Active discussion happening. Founder is currently engaged in comments. |

### Dimension 5: Artifact Feasibility (0-2)

| Score | Criteria |
|:---|:---|
| **0** | No artifact can be built with the current stack. Problem requires domain expertise outside competency (e.g., medical devices, hardware). |
| **1** | An artifact can be built but requires significant new learning or custom architecture. Build time >12 hours. |
| **2** | An artifact can be built confidently with the current stack. Build time ≤8 hours. Clear mapping to existing capabilities (Spring Boot, React, Ollama, Python). |

### Dimension 6: Artifact Reuse Factor (0-2)

| Score | Criteria |
|:---|:---|
| **0** | Artifact is entirely custom. No existing templates, components, or patterns can be reused. |
| **1** | Partial reuse possible. Some components (e.g., safety validation, DTO patterns, table rendering) can be adapted from existing projects. Reuse: 30-60%. |
| **2** | High reuse. Artifact can be built primarily from existing templates (Farm Manager AI, service-template). Only schema and configuration changes needed. Reuse: >60%. |

## Score Interpretation

| Total Score | Label | Action |
|:---|:---|:---|
| **10-12** | 🟢 **Prime Opportunity** | BUILD immediately. Begin artifact construction and outreach within 24 hours. |
| **7-9** | 🟡 **Strong Candidate** | BUILD if capacity permits. Otherwise WAIT and revisit within 7 days. |
| **4-6** | 🟠 **Monitor** | MONITOR for signal changes. Re-score if new information emerges. |
| **0-3** | 🔴 **Ignore** | IGNORE. Do not invest time. |

## Qualification Threshold

**Minimum score to qualify: 7/12.**

Opportunities scoring below 7 are automatically deprioritized. They remain in the database for re-scoring if new evidence emerges, but no artifact or outreach work begins.

---

# 5. OPPORTUNITY PORTFOLIO MANAGEMENT

The system does not simply score opportunities. It actively manages a portfolio of opportunities across four decision states.

## Decision States

```
              ┌──────────┐
              │  Signal   │
              │  Ingested │
              └─────┬─────┘
                    │
                    ▼
              ┌──────────┐
              │  Scored   │
              │  (0-12)   │
              └─────┬─────┘
                    │
         ┌─────────┼─────────┬──────────┐
         ▼         ▼         ▼          ▼
    ┌─────────┐┌────────┐┌─────────┐┌────────┐
    │  BUILD  ││  WAIT  ││ MONITOR ││ IGNORE │
    │ (10-12) ││ (7-9)  ││  (4-6)  ││ (0-3)  │
    └────┬────┘└───┬────┘└────┬────┘└────────┘
         │         │          │
         ▼         ▼          ▼
    ┌─────────┐┌────────┐┌─────────┐
    │ Artifact ││Revisit ││Re-score │
    │ + Reach  ││in 7d   ││on new   │
    │ Out      ││        ││evidence │
    └─────────┘└────────┘└─────────┘
```

## Decision Rules

### BUILD
- Score ≥ 10/12.
- OR: Score 7-9 AND artifact reuse factor = 2 AND timing = 2 (fresh + high reuse = low-risk investment).
- **Action:** Begin artifact construction immediately. Generate outreach draft. Execute within 24-48 hours.
- **Capacity Constraint:** Maximum 2 concurrent BUILD targets. Queue additional BUILDs if slots are full.

### WAIT
- Score 7-9.
- Pain is real but timing or reachability is suboptimal.
- **Action:** Set a 7-day reminder. Re-score with fresh data. Promote to BUILD if score increases, demote to MONITOR if it decreases.
- **Maximum WAIT Duration:** 21 days. After 3 re-scores with no improvement, auto-demote to MONITOR.

### MONITOR
- Score 4-6.
- Some signal of pain but insufficient qualification evidence.
- **Action:** Keep in the database. Re-score only if new evidence surfaces (e.g., founder posts again, repository activity spikes, new funding announced).
- **Maximum MONITOR Duration:** 60 days. Auto-demote to IGNORE after 60 days with no new evidence.

### IGNORE
- Score 0-3.
- OR: Failed Intent Gatekeeper.
- OR: Exceeded WAIT/MONITOR time limits.
- **Action:** No further investment. Record stays in database for historical analysis but receives no active attention.

## Portfolio Balance Targets

| State | Target Count | Rationale |
|:---|:---|:---|
| BUILD | 1-2 active | Focus. Avoid context-switching between too many artifacts. |
| WAIT | 3-5 | Pipeline buffer. Ensures next BUILD target is always ready. |
| MONITOR | 10-20 | Background awareness. Low effort to maintain. |
| IGNORE | Unlimited | Archive. No ongoing cost. |

---

# 6. TARGET DATA MODEL

The canonical JSON schema for a fully qualified opportunity:

```json
{
  "identification": {
    "id": "opp-2026-0613-001",
    "name": "krogenx / Manger",
    "platform": "hackernews",
    "url": "https://news.ycombinator.com/item?id=48351776",
    "founder_handle": "krogenx",
    "founder_name": "Matt",
    "discovered_at": "2026-06-01T12:00:00Z"
  },

  "diagnosis": {
    "problem_summary": "Livestock management app needs local AI natural language query interface for offline farm databases.",
    "evidence": [
      "Show HN post describes Manger as a livestock management app.",
      "Comment by krogenx: 'local AI integration where you can simply ask how many eggs did I collect last week'",
      "App runs on SQLite locally, prioritizes offline data storage."
    ],
    "pain_type": "explicit",
    "pain_category": "database",
    "operational_bottleneck": "Farm operators cannot query their own SQLite databases without writing SQL manually.",
    "inferred_solution": "Natural language to SQL interface using local LLM."
  },

  "qualification": {
    "ability_to_pay_signals": [
      "Product is listed on App Store and Google Play (commercial distribution).",
      "Professional product website at manger.app.",
      "Solo founder actively developing and maintaining the product."
    ],
    "accessibility_signals": [
      "HN username: krogenx",
      "Responded to comments on Show HN post.",
      "GitHub profile linked.",
      "Email discoverable via GitHub profile."
    ],
    "freshness": "2026-06-01 (12 days ago at time of scoring)"
  },

  "scoring": {
    "pain_severity": 2,
    "ability_to_pay": 1,
    "accessibility": 2,
    "timing": 1,
    "artifact_feasibility": 2,
    "artifact_reuse": 2,
    "total": 10,
    "label": "Prime Opportunity"
  },

  "execution": {
    "recommended_artifact": "NL→SQL Query Interface for Livestock Database",
    "artifact_type": "spring_boot_ollama_sqlite_react",
    "estimated_build_hours": 5,
    "reuse_percentage": 80,
    "reuse_source": "Farm Manager AI template",
    "technology_stack": ["Java 21", "Spring Boot", "SQLite", "Ollama qwen2.5-coder:7b", "React"]
  },

  "outreach": {
    "draft_message": "Subject: Built a working NL→SQL demo for Manger...",
    "suggested_questions": [
      "What SQLite schema does Manger use for livestock tracking?",
      "Are you considering on-device AI or a local server approach?",
      "What's the current query interface for farm operators?"
    ],
    "channel": "email"
  },

  "decision": {
    "action": "BUILD",
    "reasoning": "Score 10/12. Explicit pain, high reuse from existing template, founder is reachable and recently active. Artifact can be built in ~5 hours.",
    "decided_at": "2026-06-01T14:30:00Z"
  }
}
```

---

# 7. DATABASE DESIGN

Minimal SQLite schema for MVP. Five tables. No over-engineering.

```sql
-- Source Configuration
CREATE TABLE IF NOT EXISTS sources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,           -- 'hackernews_showhn', 'github_issues', etc.
    tier INTEGER NOT NULL DEFAULT 1,     -- 1, 2, or 3
    base_url TEXT NOT NULL,
    polling_interval_minutes INTEGER NOT NULL DEFAULT 30,
    last_polled_at DATETIME,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Raw Signals
CREATE TABLE IF NOT EXISTS signals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_id INTEGER NOT NULL,
    external_id TEXT NOT NULL,            -- HN item ID, GitHub issue number, etc.
    url TEXT NOT NULL,
    title TEXT,
    author TEXT,
    content TEXT,                          -- Full text of post/comment/issue
    engagement_score INTEGER DEFAULT 0,   -- Points, upvotes, stars
    signal_date DATETIME,                 -- When the signal was posted
    status TEXT NOT NULL DEFAULT 'new',   -- new, passed, rejected, processed
    rejection_reason TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES sources(id),
    UNIQUE(source_id, external_id)
);

-- Qualified Opportunities
CREATE TABLE IF NOT EXISTS opportunities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    signal_id INTEGER NOT NULL,
    name TEXT NOT NULL,                    -- 'krogenx / Manger'
    founder_handle TEXT,
    founder_name TEXT,
    platform TEXT NOT NULL,
    url TEXT NOT NULL,
    problem_summary TEXT NOT NULL,
    evidence TEXT,                          -- JSON array of evidence strings
    pain_type TEXT NOT NULL,               -- explicit, implicit
    pain_category TEXT,                    -- database, automation, AI, dashboard, integration
    bottleneck TEXT,

    -- Scoring (0-2 each)
    score_pain INTEGER NOT NULL DEFAULT 0,
    score_pay INTEGER NOT NULL DEFAULT 0,
    score_access INTEGER NOT NULL DEFAULT 0,
    score_timing INTEGER NOT NULL DEFAULT 0,
    score_feasibility INTEGER NOT NULL DEFAULT 0,
    score_reuse INTEGER NOT NULL DEFAULT 0,
    score_total INTEGER GENERATED ALWAYS AS (
        score_pain + score_pay + score_access + score_timing + score_feasibility + score_reuse
    ) STORED,

    -- Decision
    decision TEXT NOT NULL DEFAULT 'MONITOR',  -- BUILD, WAIT, MONITOR, IGNORE
    decision_reasoning TEXT,
    decision_at DATETIME,

    -- Metadata
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (signal_id) REFERENCES signals(id)
);

-- Artifact Recommendations
CREATE TABLE IF NOT EXISTS artifacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    opportunity_id INTEGER NOT NULL,
    artifact_type TEXT NOT NULL,            -- 'nl_sql_interface', 'python_automation', etc.
    description TEXT NOT NULL,
    technology_stack TEXT,                  -- JSON array
    estimated_hours REAL NOT NULL,
    reuse_percentage INTEGER NOT NULL DEFAULT 0,
    reuse_source TEXT,
    status TEXT NOT NULL DEFAULT 'proposed', -- proposed, building, completed, abandoned
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id)
);

-- Outreach Records
CREATE TABLE IF NOT EXISTS outreach (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    opportunity_id INTEGER NOT NULL,
    channel TEXT NOT NULL,                  -- email, hn_reply, github_comment, linkedin
    draft_message TEXT NOT NULL,
    suggested_questions TEXT,               -- JSON array
    status TEXT NOT NULL DEFAULT 'draft',  -- draft, sent, replied, no_response, converted
    sent_at DATETIME,
    response_at DATETIME,
    outcome TEXT,                           -- conversation, referral, contract, rejection, silence
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (opportunity_id) REFERENCES opportunities(id)
);
```

## Index Strategy

```sql
CREATE INDEX idx_signals_status ON signals(status);
CREATE INDEX idx_signals_source_date ON signals(source_id, signal_date);
CREATE INDEX idx_opportunities_decision ON opportunities(decision);
CREATE INDEX idx_opportunities_score ON opportunities(score_total DESC);
CREATE INDEX idx_outreach_status ON outreach(status);
```

---

# 8. AUDITABILITY

Every decision in the pipeline must be explainable. No black-box scoring. No unjustified rejections. No untraced recommendations.

## Audit Requirements

| Decision Point | Must Record |
|:---|:---|
| Signal rejection by Gatekeeper | Rejection rule or LLM classification with reasoning |
| Pain classification | Pain type, evidence quotes, confidence level |
| Scoring | Per-dimension score with justification for each score |
| Portfolio decision (BUILD/WAIT/MONITOR/IGNORE) | Decision rule triggered, reasoning text |
| Artifact recommendation | Why this artifact type was chosen, reuse source, build estimate basis |
| Outreach generation | Message strategy rationale, channel selection reasoning |

## Integration with Existing Trace Infrastructure

The system integrates with the existing `trace_logger.py` and `audit_builder.py` modules from the Opportunity Intelligence System.

### Trace Logger Integration

Every pipeline stage emits structured trace events:

```python
from trace_logger import TraceLogger

trace = TraceLogger(target="krogenx", operation="pain_analysis")

trace.log_source("https://news.ycombinator.com/item?id=48351776", status="visited")
trace.log_evidence("Comment by krogenx mentions 'local AI integration'")
trace.log_assumption("Assumed commercial intent based on App Store listing")
trace.log_decision("pain_type", "explicit", reasoning="Direct quote requesting AI query interface")
trace.save()  # Writes to outputs/traces/krogenx-trace.md
```

### Audit Builder Integration

After pipeline completion, the audit builder generates a human-readable audit report:

```python
from audit_builder import AuditBuilder

audit = AuditBuilder(target="krogenx")
audit.add_section("Signal Source", {"url": "...", "platform": "hackernews"})
audit.add_section("Gatekeeper Result", {"status": "PASS", "reason": "Commercial product detected"})
audit.add_section("Pain Analysis", {"type": "explicit", "evidence": ["..."]})
audit.add_section("Scoring", {"pain": 2, "pay": 1, "access": 2, "timing": 1, "feasibility": 2, "reuse": 2, "total": 10})
audit.add_section("Decision", {"action": "BUILD", "reasoning": "..."})
audit.generate()  # Writes to outputs/audit/krogenx-audit.md
```

### Trace Storage

```
outputs/
├── traces/
│   ├── krogenx-trace.md
│   ├── AdarshRao23-trace.md
│   └── [founder]-trace.md
│
└── audit/
    ├── krogenx-audit.md
    ├── AdarshRao23-audit.md
    └── [founder]-audit.md
```

---

# 9. SYSTEMIC RISKS

## Risk Analysis & Mitigation

### 1. Sunk-Cost Engineering

**Risk:** Spending 8+ hours building an artifact for a founder who never responds.

**Probability:** High (>50% of outreach receives no response).

**Mitigation:**
- Enforce the scoring threshold (≥7/12) before any artifact work begins.
- **Time-box artifact construction to 5 hours maximum** for first-contact artifacts. Full implementations only happen after a founder conversation.
- Build artifacts that are portfolio-reusable regardless of founder response. Every artifact must pass the "Would I show this to a recruiter?" test.

### 2. Founder Non-Response

**Risk:** Founders ignore outreach messages entirely.

**Probability:** High (~60-70% non-response rate is normal for cold outreach).

**Mitigation:**
- Accept non-response as the base case. Design the system to generate value even when founders don't reply (portfolio artifacts, case studies, reusable templates).
- Follow up exactly once after 7 days. No further follow-ups.
- Track response rates per channel and per source to optimize future outreach allocation.

### 3. Hallucinated Pain

**Risk:** The local LLM invents pain that doesn't exist in the signal text.

**Probability:** Medium. LLMs can over-interpret neutral statements.

**Mitigation:**
- Require the Pain Analyst to output direct evidence quotes from the signal text.
- If no direct quote supports the pain classification, confidence must be set to "low" and the opportunity score is capped at 1/2 on the pain dimension.
- Human review of all opportunities scoring ≥7 before artifact work begins.

### 4. False Positives

**Risk:** System recommends investing time in opportunities that appear valuable but are actually dead ends (abandoned products, founders who've moved on, problems already solved).

**Probability:** Medium.

**Mitigation:**
- Timing dimension (0-2) penalizes stale signals.
- Verification step: Before BUILD, manually confirm the product is still active (check website, recent commits, recent social posts).
- Maintain a "false positive log" to train better filtering heuristics over time.

### 5. False Negatives

**Risk:** System rejects genuinely valuable opportunities due to overly aggressive filtering.

**Probability:** Low-Medium.

**Mitigation:**
- Weekly review of REJECT-tagged signals to check for missed opportunities.
- Adjust keyword heuristics and gatekeeper rules based on false negative patterns.
- Keep rejection rates visible in dashboard metrics.

### 6. Source Bias

**Risk:** Over-indexing on a single source (e.g., only HN) creates a narrow, homogeneous opportunity pipeline.

**Probability:** Medium in MVP phase (only Tier 1 sources implemented).

**Mitigation:**
- Track source distribution in weekly metrics.
- Enforce minimum polling across all active sources.
- Add Tier 2 sources in Month 2.

### 7. API Rate Limits

**Risk:** Aggressive polling triggers rate limiting or IP bans from source APIs.

**Probability:** Low (with proper implementation).

**Mitigation:**
- Respect published rate limits. HN Algolia: no strict limit but be polite. GitHub: 5,000 requests/hour authenticated.
- Implement exponential backoff on 429/503 responses.
- Cache all responses locally. Never re-fetch unchanged data.

### 8. Opportunity Saturation

**Risk:** Spending too much time on opportunity discovery and too little time on artifact construction and outreach execution.

**Probability:** Medium. Discovery is addictive; execution is hard.

**Mitigation:**
- **Hard rule: Maximum 1 hour per day on discovery and scoring.** The rest of available time must go to BUILD and outreach.
- Dashboard prominently displays "Hours spent discovering vs. Hours spent building" ratio. Target ratio: 1:4 (1 hour discovery for every 4 hours building).

---

# 10. MVP BUILD PLAN

The MVP must be buildable by a single student in under one week.

## Folder Structure

```
opportunity-engine/
├── config/
│   ├── sources.json          # Source definitions and polling config
│   ├── keywords.json         # Pain keyword heuristics
│   └── scoring_rules.json    # Dimension thresholds
│
├── collectors/
│   ├── hn_collector.py       # Hacker News API poller
│   ├── github_collector.py   # GitHub Issues/Discussions poller
│   └── base_collector.py     # Abstract collector interface
│
├── agents/
│   ├── gatekeeper.py         # Intent Gatekeeper (rule-based + LLM)
│   ├── pain_analyst.py       # Pain extraction and classification
│   ├── artifact_strategist.py # Artifact mapping
│   └── outreach_strategist.py # Message generation
│
├── scoring/
│   └── scorer.py             # 6-dimension scoring engine
│
├── database/
│   ├── schema.sql            # SQLite DDL
│   ├── db.py                 # Database connection and helpers
│   └── opportunity.db        # SQLite database file (gitignored)
│
├── outputs/
│   ├── traces/               # Audit traces per target
│   ├── audit/                # Audit reports per target
│   ├── founders/             # Founder dossiers
│   └── outreach/             # Draft messages
│
├── utils/
│   ├── trace_logger.py       # Shared tracing module
│   ├── audit_builder.py      # Audit report generator
│   └── llm_client.py         # Ollama API client wrapper
│
├── pipeline.py               # Main orchestrator (runs full pipeline)
├── requirements.txt          # Python dependencies
└── README.md                 # Project documentation
```

## Milestones

### Day 1: Foundation (4-5 hours)
- [ ] Initialize project structure and `requirements.txt`.
- [ ] Create `database/schema.sql` and `db.py` with connection helpers.
- [ ] Implement `utils/llm_client.py` (Ollama HTTP wrapper with timeout/retry).
- [ ] Implement `utils/trace_logger.py` (adapted from existing module).
- [ ] Verify database creation and LLM connectivity.

### Day 2: Signal Collection (4-5 hours)
- [ ] Implement `collectors/base_collector.py` (abstract interface).
- [ ] Implement `collectors/hn_collector.py` (Show HN + Ask HN via Algolia API).
- [ ] Configure `config/sources.json` and `config/keywords.json`.
- [ ] Run collector. Verify signals are stored in `signals` table.
- [ ] Target: ≥50 signals collected from HN in a single run.

### Day 3: Gatekeeper + Pain Analysis (4-5 hours)
- [ ] Implement `agents/gatekeeper.py` with hard rejection rules.
- [ ] Implement LLM-based intent classification for ambiguous signals.
- [ ] Implement `agents/pain_analyst.py` with LLM pain extraction.
- [ ] Run pipeline through Stage 1-3. Verify pain annotations.
- [ ] Target: ≥10 pain-annotated candidates from Day 2's signals.

### Day 4: Scoring + Portfolio Management (3-4 hours)
- [ ] Implement `scoring/scorer.py` with all 6 dimensions.
- [ ] Implement portfolio decision rules (BUILD/WAIT/MONITOR/IGNORE).
- [ ] Configure `config/scoring_rules.json` with threshold values.
- [ ] Run full pipeline. Verify scored opportunities in database.
- [ ] Target: ≥3 opportunities scoring ≥7/12.

### Day 5: Artifact + Outreach Strategy (4-5 hours)
- [ ] Implement `agents/artifact_strategist.py` with artifact catalog mapping.
- [ ] Implement `agents/outreach_strategist.py` with message generation.
- [ ] Implement `utils/audit_builder.py` for human-readable reports.
- [ ] Run complete pipeline end-to-end. Verify full output chain.
- [ ] Target: ≥1 complete opportunity with artifact recommendation + draft outreach.

### Day 6: Integration + Polish (3-4 hours)
- [ ] Implement `pipeline.py` as the single-command orchestrator.
- [ ] Add CLI flags: `--collect`, `--score`, `--full`, `--target [name]`.
- [ ] Generate audit reports for top 3 scored opportunities.
- [ ] Write `README.md` with setup and usage instructions.
- [ ] Final end-to-end validation run.

### Day 7: Buffer + Validation (2-3 hours)
- [ ] Fix any bugs discovered during Day 6 validation.
- [ ] Run pipeline on fresh data (new day's signals).
- [ ] Review audit traces for accuracy and completeness.
- [ ] Document lessons learned and next iteration priorities.

## Validation Strategy

| Validation Check | Criteria | Method |
|:---|:---|:---|
| Signal ingestion works | ≥50 signals per HN poll | Run `hn_collector.py`, count rows in `signals` table |
| Gatekeeper filters correctly | 70-90% rejection rate | Manual review of 20 random rejections for false negatives |
| Pain analysis is accurate | ≥80% agreement with human assessment | Manually review 10 pain annotations against source text |
| Scoring produces spread | Scores distributed across 0-12 range | Histogram of scores for 50+ opportunities |
| Artifacts map correctly | Recommended artifact matches pain category | Manual review of 10 artifact recommendations |
| Outreach is specific | Messages reference specific product/pain | Manual review of 5 draft messages for personalization |
| Audit traces are complete | Every scored opportunity has a trace file | Check `outputs/traces/` for corresponding files |
| End-to-end pipeline runs | Single command processes signal→outreach | Run `python pipeline.py --full` and verify outputs |

---

# 11. SUCCESS METRICS

## Primary KPIs

### Discovery Metrics

| Metric | Target (Weekly) | Measurement |
|:---|:---|:---|
| Raw signals collected | ≥200 | COUNT of `signals` table rows created per week |
| Signals passing gatekeeper | ≥30 | COUNT of signals with status = 'passed' per week |
| Pain-annotated candidates | ≥15 | COUNT of signals with non-null pain_type per week |
| Qualified opportunities (score ≥7) | ≥5 | COUNT of opportunities with score_total ≥ 7 per week |

### Execution Metrics

| Metric | Target (Weekly) | Measurement |
|:---|:---|:---|
| Artifacts built | ≥1 | COUNT of artifacts with status = 'completed' per week |
| Outreach messages sent | ≥3 | COUNT of outreach with status = 'sent' per week |
| Average build time per artifact | ≤6 hours | AVG of actual hours logged per completed artifact |

### Conversion Metrics

| Metric | Target (Monthly) | Measurement |
|:---|:---|:---|
| Founder response rate | ≥20% | COUNT(replied) / COUNT(sent) for outreach records |
| Outreach → Conversation | ≥15% | COUNT(outcome='conversation') / COUNT(sent) |
| Conversation → Referral | ≥10% | COUNT(outcome='referral') / COUNT(outcome='conversation') |
| Conversation → Revenue | ≥5% | COUNT(outcome='contract') / COUNT(outcome='conversation') |
| Artifact → Portfolio addition | ≥80% | COUNT of completed artifacts added to GitHub portfolio |

### Efficiency Metrics

| Metric | Target | Measurement |
|:---|:---|:---|
| Discovery:Build time ratio | 1:4 | Hours on discovery / Hours on building per week |
| Expected value per hour invested | Increasing trend | (Qualified opportunities × avg conversion rate) / total hours |
| False positive rate | ≤20% | COUNT of BUILD decisions that yielded zero response or value / COUNT of BUILD decisions |
| Source diversity index | ≥2 sources producing qualified opportunities | COUNT DISTINCT source_id for opportunities scoring ≥7 |

## Compounding Metrics (Quarterly)

| Metric | Target | Measurement |
|:---|:---|:---|
| Portfolio artifacts | ≥8 per quarter | Total completed, polished GitHub projects |
| Founder conversations | ≥6 per quarter | Total unique founders who responded |
| Referrals received | ≥2 per quarter | Introductions made by previous contacts |
| Revenue generated | Any | Total contract/freelance income from system-sourced opportunities |
| Reputation signals | Increasing | GitHub stars, HN karma, LinkedIn connection accepts from founders |

## Anti-Metrics (Things to Minimize)

| Anti-Metric | Maximum Acceptable | Alert Threshold |
|:---|:---|:---|
| Hours spent on IGNORE-scored targets | 0 hours/week | Any time logged on score <4 |
| Artifacts abandoned mid-build | ≤1 per month | 2 abandoned in same month |
| Generic outreach messages sent | 0 | Any message not referencing specific product/pain |
| Days without pipeline execution | ≤2 consecutive | 3 consecutive days without running pipeline |

---

## Appendix: Technology Stack Summary

| Layer | Technology | Rationale |
|:---|:---|:---|
| **Pipeline Orchestration** | Python 3.11 | Rapid scripting, rich HTTP/JSON ecosystem, existing automation skills |
| **Local LLM** | Ollama + qwen2.5-coder:7b | Zero API cost, offline-capable, proven with Farm Manager AI |
| **Database** | SQLite | Single-file, zero-config, portable, proven with existing projects |
| **Artifact Backend** | Java 21 + Spring Boot | High-quality portfolio artifacts, proven stack |
| **Artifact Frontend** | React + TypeScript + Tailwind | Modern, recruiter-friendly UI stack |
| **Tracing** | trace_logger.py + audit_builder.py | Existing modules from Opportunity Intelligence System |
| **Hardware** | Ryzen 7 8845HS / RTX 4050 6GB / 32GB DDR5 | Sufficient for local LLM inference at acceptable latency |
