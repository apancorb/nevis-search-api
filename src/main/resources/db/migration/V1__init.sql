CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE clients (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    description  TEXT,
    social_links JSONB DEFAULT '[]',
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_clients_email_trgm ON clients USING gin (email gin_trgm_ops);
CREATE INDEX idx_clients_name_trgm ON clients USING gin ((first_name || ' ' || last_name) gin_trgm_ops);
CREATE INDEX idx_clients_description_trgm ON clients USING gin (description gin_trgm_ops);

CREATE TABLE documents (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id  UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    title      VARCHAR(500) NOT NULL,
    content    TEXT NOT NULL,
    summary    TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_documents_client_id ON documents(client_id);
