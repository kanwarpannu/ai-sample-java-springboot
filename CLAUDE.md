# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

Multi-module Maven project. Parent artifact: `developer-onboarding-platform`.

| Module | Port | Role | Status |
|---|---|---|---|
| `onboarding-agent-service` | 8080 | MCP host / orchestrator, Swagger UI entry point | Implemented |
| `knowledge-mcp-server` | 8081 | MCP server — knowledge base tools | Implemented |
| `onboarding-mcp-server` | 8082 | MCP server — onboarding workflow tools (PostgreSQL) | Implemented |
| `rag-service` | 8083 | RAG pipeline — vector search and embeddings | Implemented |

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
./mvnw -pl knowledge-mcp-server test
./mvnw -pl onboarding-mcp-server test
./mvnw -pl rag-service test

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

## Prerequisites for rag-service and knowledge-mcp-server

`knowledge-mcp-server` calls `rag-service` over HTTP. Start both dependencies before running either:

```bash
# 1. Start pgvector (PostgreSQL + pgvector extension) on port 5433
docker-compose up pgvector -d

# 2. Pull the embedding model into Ollama (one-time)
ollama pull nomic-embed-text

# 3. Start rag-service (port 8083) — Ollama must be running
./mvnw -pl rag-service spring-boot:run

# 4. Start knowledge-mcp-server (port 8081) — rag-service must be running
./mvnw -pl knowledge-mcp-server spring-boot:run
```

- `rag-service`: Host `localhost:5433`, database `rag_db`, user/password `rag` / `rag`
- `knowledge-mcp-server` calls `rag-service` at startup to register MCP Resources — fails fast if rag-service is unreachable
- MCP Inspector: connect to `http://localhost:8081/mcp` (Streamable HTTP)

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
- **MCP client**: `spring-ai-starter-mcp-client` is enabled (`spring.ai.mcp.client.enabled: true`). It connects to both `onboarding-mcp-server` (`http://localhost:8082/mcp`) and `knowledge-mcp-server` (`http://localhost:8081/mcp`) via Streamable HTTP. A single `SyncMcpToolCallbackProvider` bean is auto-created and aggregates all tools from both connections; it is injected directly into `AgentConfiguration` to build the `ChatClient`.
- **MCP servers**: Both `onboarding-mcp-server` and `knowledge-mcp-server` use `spring-ai-starter-mcp-server-webmvc`. In Spring AI 2.0, the default transport is **Streamable HTTP** (`spring.ai.mcp.server.protocol=streamable`) at endpoint `POST /mcp`. SSE (`/sse`) is only active when `protocol: SSE` is set explicitly.
- **RAG**: `rag-service` (port 8083) is a plain REST service backed by PGVector. `knowledge-mcp-server` calls it via `HttpRagServiceClient` using Spring's `RestClient`. Base URL configured via `rag-service.base-url` in `application.yaml`.
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
| `config` | `AgentConfiguration` | `ChatClient` bean — injects `SyncMcpToolCallbackProvider` (aggregates all tools from both MCP servers) |

Conversation flow: each request adds a `UserMessage` to session history → calls `ChatClient` with the full history → appends `AssistantMessage` with the reply → returns the reply.

## knowledge-mcp-server Implementation

MCP server on port 8081. Exposes 4 tools and MCP Resources via Streamable HTTP transport (`POST /mcp`). Delegates all data access to `rag-service` over HTTP via `HttpRagServiceClient`. Requires `rag-service` to be running at startup (fail-fast).

### MCP Tools (4 tools)

| Tool method | HTTP call to rag-service | Description |
|---|---|---|
| `searchDocuments(keyword)` | `POST /api/v1/search {query: keyword}` | Semantic search across all documents; returns list with ID, title, category, tags, 200-char snippet |
| `getDocument(documentId)` | `GET /api/v1/documents/{id}` | Retrieve full content of a document by its ID |
| `listDocuments(category)` | `GET /api/v1/documents[?category=…]` | List all documents, optionally filtered by category. Accepts `null` for all. |
| `searchByCategory(category, keyword)` | `POST /api/v1/search {query: keyword, category: category}` | Semantic search scoped to a specific category |

### MCP Resources

Each document is also exposed as an MCP Resource with URI scheme `knowledge://{category}/{documentId}` (MIME type `text/plain`). Resources are registered at startup by calling `listDocuments(null)` — this is the call that fails fast if `rag-service` is unreachable.

### Package layout under `com.example.knowledge.mcp`

