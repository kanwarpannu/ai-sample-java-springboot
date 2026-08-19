CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS documents (
    id          VARCHAR(255) PRIMARY KEY,
    title       VARCHAR(500) NOT NULL,
    content     TEXT         NOT NULL,
    category    VARCHAR(100),
    tags        TEXT,
    source      VARCHAR(500),
    last_updated DATE
);

CREATE INDEX IF NOT EXISTS idx_documents_category ON documents (category);

-- Spring AI PgVectorStore table (768 dims = nomic-embed-text)
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(768)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store USING HNSW (embedding vector_cosine_ops);
