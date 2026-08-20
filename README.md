# Intelligent Developer Onboarding Agent

An AI-powered platform that helps new engineers get up to speed faster using a conversational agent backed by an LLM, a knowledge base, and an onboarding progress tracker.

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 26 |
| Framework | Spring Boot | 4.0.7 |
| AI / LLM | Spring AI | 2.0.0 |
| LLM runtime | Ollama (gemma4 model) | latest |
| API docs | SpringDoc OpenAPI 3 | 3.0.2 |
| Code generation | Lombok | 1.18.x (via Spring Boot BOM) |
| Build tool | Maven Wrapper | 3.9.x |
| Test framework | JUnit Jupiter | 6.0.x |
| Mocking | Mockito | 5.x |

### Modules

| Module | Port | Role |
|---|---|---|
| `onboarding-agent-service` | 8080 | MCP host — LLM orchestrator, primary API entry point |
| `knowledge-mcp-server` | 8081 | MCP server — knowledge base tools |
| `onboarding-mcp-server` | 8082 | MCP server — onboarding workflow tools (PostgreSQL) |
| `rag-service` | 8083 | RAG pipeline — vector search and embeddings |

## Prerequisites

- Java 26+ (`java -version`)
- Maven Wrapper included — no separate Maven installation needed
- [Ollama](https://ollama.com) running locally with the required models pulled:

```bash
ollama pull gemma4           # LLM used by the agent
ollama pull nomic-embed-text # embedding model used by rag-service
ollama serve                 # if not already running as a system service
```

## Build the Project

Build all modules from the repository root:

```bash
# Compile and package (skip tests)
./mvnw package -DskipTests

# Compile, package, and run all tests
./mvnw package
```

Build a specific module only:

```bash
./mvnw -pl onboarding-agent-service package -DskipTests
```

## Run Tests

```bash
# Run tests for all modules
./mvnw test

# Run tests for the onboarding-agent-service only
./mvnw -pl onboarding-agent-service test
```

## Run the Onboarding Agent Service

The agent service connects to both MCP servers at startup (fail-fast — both must be running). Start all dependencies in order:

```bash
# 1. Start both PostgreSQL instances
docker-compose up postgres pgvector -d

# 2. Start the onboarding MCP server (port 8082)
./mvnw -pl onboarding-mcp-server spring-boot:run &

# 3. Start rag-service (port 8083) — Ollama + nomic-embed-text must be running
./mvnw -pl rag-service spring-boot:run &

# 4. Start the knowledge MCP server (port 8081) — rag-service must be running
./mvnw -pl knowledge-mcp-server spring-boot:run &

# 5. Start the agent (port 8080) — Ollama + gemma4 must be running
./mvnw -pl onboarding-agent-service spring-boot:run
```

The agent service starts on **port 8080**.

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — interactive API explorer |
| `http://localhost:8080/api-docs` | Raw OpenAPI 3 JSON spec |

### Example API call

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I am Alice, a new Backend Engineer. Can you create my onboarding plan?"
  }'
```

Response:

```json
{
  "sessionId": "3f8a2b1c-...",
  "reply": "I've created your onboarding plan, Alice! Your Plan ID is e7d2f9a1-... — keep it handy to check progress. Here are your 10 steps for BACKEND_ENGINEER: ..."
}
```

To continue the conversation in the same session, include the returned `sessionId` in subsequent requests:

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "3f8a2b1c-...",
    "message": "What is my progress so far?"
  }'
```

## Observability — State Machine & Session-correlated Logging

Every chat session is tracked by two orthogonal state machines and every log line carries the `sessionId` via SLF4J MDC so you can grep a session's full activity in one step.

### Agent Processing State (resets after each request)

```
IDLE → PROCESSING → CALLING_TOOL → PROCESSING → RESPONDING → DONE
                         ↑__________↑  (one cycle per tool call)
ANY  → ERROR  (forced from any state; resets to IDLE in finally)
```

### Onboarding Workflow State (persists for the session lifetime)

| Tool the agent calls | State transition |
|---|---|
| `createOnboardingPlan` | any → `PLAN_CREATED` |
| `updateOnboardingStep` | → `IN_PROGRESS` |
| `reportBlocker` | → `BLOCKED` |
| `resolveBlocker` | → `IN_PROGRESS` |

### Sample log output

```
14:32:01.001 INFO  [abc-123] AgentService          - Received message for session
14:32:01.002 INFO  [abc-123] AgentStateMachine      - [agentState: IDLE->PROCESSING]
14:32:01.050 INFO  [abc-123] StateAwareToolCallback - Calling tool: createOnboardingPlan | input=...
14:32:01.051 INFO  [abc-123] OnboardingStateMachine - [onboardingState: NOT_STARTED->PLAN_CREATED]
14:32:01.052 INFO  [abc-123] AgentStateMachine      - [agentState: CALLING_TOOL->PROCESSING]
14:32:01.075 INFO  [abc-123] AgentStateMachine      - [agentState: PROCESSING->RESPONDING]
14:32:01.076 INFO  [abc-123] AgentStateMachine      - [agentState: RESPONDING->DONE]
14:32:01.077 INFO  [abc-123] AgentService          - Response generated | onboardingState=PLAN_CREATED
14:32:01.077 INFO  [abc-123] AgentStateMachine      - [agentState: DONE->IDLE (reset)]
```

