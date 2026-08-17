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

Start Ollama first (see Prerequisites), then:

```bash
./mvnw -pl onboarding-agent-service spring-boot:run
```

The service starts on **port 8080**.

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
  "reply": "Here is your personalised onboarding plan, Alice ..."
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

> **Note:** `knowledge-mcp-server`, `onboarding-mcp-server`, and `rag-service` are not yet implemented. The `onboarding-agent-service` uses `@Tool`-annotated stub beans to mock their responses during development.
