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

The agent service connects to `onboarding-mcp-server` at startup. Start all dependencies in order:

```bash
# 1. Start PostgreSQL
docker-compose up postgres -d

# 2. Start the MCP server (port 8082)
./mvnw -pl onboarding-mcp-server spring-boot:run &

# 3. Start the agent (port 8080) — Ollama must also be running
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

> **Note:** `onboarding-mcp-server` is fully implemented and wired to `onboarding-agent-service` via the Spring AI MCP client using Streamable HTTP (`POST /mcp`). Streamable HTTP must be explicitly enabled on the server with `protocol: STREAMABLE` — Spring AI 2.0 defaults to SSE when this property is absent. `knowledge-mcp-server` and `rag-service` are not yet built — `searchDocuments` is still served by a local stub bean in the agent service.


## MCP Inspector

Use the [MCP Inspector](https://github.com/modelcontextprotocol/inspector) to browse and test the tools exposed by `onboarding-mcp-server`.

### Launch the inspector

```bash
npx @modelcontextprotocol/inspector
```

Open the inspector UI at `http://localhost:6274`.

### Connect to onboarding-mcp-server

`onboarding-mcp-server` uses the **Streamable HTTP** transport, enabled explicitly via `spring.ai.mcp.server.protocol: STREAMABLE` in its `application.yaml` (Spring AI 2.0 defaults to SSE if this is not set).

| Field | Value |
|---|---|
| Transport | `Streamable HTTP` |
| URL | `http://localhost:8082/mcp` |

> **Note:** Start PostgreSQL and `onboarding-mcp-server` before connecting (see [Run the Onboarding Agent Service](#run-the-onboarding-agent-service)).

```bash
docker-compose up postgres -d
./mvnw -pl onboarding-mcp-server spring-boot:run
```