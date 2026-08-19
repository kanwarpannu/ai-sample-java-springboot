INSERT INTO documents (id, title, content, category, tags, source, last_updated) VALUES

('onboarding-platform-setup',
 'Developer Onboarding Platform Setup Guide',
 $DOC$This guide covers the end-to-end setup of the Developer Onboarding Platform.

Prerequisites:
- Java 26 (via SDKMAN or Homebrew)
- Maven 3.9+
- Docker Desktop
- IntelliJ IDEA or VS Code
- Ollama with gemma4 model

Steps:
1. Clone the repository: git clone <repo-url>
2. Start infrastructure: docker-compose up postgres -d
3. Start Ollama: ollama run gemma4
4. Build all modules: ./mvnw package -DskipTests
5. Run onboarding-mcp-server: ./mvnw -pl onboarding-mcp-server spring-boot:run
6. Run onboarding-agent-service: ./mvnw -pl onboarding-agent-service spring-boot:run
7. Access Swagger UI at: http://localhost:8080/swagger-ui.html

Ports:
- 8080: Onboarding Agent Service (main entry point)
- 8081: Knowledge MCP Server
- 8082: Onboarding MCP Server
- 8083: RAG Service
- 5432: PostgreSQL (onboarding_db)
- 5433: PGVector (rag_db)$DOC$,
 'setup-guide',
 'setup,onboarding,platform,docker,java,maven',
 'https://github.com/example/developer-onboarding-platform/wiki/setup',
 '2026-08-01'),

('local-dev-environment',
 'Local Development Environment Setup',
 $DOC$This guide explains how to configure your local machine for development on this project.

Required Tools:
- Java 26: Install via SDKMAN (sdk install java 26-open) or Homebrew
- Maven: Bundled via Maven Wrapper (./mvnw) — no install needed
- Docker: Docker Desktop for Mac/Windows, or Docker Engine on Linux
- Ollama: Download from https://ollama.ai and run 'ollama pull gemma4'
- IDE: IntelliJ IDEA recommended; install Lombok plugin

Environment Variables:
- No mandatory env vars for local dev
- Optional: JAVA_HOME, MAVEN_OPTS=-Xmx2g

Recommended IDE settings:
- Enable annotation processing for Lombok
- Set Java SDK to version 26
- Import project as Maven project

Common Issues:
- 'Compilation error: cannot find symbol @Data' -> Lombok annotation processing not enabled
- 'Connection refused: 5432' -> PostgreSQL container not running; run docker-compose up postgres -d
- 'Model not found: gemma4' -> Run: ollama pull gemma4$DOC$,
 'setup-guide',
 'setup,local,development,java,docker,ollama,ide,lombok',
 'https://github.com/example/developer-onboarding-platform/wiki/local-dev',
 '2026-07-15'),

('deploy-onboarding-service',
 'Deployment Runbook: Onboarding Agent Service',
 $DOC$Runbook for deploying the Onboarding Agent Service (port 8080) to production.

Pre-deployment Checklist:
- All tests pass: ./mvnw -pl onboarding-agent-service test
- Docker image built: ./mvnw -pl onboarding-agent-service spring-boot:build-image
- Environment variables configured in deployment target
- Downstream services (onboarding-mcp-server, knowledge-mcp-server) deployed
- Ollama instance accessible at configured URL

Deployment Steps:
1. Build OCI image: ./mvnw -pl onboarding-agent-service spring-boot:build-image
2. Tag image: docker tag onboarding-agent-service:0.0.1-SNAPSHOT <registry>/onboarding-agent-service:latest
3. Push image: docker push <registry>/onboarding-agent-service:latest
4. Deploy to target environment
5. Verify health: curl http://<host>:8080/actuator/health

Key Configuration (application.yaml):
- spring.ai.ollama.base-url: URL of Ollama instance
- spring.ai.mcp.client.streamable-http.connections: MCP server endpoints

Rollback:
- Re-deploy previous image tag
- Verify health endpoint before closing old instances$DOC$,
 'runbook',
 'deployment,runbook,docker,production,onboarding,agent',
 'https://github.com/example/developer-onboarding-platform/wiki/deploy-agent',
 '2026-08-10'),

('postgres-maintenance',
 'PostgreSQL Maintenance Runbook',
 $DOC$Runbook for routine PostgreSQL maintenance operations for the onboarding_db database.

Connection Details (local):
- Host: localhost:5432
- Database: onboarding_db
- Username/Password: onboarding/onboarding
- Container: docker exec -it <postgres-container> psql -U onboarding -d onboarding_db

Common Operations:

Check table sizes:
  SELECT relname, pg_size_pretty(pg_total_relation_size(oid)) FROM pg_class WHERE relkind='r' ORDER BY pg_total_relation_size(oid) DESC;

View active connections:
  SELECT pid, usename, application_name, state FROM pg_stat_activity WHERE datname='onboarding_db';

Vacuum tables (reclaim space after deletions):
  VACUUM ANALYZE onboarding_plans;
  VACUUM ANALYZE onboarding_steps;

Flyway migration status:
  SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;

Reset local database (destructive - dev only):
  docker-compose down -v && docker-compose up postgres -d

Backup (pg_dump):
  docker exec <container> pg_dump -U onboarding onboarding_db > backup.sql$DOC$,
 'runbook',
 'postgresql,database,maintenance,runbook,flyway,backup,docker',
 'https://github.com/example/developer-onboarding-platform/wiki/postgres-ops',
 '2026-06-20'),

