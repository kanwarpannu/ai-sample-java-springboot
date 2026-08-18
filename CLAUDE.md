# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

Multi-module Maven project. Parent artifact: `developer-onboarding-platform`.

| Module | Port | Role | Status |
|---|---|---|---|
| `onboarding-agent-service` | 8080 | MCP host / orchestrator, Swagger UI entry point | Implemented |
| `knowledge-mcp-server` | 8081 | MCP server — knowledge base tools | Not yet built |
| `onboarding-mcp-server` | 8082 | MCP server — onboarding workflow tools (PostgreSQL) | Implemented |
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
./mvnw -pl onboarding-mcp-server test

# Build OCI image for a specific module
./mvnw -pl onboarding-agent-service spring-boot:build-image
```

## Prerequisites for onboarding-mcp-server

Start the PostgreSQL container before running or testing the server:

```bash
docker-compose up postgres -d
```

- Host: `localhost:5432`, database: `onboarding_db`, user/password: `onboarding`
- Flyway runs migrations automatically on startup (schema + seed data)
- MCP Inspector: connect to `http://localhost:8082/mcp` (Streamable HTTP — Spring AI 2.0 default)

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
- **MCP client**: `spring-ai-starter-mcp-client` is enabled (`spring.ai.mcp.client.enabled: true`). It connects to `onboarding-mcp-server` via Streamable HTTP (`http://localhost:8082/mcp`). A `SyncMcpToolCallbackProvider` is auto-created and injected into `AgentConfiguration`. `KnowledgeMcpTools` (`searchDocuments`) remains a local stub until `knowledge-mcp-server` is built.
- **MCP servers**: `onboarding-mcp-server` uses `spring-ai-starter-mcp-server-webmvc`. In Spring AI 2.0, the default transport is **Streamable HTTP** (`spring.ai.mcp.server.protocol=streamable`) at endpoint `POST /mcp`. SSE (`/sse`) is only active when `protocol: SSE` is set explicitly. `knowledge-mcp-server` is not yet built.
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
| `mcp` | `KnowledgeMcpTools` | `@Tool searchDocuments()` stub (until knowledge-mcp-server is built) |
| `config` | `AgentConfiguration` | `ChatClient` bean wiring `KnowledgeMcpTools` + `SyncMcpToolCallbackProvider` (MCP tools) |

Conversation flow: each request adds a `UserMessage` to session history → calls `ChatClient` with the full history → appends `AssistantMessage` with the reply → returns the reply.

## onboarding-mcp-server Implementation

MCP server on port 8082. Exposes 5 tools via Streamable HTTP transport (`POST /mcp`). Backed by PostgreSQL (`onboarding_db`). Schema is managed by Flyway.

### Database schema (4 tables)

| Table | Purpose |
|---|---|
| `step_templates` | Predefined steps per role, seeded at startup |
| `onboarding_plans` | One plan per developer (UUID primary key) |
| `onboarding_steps` | Step instances within a plan, copied from templates |
| `blockers` | Blockers on individual steps |

Seed data (V2 migration): 10 steps each for `BACKEND_ENGINEER`, `FRONTEND_ENGINEER`, `PRODUCT_MANAGER`.

### MCP Tools (5 tools)

| Tool method | Description |
|---|---|
| `createOnboardingPlan(developerName, role)` | Creates plan + steps from role template; returns Plan ID |
| `getOnboardingProgress(planId)` | Returns completed/total counts, remaining steps, open blockers |
| `updateOnboardingStep(planId, stepNumber, completed)` | Marks a step COMPLETED or REOPENED |
| `reportBlocker(planId, stepNumber, description)` | Adds a blocker to a step; returns Blocker ID |
| `resolveBlocker(blockerId)` | Marks a blocker resolved |

### Package layout under `com.example.onboarding.mcp`

| Package | Class | Role |
|---|---|---|
| `domain` | `OnboardingPlan`, `OnboardingStep`, `Blocker`, `StepTemplate` | JPA entities (Lombok `@Getter @Setter @Builder`) |
| `repository` | `OnboardingPlanRepository`, `OnboardingStepRepository`, `BlockerRepository`, `StepTemplateRepository` | Spring Data JPA repositories |
| `service` | `OnboardingService` | Business logic; all 5 tool operations |
| `tool` | `OnboardingMcpTools` | `@Tool`-annotated methods delegating to `OnboardingService` |
| `config` | `McpConfiguration` | Registers `OnboardingMcpTools` as `ToolCallbackProvider` |

### Test structure (31 tests)

- **Repository tests** (`@DataJpaTest` + H2 in-memory, Flyway disabled): `OnboardingPlanRepositoryTest`, `OnboardingStepRepositoryTest`, `BlockerRepositoryTest`
- **Service test** (`@ExtendWith(MockitoExtension.class)`): `OnboardingServiceTest` — 13 tests covering all operations and error paths
- **Tool test** (`@ExtendWith(MockitoExtension.class)`): `OnboardingMcpToolsTest` — 5 tests, one per tool

## Key Technical Details

- **Java 26**, **Spring Boot 4.0.7**, **Spring AI 2.0.0** — all bleeding-edge; prefer the official Spring AI 2.x docs.
- Configuration is in `application.yaml` (not `.properties`) inside each module's `src/main/resources/`.
- **Lombok** is used in `onboarding-agent-service` and `onboarding-mcp-server`. Because of Maven Compiler Plugin 3.14 + Java 26, Lombok **must** be declared in `<annotationProcessorPaths>` in the module's pom.xml — adding it only as a regular dependency is not sufficient.
- Tests use JUnit Jupiter 6.0.3 + `spring-boot-starter-webmvc-test` (MockMvc available).
- Package roots: `com.example.agent`, `com.example.knowledge.mcp`, `com.example.onboarding.mcp`, `com.example.rag`.

## Spring Boot 4.x Breaking Changes (vs 3.x)

These caught us during development — apply to all modules:

| Area | Spring Boot 3.x import | Spring Boot 4.x import |
|---|---|---|
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `@DataJpaTest` | `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `@MockBean` (deprecated) | `org.springframework.boot.test.mock.mockito.MockBean` | Use `@MockitoBean` instead |
| `@MockitoBean` | n/a | `org.springframework.test.context.bean.override.mockito.MockitoBean` |
| JUnit Jupiter | 5.x | 6.0.3 (same API, different version) |

`ObjectMapper` is not auto-configured in `@WebMvcTest` slices — instantiate it directly in tests: `new ObjectMapper()`.

`@DataJpaTest` requires the `spring-boot-starter-data-jpa-test` dependency (a Spring Boot 4.x technology-specific test starter — not included in `spring-boot-starter-data-jpa`). In tests, disable Flyway and use `ddl-auto: create-drop` with H2 via `src/test/resources/application.yaml`.

Flyway auto-configuration is **not** triggered by adding `flyway-core` alone. In Spring Boot 4.x, Flyway support was extracted to its own module. Use `spring-boot-starter-flyway` (provides the auto-configuration) **plus** `flyway-database-postgresql` (provides the PostgreSQL dialect for Flyway 11.x). Without the starter, Flyway silently never runs and Hibernate schema validation fails with `missing table` errors.
