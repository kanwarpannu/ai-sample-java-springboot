# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

Multi-module Maven project. Parent artifact: `developer-onboarding-platform`.

| Module | Port | Role | Status |
|---|---|---|---|
| `onboarding-agent-service` | 8080 | MCP host / orchestrator, Swagger UI entry point | Implemented |
| `knowledge-mcp-server` | 8081 | MCP server — knowledge base tools | Not yet built |
| `onboarding-mcp-server` | 8082 | MCP server — onboarding workflow tools (PostgreSQL) | Not yet built |
| `rag-service` | 8083 | RAG pipeline — vector search and embeddings | Not yet built |

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

- **LLM**: Ollama running locally with `gemma4` model, via `spring-ai-starter-model-ollama`. Configure under `spring.ai.ollama.*` in `onboarding-agent-service/src/main/resources/application.yaml`.
- **MCP client**: `spring-ai-starter-mcp-client` is present but disabled (`spring.ai.mcp.client.enabled: false`). MCP tool calls are currently mocked via `@Tool`-annotated Spring beans in the `mcp/` package.
- **MCP servers**: `knowledge-mcp-server` and `onboarding-mcp-server` will use `spring-ai-starter-mcp-server-webmvc` when implemented.
- **RAG**: `rag-service` is a plain REST service; vector store dependencies added when expanding.
- **API docs**: SpringDoc OpenAPI 3 in `onboarding-agent-service` generates Swagger UI at `http://localhost:8080/swagger-ui.html`.

## onboarding-agent-service Implementation

REST endpoint: `POST /api/v1/chat` — accepts `{sessionId?, message}`, returns `{sessionId, reply}`.

Package layout under `com.example.agent`:

| Package | Class | Role |
|---|---|---|
| `controller` | `ChatController` | REST endpoint with Swagger annotations |
| `dto` | `ChatRequest`, `ChatResponse` | Lombok `@Data` DTOs |
| `service` | `AgentService` | Orchestrates ChatClient + conversation history |
| `service` | `ConversationStore` | In-memory `ConcurrentHashMap<sessionId, List<Message>>` |
| `mcp` | `KnowledgeMcpTools` | `@Tool searchDocuments()` stub |
| `mcp` | `OnboardingMcpTools` | `@Tool` stubs: createOnboardingPlan, getOnboardingProgress, updateOnboardingStep |
| `config` | `AgentConfiguration` | `ChatClient` bean with system prompt and default tools |

Conversation flow: each request adds a `UserMessage` to session history → calls `ChatClient` with the full history → appends `AssistantMessage` with the reply → returns the reply.

## Key Technical Details

- **Java 26**, **Spring Boot 4.0.7**, **Spring AI 2.0.0** — all bleeding-edge; prefer the official Spring AI 2.x docs.
- Configuration is in `application.yaml` (not `.properties`) inside each module's `src/main/resources/`.
- **Lombok** is used in `onboarding-agent-service`. Because of Maven Compiler Plugin 3.14 + Java 26, Lombok **must** be declared in `<annotationProcessorPaths>` in the module's pom.xml — adding it only as a regular dependency is not sufficient.
- Tests use JUnit Jupiter 6.0.3 + `spring-boot-starter-webmvc-test` (MockMvc available).
- Package roots: `com.example.agent`, `com.example.knowledge.mcp`, `com.example.onboarding.mcp`, `com.example.rag`.

## Spring Boot 4.x Breaking Changes (vs 3.x)

These caught us during development — apply to all modules:

| Area | Spring Boot 3.x import | Spring Boot 4.x import |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `@MockBean` (deprecated) | `org.springframework.boot.test.mock.mockito.MockBean` | Use `@MockitoBean` instead |
| `@MockitoBean` | n/a | `org.springframework.test.context.bean.override.mockito.MockitoBean` |
| JUnit Jupiter | 5.x | 6.0.3 (same API, different version) |

`ObjectMapper` is not auto-configured in `@WebMvcTest` slices — instantiate it directly in tests: `new ObjectMapper()`.
