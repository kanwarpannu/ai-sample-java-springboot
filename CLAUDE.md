# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

Multi-module Maven project. Parent artifact: `developer-onboarding-platform`.

| Module | Port | Role |
|---|---|---|
| `onboarding-agent-service` | 8080 | MCP host / orchestrator, Swagger UI entry point |
| `knowledge-mcp-server` | 8081 | MCP server — knowledge base tools |
| `onboarding-mcp-server` | 8082 | MCP server — onboarding workflow tools (PostgreSQL) |
| `rag-service` | 8083 | RAG pipeline — vector search and embeddings |

## Commands

```bash
# Build all modules from root
./mvnw package -DskipTests

# Build and test all modules from root
./mvnw package

# Run a specific module
./mvnw -pl onboarding-agent-service spring-boot:run
./mvnw -pl knowledge-mcp-server spring-boot:run
./mvnw -pl onboarding-mcp-server spring-boot:run
./mvnw -pl rag-service spring-boot:run

# Test a specific module
./mvnw -pl onboarding-agent-service test

# Build OCI image for a specific module
./mvnw -pl onboarding-agent-service spring-boot:build-image
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

- **LLM**: Ollama running locally, via `spring-ai-starter-model-ollama`. Configure under `spring.ai.ollama.*` in `onboarding-agent-service/src/main/resources/application.yaml`.
- **MCP clients**: declared in `onboarding-agent-service` application.yaml under `spring.ai.mcp.client.*`.
- **MCP servers**: `knowledge-mcp-server` and `onboarding-mcp-server` use `spring-ai-starter-mcp-server-webmvc`.
- **RAG**: `rag-service` is a plain REST service; vector store dependencies added when expanding.
- **API docs**: SpringDoc OpenAPI 3 in `onboarding-agent-service` generates Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Key Technical Details

- **Java 26**, **Spring Boot 4.0.7**, **Spring AI 2.0.0** — all bleeding-edge; prefer the official Spring AI 2.x docs.
- Configuration is in `application.yaml` (not `.properties`) inside each module's `src/main/resources/`.
- No Lombok, MapStruct, or other annotation processors.
- Tests use JUnit 5 + `spring-boot-starter-webmvc-test` (MockMvc available).
- Package roots: `com.example.agent`, `com.example.knowledge.mcp`, `com.example.onboarding.mcp`, `com.example.rag`.
