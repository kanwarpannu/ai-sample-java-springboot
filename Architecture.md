                         ┌─────────────────────┐
                         │     Swagger UI      │
                         └──────────┬──────────┘
                                    │
                                    ▼
                   ┌────────────────────────────────────────┐
                   │       Onboarding Agent Service         │
                   │         (MCP Host, port 8080)          │
                   │                                        │
                   │  ┌─────────────────────────────────┐   │
                   │  │  State Machine (per session)    │   │
                   │  │  AgentProcessingState:          │   │
                   │  │  IDLE → PROCESSING →            │   │
                   │  │  CALLING_TOOL → RESPONDING →    │   │
                   │  │  DONE / ERROR                   │   │
                   │  │                                 │   │
                   │  │  OnboardingWorkflowState:       │   │
                   │  │  NOT_STARTED → PLAN_CREATED →   │   │
                   │  │  IN_PROGRESS ⇄ BLOCKED →        │   │
                   │  │  COMPLETED                      │   │
                   │  └─────────────────────────────────┘   │
                   │                                        │
                   │  ┌─────────────────────────────────┐   │
                   │  │  MDC Logging (sessionId in      │   │
                   │  │  every log line via SLF4J MDC)  │   │
                   │  └─────────────────────────────────┘   │
                   │                                        │
                   └───────┬───────────────┬────────────────┘
                           │               │
                    MCP Client       MCP Client
                    (Streamable)     (Streamable)
                           │               │
          ┌────────────────▼─┐     ┌──────▼────────────┐
          │ Knowledge MCP    │     │ Onboarding MCP    │
          │ Server           │     │ Server            │
          │ (port 8081)      │     │ (port 8082)       │
          └────────┬─────────┘     └──────┬────────────┘
                   │                      │
                   ▼                      ▼
          ┌────────────────┐      ┌───────────────────┐
          │  RAG Service   │      │   PostgreSQL       │
          │  (port 8083)   │      │  (port 5432)       │
          └───────┬────────┘      │  onboarding_db     │
                  │               └───────────────────┘
                  ▼
          ┌────────────────┐
          │   PostgreSQL   │
          │  + pgvector    │
          │  (port 5433)   │
          │   rag_db       │
          └────────────────┘


## Components

### 1. Onboarding Agent Service (port 8080)

The orchestrator. Hosts the LLM (Ollama / gemma4) via Spring AI and acts as an MCP host connecting to both downstream MCP servers. Every chat session carries two state machines and a session-correlated logger.

**Key responsibilities:**
- Accept chat requests (`POST /api/v1/chat`) and streaming requests (`POST /api/v1/chat/stream`)
- Maintain per-session conversation history (`SessionContext`)
- Track agent activity via `AgentStateMachine` (resets after each request)
- Track onboarding progress via `OnboardingStateMachine` (persists for session lifetime)
- Instrument every MCP tool call via `StateAwareToolCallback` — transitions state, logs tool name and result
- Set SLF4J MDC `sessionId` on every log line so any session's activity can be isolated with a single grep

**State machine — AgentProcessingState:**
```
IDLE → PROCESSING → CALLING_TOOL → PROCESSING → RESPONDING → DONE
                         ↑__________↑  (one cycle per tool call)
ANY  → ERROR  (always allowed; resets to IDLE in finally)
```

**State machine — OnboardingWorkflowState (tool-driven):**

| MCP tool called | Transition |
|---|---|
| `createOnboardingPlan` | any → PLAN_CREATED |
| `updateOnboardingStep` | → IN_PROGRESS |
| `reportBlocker` | → BLOCKED |
| `resolveBlocker` | BLOCKED → IN_PROGRESS |

**Log format:**
```
HH:mm:ss.SSS LEVEL [sessionId] [thread] logger - message
```
Example:
```
14:32:01.050 INFO  [abc-123] StateAwareToolCallback - Calling tool: createOnboardingPlan
14:32:01.051 INFO  [abc-123] OnboardingStateMachine - [onboardingState: NOT_STARTED->PLAN_CREATED]
```

---

### 2. Knowledge MCP Server (port 8081)

Exposes engineering knowledge through MCP tools and MCP Resources. Delegates all data retrieval to `rag-service` over HTTP.

**MCP Tools (4):**

| Tool | Description |
|---|---|
| `searchDocuments(keyword)` | Semantic search across all documents |
| `getDocument(documentId)` | Fetch full document content by ID |
| `listDocuments(category)` | List documents, optionally filtered by category |
| `searchByCategory(category, keyword)` | Semantic search scoped to a category |

**MCP Resources:** Each document exposed as `knowledge://{category}/{documentId}` (registered at startup — fails fast if `rag-service` is unreachable).

---

### 3. RAG Service (port 8083)

Plain REST service. Handles document ingestion and semantic vector search. Backed by PostgreSQL with the pgvector extension.

**REST API:**

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/documents` | Ingest a document (JSON body) |
| `POST` | `/api/v1/documents/upload` | Ingest a plain-text file (multipart) |
| `GET` | `/api/v1/documents` | List documents, optional `?category=` filter |
| `GET` | `/api/v1/documents/{id}` | Fetch a single document by ID |
| `POST` | `/api/v1/search` | Semantic search — `{query, category?, keyword?}` |

**Search flow:** query → Ollama (`nomic-embed-text`) → `VECTOR(768)` → pgvector cosine similarity (`<=>`) top-5 → optional keyword post-filter.

---

### 4. Onboarding MCP Server (port 8082)

Tracks developer onboarding progress. Backed by PostgreSQL (`onboarding_db`). Schema managed by Flyway (4 tables: `step_templates`, `onboarding_plans`, `onboarding_steps`, `blockers`).

**MCP Tools (5):**

| Tool | Description |
|---|---|
| `createOnboardingPlan(developerName, role)` | Creates plan + steps from role template; returns Plan ID |
| `getOnboardingProgress(planId)` | Completed/total counts, remaining steps, open blockers |
| `updateOnboardingStep(planId, stepNumber, completed)` | Mark step done or reopen |
| `reportBlocker(planId, stepNumber, description)` | Add a blocker; returns Blocker ID |
| `resolveBlocker(blockerId)` | Mark a blocker resolved |

Supported roles: `BACKEND_ENGINEER`, `FRONTEND_ENGINEER`, `PRODUCT_MANAGER` (10 steps each, seeded by Flyway V2 migration).

---

### 5. PostgreSQL (port 5432 — onboarding_db)

Stores onboarding plans, steps, and blockers for `onboarding-mcp-server`.

### 6. PostgreSQL + pgvector (port 5433 — rag_db)

Stores document metadata (`documents` table) and embeddings (`vector_store` table with `VECTOR(768)` column and HNSW index) for `rag-service`.
