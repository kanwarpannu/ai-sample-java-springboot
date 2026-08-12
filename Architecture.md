                         ┌─────────────────────┐
                         │     Swagger UI      │
                         └──────────┬──────────┘
                                    │
                                    ▼
                   ┌────────────────────────────────┐
                   │  Onboarding Agent Service      │
                   │  (Spring Boot MCP Host)        │
                   └───────┬───────────┬────────────┘
                           │           │
                    MCP Client   MCP Client
                           │           │
          ┌────────────────▼─┐     ┌──▼────────────────┐
          │ Knowledge MCP    │     │ Onboarding MCP    │
          │ Server           │     │ Server            │
          └────────┬─────────┘     └──────┬────────────┘
                   │                      │
                   ▼                      ▼
          ┌────────────────┐      ┌───────────────┐
          │  RAG Service   │      │ PostgreSQL    │
          │                │      │               │
          └───────┬────────┘      └───────────────┘
                  │
                  ▼
          ┌────────────────┐
          │ ChromaDB /     │
          │ PGVector       │
          └────────────────┘


1. Onboarding Agent Service (Main Service)

Role: The brain of the system.

Hosts the LLM integration (Ollama).
Creates onboarding plans and decides which MCP tools to call.
Maintains conversation context and orchestrates the workflow.
2. Knowledge MCP Server

Role: Exposes engineering knowledge through MCP.

Provides setup guides, architecture docs, FAQs, standards, runbooks, etc.
Exposes tools like search_documents() and resources like order-service/setup.
3. RAG Service

Role: Document ingestion and retrieval.

Chunks documents, creates embeddings, and indexes them.
Performs semantic search and returns the most relevant document sections.
4. Onboarding MCP Server

Role: Tracks onboarding progress.

Creates onboarding checklists and plans.
Stores completed steps, blockers, and next recommended actions.
5. PostgreSQL

Role: Persistent storage.

Stores onboarding sessions, plans, chat history, document metadata, and agent traces.
Useful later for prompt evaluation results as well.