The `[abc-123]` field is the `sessionId` — grep it to see everything that happened in one conversation across all log lines.

---

## Architecture

```
Swagger UI (primary user interface)
    |
    v
onboarding-agent-service  (MCP host, port 8080)
  ├── State machine per session (AgentProcessingState + OnboardingWorkflowState)
  ├── MDC session-correlated logging (sessionId in every log line)
  ├── StateAwareToolCallback (wraps each MCP tool call)
    |                     |
MCP Client            MCP Client
    |                     |
knowledge-mcp-server  onboarding-mcp-server
(port 8081)           (port 8082, PostgreSQL :5432)
    |
rag-service (port 8083)
    |
PGVector (PostgreSQL :5433 + pgvector extension)
```

> All four modules are fully implemented. `knowledge-mcp-server` calls `rag-service` over HTTP (`RestClient`) to perform semantic vector search. Both MCP servers connect to `onboarding-agent-service` via Streamable HTTP (`POST /mcp`).


## RAG Service & Vector Database

The `rag-service` (port 8083) handles document ingestion and semantic search. It connects to a **PostgreSQL** instance on port 5433 with the **pgvector** extension — not a separate database, just a Postgres plugin that adds vector column types and similarity search operators.

### Why pgvector instead of plain SQL?

Normal SQL finds exact or pattern matches (`WHERE content LIKE '%auth%'`). Vector search finds *semantically similar* content — so a query for "how do I log in" can match a document about "authentication flow" even if those words don't appear in it.

### Database (port 5433)

| Detail | Value |
|---|---|
| Host | `localhost:5433` |
| Database | `rag_db` |
| User / Password | `rag` / `rag` |

### Schema ([V1__create_schema.sql](rag-service/src/main/resources/db/migration/V1__create_schema.sql))

Two tables, managed by Flyway:

**`documents`** — plain metadata table, nothing vector-specific:

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR PK | Document identifier |
| `title`, `content` | VARCHAR / TEXT | Human-readable content |
| `category`, `tags`, `source` | VARCHAR / TEXT | Filtering metadata |
| `last_updated` | DATE | |

**`vector_store`** — Spring AI's managed table for embeddings:

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | Auto-generated |
| `content` | TEXT | The text chunk that was embedded |
| `metadata` | JSON | Arbitrary key-value pairs |
| `embedding` | VECTOR(768) | 768 floating-point numbers representing the meaning of `content` |

The `VECTOR(768)` column is what pgvector adds. Think of it as a coordinate in 768-dimensional space — texts with similar meaning end up at nearby coordinates.

### The HNSW index

```sql
CREATE INDEX vector_store_embedding_idx
    ON vector_store USING HNSW (embedding vector_cosine_ops);
```

This is the vector equivalent of a B-tree index. Instead of speeding up exact lookups, it speeds up **nearest-neighbor searches** — finding rows whose embedding is closest to a query embedding. HNSW (Hierarchical Navigable Small World) is the algorithm; `vector_cosine_ops` means distance is measured by cosine similarity (angle between vectors), which works well for text.

### Query flow

1. User asks a question
2. `rag-service` sends the question to Ollama (`nomic-embed-text` model) and gets back a `VECTOR(768)`
3. pgvector finds the closest stored embeddings: `ORDER BY embedding <=> $1 LIMIT 5` (the `<=>` operator is "cosine distance")
4. The matched `content` chunks are returned to the caller to use as context for the LLM answer

### Start the RAG service

```bash
# Start the dedicated PostgreSQL + pgvector instance for rag-service (port 5433)
docker-compose up pgvector -d

# Run the service — Ollama with nomic-embed-text must be running
./mvnw -pl rag-service spring-boot:run
```

## MCP Inspector

Use the [MCP Inspector](https://github.com/modelcontextprotocol/inspector) to browse and test the tools exposed by either MCP server.

### Launch the inspector

```bash
npx @modelcontextprotocol/inspector
```

Open the inspector UI at `http://localhost:6274`.

Both servers use the **Streamable HTTP** transport, enabled via `spring.ai.mcp.server.protocol: STREAMABLE` in each server's `application.yaml`.

### Connect to onboarding-mcp-server

| Field | Value |
|---|---|
| Transport | `Streamable HTTP` |
| URL | `http://localhost:8082/mcp` |

> Exposes 5 tools: `createOnboardingPlan`, `getOnboardingProgress`, `updateOnboardingStep`, `reportBlocker`, `resolveBlocker`. Requires PostgreSQL to be running.

```bash
docker-compose up postgres -d
./mvnw -pl onboarding-mcp-server spring-boot:run
```

### Connect to knowledge-mcp-server

| Field | Value |
|---|---|
| Transport | `Streamable HTTP` |
| URL | `http://localhost:8081/mcp` |

> Exposes 4 tools: `searchDocuments`, `getDocument`, `listDocuments`, `searchByCategory`. Also exposes MCP Resources at `knowledge://{category}/{documentId}`. Requires `rag-service` to be running (calls it at startup to register resources).

```bash
docker-compose up pgvector -d
./mvnw -pl rag-service spring-boot:run &
./mvnw -pl knowledge-mcp-server spring-boot:run
```