('mcp-protocol-faq',
 'FAQ: Model Context Protocol (MCP)',
 $DOC$Frequently asked questions about the Model Context Protocol used in this platform.

Q: What is MCP?
A: MCP (Model Context Protocol) is an open standard that allows LLMs to interact with external tools and data sources in a structured way. It defines how an AI host (like our onboarding-agent-service) discovers and calls tools exposed by MCP servers.

Q: What transport does this project use?
A: Streamable HTTP (the Spring AI 2.0 default). The MCP endpoint is POST /mcp. For onboarding-mcp-server: http://localhost:8082/mcp. For knowledge-mcp-server: http://localhost:8081/mcp.

Q: How do I test MCP tools without the full agent?
A: Use MCP Inspector - connect it to http://localhost:8082/mcp or http://localhost:8081/mcp. You can call tools directly and inspect their responses.

Q: What is the difference between MCP Tools and MCP Resources?
A: Tools are callable functions the LLM invokes to perform actions. Resources are read-only content identified by URI (e.g., knowledge://setup-guide/onboarding-platform-setup) that clients can fetch directly.

Q: How are tools registered in Spring AI 2.0?
A: Annotate methods with @Tool, wrap the class in a MethodToolCallbackProvider bean, and declare it as a ToolCallbackProvider @Bean. Spring AI auto-configuration converts this to MCP tool registrations.

Q: Can I add new tools without restarting the agent?
A: No - MCP tools are registered at server startup and advertised to the agent when it initialises its MCP client connections.$DOC$,
 'faq',
 'mcp,protocol,tools,resources,spring-ai,streamable-http,faq',
 'https://github.com/example/developer-onboarding-platform/wiki/mcp-faq',
 '2026-08-05'),

('spring-boot-4-faq',
 'FAQ: Spring Boot 4 Breaking Changes',
 $DOC$Frequently asked questions about Spring Boot 4 changes encountered in this project.

Q: Test annotation imports changed - what do I use now?
A: Several test annotations moved packages:
   - @WebMvcTest: org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
   - @DataJpaTest: org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
   - @MockBean (deprecated): use @MockitoBean from org.springframework.test.context.bean.override.mockito.MockitoBean

Q: @DataJpaTest does not include my JPA test starter - why?
A: Spring Boot 4 extracted technology-specific test starters. Add spring-boot-starter-data-jpa-test to test scope in pom.xml.

Q: ObjectMapper is null in @WebMvcTest - how do I fix it?
A: ObjectMapper is not auto-configured in web slice tests. Instantiate it directly: new ObjectMapper().

Q: Flyway never runs even though I added flyway-core - why?
A: In Spring Boot 4, you need spring-boot-starter-flyway (auto-configuration) plus flyway-database-postgresql (PostgreSQL dialect). flyway-core alone is not enough.

Q: Lombok does not work with Maven Compiler Plugin 3.14 + Java 26 - how do I fix it?
A: Declare Lombok in <annotationProcessorPaths> inside the maven-compiler-plugin configuration. Adding it only as a regular dependency is not sufficient for Java 26.

Q: JUnit version changed?
A: Yes - JUnit Jupiter is now version 6.0.3. The API is the same, but the version number is higher than Spring Boot 3.x used.$DOC$,
 'faq',
 'spring-boot-4,faq,breaking-changes,test,lombok,flyway,junit',
 'https://github.com/example/developer-onboarding-platform/wiki/spring-boot-4-faq',
 '2026-07-28'),

('rest-api-standards',
 'REST API Design Standards',
 $DOC$Standards for designing REST APIs across all services in this platform.

URL Design:
- Use lowercase, hyphenated paths: /api/v1/onboarding-plans
- Version in path prefix: /api/v1/...
- Resource names are plural nouns: /plans, /steps, /blockers
- Nested resources for containment: /plans/{planId}/steps/{stepId}

HTTP Methods:
- GET: Read-only, idempotent, no body
- POST: Create new resource; return 201 Created with Location header
- PUT: Full replacement; idempotent
- PATCH: Partial update
- DELETE: Remove resource; return 204 No Content

Response Codes:
- 200 OK: Successful GET, PUT, PATCH
- 201 Created: Successful POST
- 204 No Content: Successful DELETE
- 400 Bad Request: Validation failure (include error details in body)
- 404 Not Found: Resource does not exist
- 409 Conflict: State conflict (e.g., duplicate)
- 500 Internal Server Error: Unexpected error (do not expose stack traces)

Request/Response Format:
- Content-Type: application/json
- Use camelCase for JSON field names
- Include timestamps in ISO 8601 format: 2026-08-01T10:30:00Z
- Paginate large collections using ?page=0&size=20

Documentation:
- All endpoints must have Swagger/OpenAPI annotations
- Include example request/response in Swagger$DOC$,
 'standards',
 'rest,api,standards,http,json,swagger,openapi,design',
 'https://github.com/example/developer-onboarding-platform/wiki/api-standards',
 '2026-05-10'),

('code-review-process',
 'Code Review Process and Standards',
 $DOC$Guidelines for code review on this project.

Opening a Pull Request:
- Branch from main: git checkout -b feature/my-feature
- Keep PRs focused: one logical change per PR
- Write a clear PR description explaining what changed and why
- Link to the relevant issue or task
- Ensure all tests pass and build is green before requesting review

PR Checklist (author):
- Tests added or updated for the change
- No debug code or commented-out code left in
- API changes documented in Swagger annotations
- CLAUDE.md updated if architecture changes were made

Reviewer Expectations:
- Respond within 1 business day
- Focus on correctness, maintainability, and security
- Be specific in feedback - reference file and line
- Distinguish blocking issues from suggestions (use prefixes: BLOCKING: / NIT: / SUGGESTION:)

Merging:
- Require 1 approval minimum
- Author merges after approval (squash merge preferred for feature branches)
- Delete branch after merge

Commit Message Format:
- Present tense imperative: "Add user authentication" not "Added user authentication"
- First line max 72 characters
- Include context in body if non-obvious$DOC$,
 'standards',
 'code-review,pull-request,git,standards,workflow,merge',
 'https://github.com/example/developer-onboarding-platform/wiki/code-review',
 '2026-04-22'),

('system-architecture',
 'System Architecture Overview',
 $DOC$Overview of the Developer Onboarding Platform architecture.

Services:

onboarding-agent-service (port 8080):
- The brain of the system
- Hosts the LLM integration via Ollama (gemma4 model)
- MCP host: connects to MCP servers as a client
- Exposes REST API: POST /api/v1/chat
- Swagger UI at http://localhost:8080/swagger-ui.html

knowledge-mcp-server (port 8081):
- MCP server for the knowledge base
- Exposes tools: searchDocuments, getDocument, listDocuments, searchByCategory
- Exposes MCP resources: knowledge://{category}/{documentId}
- Delegates to rag-service for semantic search

onboarding-mcp-server (port 8082):
- MCP server for onboarding workflow
- Backed by PostgreSQL (onboarding_db)
- Exposes tools: createOnboardingPlan, getOnboardingProgress, updateOnboardingStep, reportBlocker, resolveBlocker

rag-service (port 8083):
- Document ingestion and retrieval pipeline
- Creates embeddings and indexes documents
- Performs semantic vector search
- Backed by PGVector (rag_db on port 5433)

Communication:
- Agent to MCP servers: Streamable HTTP (POST /mcp)
- knowledge-mcp-server to rag-service: REST HTTP
- onboarding-mcp-server to PostgreSQL: JDBC$DOC$,
 'architecture',
 'architecture,microservices,mcp,spring-boot,llm,system-design',
 'https://github.com/example/developer-onboarding-platform/wiki/architecture',
 '2026-08-01'),

('data-flow-integration',
 'Data Flow and Integration Patterns',
 $DOC$How data flows through the Developer Onboarding Platform and how services integrate.

Chat Request Flow:
1. User sends POST /api/v1/chat {sessionId, message} to onboarding-agent-service
2. AgentService appends UserMessage to conversation history (keyed by sessionId)
3. ChatClient sends full history + system prompt to Ollama (gemma4)
4. If Ollama decides to call a tool, Spring AI dispatches the MCP tool call:
   - Knowledge tools -> knowledge-mcp-server (http://localhost:8081/mcp)
   - Onboarding tools -> onboarding-mcp-server (http://localhost:8082/mcp)
5. Tool result returned to Ollama for final response generation
6. AssistantMessage appended to history; reply returned to user

Knowledge Retrieval Flow:
1. LLM calls searchDocuments(keyword) on knowledge-mcp-server
2. KnowledgeMcpTools delegates to KnowledgeService
3. KnowledgeService calls RagServiceClient.searchDocuments(keyword)
4. (Production) RagServiceClient calls rag-service POST /api/v1/search
5. rag-service generates embedding via Ollama embedding model
6. PGVector performs cosine-similarity search and returns top-k chunks
7. Chunks returned up the chain as formatted String to the LLM

Onboarding Plan Flow:
1. LLM calls createOnboardingPlan(developerName, role)
2. onboarding-mcp-server looks up step templates for the role
3. Creates OnboardingPlan + OnboardingStep entities in PostgreSQL
4. Returns plan UUID; LLM presents plan summary to user

Session State:
- Conversation history: in-memory ConcurrentHashMap (ConversationStore)
- Onboarding state: persisted in PostgreSQL
- Knowledge base: in-memory mock (will be PGVector in production)$DOC$,
 'architecture',
 'data-flow,integration,llm,mcp,vector-search,pgvector,session',
 'https://github.com/example/developer-onboarding-platform/wiki/data-flow',
 '2026-08-01');