| Package | Class | Role |
|---|---|---|
| `client` | `RagServiceClient` (interface) | Defines 4 query methods; return `Document` domain objects |
| `client` | `HttpRagServiceClient` | `@Component` — `RestClient`-based HTTP implementation |
| `client/dto` | `RagSearchRequest`, `RagSearchResult`, `RagSearchResponse`, `RagDocumentResponse` | Wire-format records for rag-service JSON; private to the `client` package |
| `domain` | `Document` | Java record: `id, title, content, category, tags, source, lastUpdated` |
| `service` | `KnowledgeService` | Delegates to `RagServiceClient`; formats results as `String` for the LLM |
| `tool` | `KnowledgeMcpTools` | `@Tool`-annotated methods delegating to `KnowledgeService` |
| `config` | `McpConfiguration` | Registers `ToolCallbackProvider` + `List<SyncResourceSpecification>` beans |
| `config` | `RagClientConfiguration` | `@Bean RestClient ragServiceRestClient` — built via `RestClient.builder().baseUrl(...)` |

### Test structure (21 tests)

- **Service test** (`@ExtendWith(MockitoExtension.class)`): `KnowledgeServiceTest` — 10 tests covering all operations, empty results, and content truncation
- **Tool test** (`@ExtendWith(MockitoExtension.class)`): `KnowledgeMcpToolsTest` — 4 tests (one per tool, delegation checks)
- **HTTP client test** (no Spring context): `HttpRagServiceClientTest` — 7 tests using `MockRestServiceServer.createServer(RestClient.Builder)` to intercept HTTP calls
- Test YAML disables the MCP server (`spring.ai.mcp.server.enabled: false`) so tests don't need a servlet container.

## rag-service Implementation

REST service on port 8083. Handles document ingestion and semantic search. Backed by PostgreSQL with `pgvector` extension (`rag_db` on port 5433). Embeddings via Ollama (`nomic-embed-text`, 768 dimensions).

### REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/documents` | Ingest a document (JSON body) |
| `POST` | `/api/v1/documents/upload` | Ingest a plain-text file (multipart) |
| `GET` | `/api/v1/documents` | List documents, optional `?category=` filter |
| `GET` | `/api/v1/documents/{id}` | Fetch a single document by ID (full content) |
| `POST` | `/api/v1/search` | Semantic search — body: `{query, category?, keyword?}` |

Search: `query` drives vector similarity (top 5, threshold 0.5), `category` is a metadata filter, `keyword` is a substring post-filter applied in Java after vector results are returned.

### Package layout under `com.example.rag`

| Package | Class | Role |
|---|---|---|
| `controller` | `DocumentController`, `SearchController` | REST endpoints |
| `dto` | `DocumentIngestRequest`, `DocumentResponse`, `SearchRequest`, `SearchResponse`, `SearchResult` | Request/response shapes |
| `domain` | `Document` | JPA entity (`documents` table) |
| `repository` | `DocumentRepository` | Spring Data JPA — `findByCategory` |
| `service` | `RagService` | All business logic; calls `VectorStore` for embeddings |
| `config` | `RagConfiguration` | `ApplicationRunner` — re-seeds `vector_store` at startup if empty |

### Database schema (2 tables, Flyway-managed)

- `documents` — plain metadata table (id, title, content, category, tags, source, last_updated)
- `vector_store` — Spring AI managed (`id UUID`, `content TEXT`, `metadata JSON`, `embedding VECTOR(768)`) with HNSW index

### Test structure

`RagServiceTest` (12 tests, Mockito), `DocumentControllerTest` (5 tests, `@WebMvcTest`), `SearchControllerTest` (3 tests, `@WebMvcTest`). Uses `com.fasterxml.jackson.core:jackson-databind:2.21` (Jackson 2.x compat layer) — this module has both Jackson 2 and Jackson 3 on its classpath.

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
- **Lombok** is used in `onboarding-agent-service`, `knowledge-mcp-server`, and `onboarding-mcp-server`. Because of Maven Compiler Plugin 3.14 + Java 26, Lombok **must** be declared in `<annotationProcessorPaths>` in the module's pom.xml — adding it only as a regular dependency is not sufficient.
- Tests use JUnit Jupiter 6.0.3 + `spring-boot-starter-webmvc-test` (MockMvc available).
- Package roots: `com.example.agent`, `com.example.knowledge.mcp`, `com.example.onboarding.mcp`, `com.example.rag`.
- **Jackson 3**: Spring Boot 4 uses Jackson 3 (`tools.jackson.*` package namespace), not Jackson 2 (`com.fasterxml.jackson.*`). `rag-service` additionally has the Jackson 2 compatibility layer on its classpath. In `knowledge-mcp-server`, use `tools.jackson.databind.ObjectMapper` if you need an ObjectMapper directly; `LocalDate` is handled automatically (Java Time support is built into Jackson 3 databind).
- **`RestClient.Builder` is not auto-configured as a bean** in Spring Boot 4 with `spring-boot-starter-webmvc`. Use `RestClient.builder()` (static factory) directly. Injecting `RestClient.Builder` will fail with "required a bean of type … that could not be found."
- **Testing `RestClient`**: Use `MockRestServiceServer.createServer(RestClient.Builder)` (Spring 7 API) to intercept HTTP calls without starting a real server and without needing a custom `ObjectMapper`.

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
