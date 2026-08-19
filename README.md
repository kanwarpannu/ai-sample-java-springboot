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
- [Ollama](https://ollama.com) running locally with the `gemma4` model pulled:

```bash
ollama pull gemma4
ollama serve          # if not already running as a system service
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
# 1. Start PostgreSQL
docker-compose up postgres -d

# 2. Start the onboarding MCP server (port 8082)
./mvnw -pl onboarding-mcp-server spring-boot:run &

# 3. Start the knowledge MCP server (port 8081)
./mvnw -pl knowledge-mcp-server spring-boot:run &

# 4. Start the agent (port 8080) — Ollama must also be running
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

## Architecture

```
Swagger UI (primary user interface)
    |
    v
onboarding-agent-service  (MCP host, port 8080)
    |                     |
MCP Client            MCP Client
    |                     |
knowledge-mcp-server  onboarding-mcp-server
(port 8081)           (port 8082, PostgreSQL)
    |
rag-service (port 8083)
    |
ChromaDB / PGVector
```

> **Note:** Both `onboarding-mcp-server` and `knowledge-mcp-server` are fully implemented and wired to `onboarding-agent-service` via the Spring AI MCP client using Streamable HTTP (`POST /mcp`). Streamable HTTP must be explicitly enabled on each server with `protocol: STREAMABLE` — Spring AI 2.0 defaults to SSE when this property is absent. `rag-service` is not yet built — `knowledge-mcp-server` currently uses an in-memory mock document store.


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
# Start the dedicated PostgreSQL instance for rag-service
docker-compose up rag-postgres -d

# Run the service
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

> Exposes 4 tools: `searchDocuments`, `getDocument`, `listDocuments`, `searchByCategory`. Also exposes MCP Resources at `knowledge://{category}/{documentId}`. No external dependencies — starts standalone.

```bash
./mvnw -pl knowledge-mcp-server spring-boot:run
```