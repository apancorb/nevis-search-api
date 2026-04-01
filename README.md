# Nevis Search API

A Search API across clients and documents with semantic search capabilities, built for wealth management advisors.

- **Client search**: substring and fuzzy matching across email, name, and description using PostgreSQL trigram indexes
- **Document search**: semantic similarity search using vector embeddings (e.g., "address proof" finds documents containing "utility bill")
- **Document summarization** (optional): LLM-powered summaries via OpenAI when an API key is provided

## Requirements

- Java 17+
- Docker and Docker Compose

## Quick Start

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Configuration

All configuration is done via environment variables with sensible defaults:

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `nevis_search` | Database name |
| `DB_USER` | `nevis` | Database user |
| `DB_PASSWORD` | `nevis` | Database password |
| `OPENAI_API_KEY` | (empty) | Optional. Enables LLM-powered document summaries |

To enable document summarization:

```bash
OPENAI_API_KEY=sk-... docker compose up --build
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/clients` | Create a new client |
| POST | `/clients/{id}/documents` | Create a document for a client |
| GET | `/search?q={query}` | Search across clients and documents |

Full API documentation is available at `/swagger-ui.html` when the application is running.

## Demo Walkthrough

After running `docker compose up --build`, follow these steps to see the API in action with realistic wealth management data.

### Step 1: Create clients

```bash
# Wealth manager
curl -s -X POST http://localhost:8080/clients -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john.doe@neviswealth.com","description":"Senior portfolio manager at Nevis Wealth, specializing in high-net-worth individuals","socialLinks":["https://linkedin.com/in/johndoe"]}'

# Financial advisor
curl -s -X POST http://localhost:8080/clients -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Smith","email":"jane.smith@goldmanpartners.com","description":"Financial advisor specializing in retirement planning and estate management"}'

# Tech entrepreneur
curl -s -X POST http://localhost:8080/clients -H "Content-Type: application/json" \
  -d '{"firstName":"Michael","lastName":"Chen","email":"m.chen@bridgecapital.io","description":"Tech entrepreneur, angel investor, focused on early-stage fintech startups"}'

# Compliance officer
curl -s -X POST http://localhost:8080/clients -H "Content-Type: application/json" \
  -d '{"firstName":"Sarah","lastName":"Williams","email":"sarah.w@neviswealth.com","description":"Head of compliance, responsible for KYC and AML processes"}'

# Retired physician
curl -s -X POST http://localhost:8080/clients -H "Content-Type: application/json" \
  -d '{"firstName":"Robert","lastName":"Martinez","email":"rob.martinez@outlook.com","description":"Retired physician, conservative investment profile, income-focused strategy"}'
```

### Step 2: Create documents

Use the client IDs from the responses above. Here are examples of realistic wealth management documents:

```bash
CLIENT_ID="<john-doe-id>"

# Address proof document
curl -s -X POST http://localhost:8080/clients/$CLIENT_ID/documents -H "Content-Type: application/json" \
  -d '{"title":"Utility Bill - March 2026","content":"Electricity bill from ConEd for the period of March 2026. Service address: 123 Main Street, New York, NY 10001. Account holder: John Doe. Total amount due: $142.50."}'

# Portfolio review
curl -s -X POST http://localhost:8080/clients/$CLIENT_ID/documents -H "Content-Type: application/json" \
  -d '{"title":"Investment Portfolio Summary Q1 2026","content":"Quarterly portfolio review for Q1 2026. Total assets under management: $2.4M. Asset allocation: 60% equities, 30% fixed income, 10% alternatives. Year-to-date return: +4.2%. Benchmark comparison: S&P 500 +3.8%."}'

# Identity document
curl -s -X POST http://localhost:8080/clients/$CLIENT_ID/documents -H "Content-Type: application/json" \
  -d '{"title":"Passport Copy","content":"United States passport. Full name: John Andrew Doe. Date of birth: March 15, 1978. Passport number: 587234961. Issue date: January 10, 2023. Expiration date: January 9, 2033."}'

# Bank statement
curl -s -X POST http://localhost:8080/clients/$CLIENT_ID/documents -H "Content-Type: application/json" \
  -d '{"title":"Bank Statement - February 2026","content":"Chase Bank checking account statement for February 2026. Account ending in 4589. Opening balance: $45,230.00. Total deposits: $12,500.00. Total withdrawals: $8,750.00. Closing balance: $48,980.00. Direct deposit from Nevis Wealth Management LLC."}'
```

Wait a few seconds for async embedding to complete, then search.

### Step 3: Search examples

**Client search by email substring** - "NevisWealth" finds both employees with `@neviswealth.com` emails:

```bash
curl "http://localhost:8080/search?q=NevisWealth"
```

| Score | Type | Result |
|---|---|---|
| 0.90 | client | John Doe (john.doe@neviswealth.com) |
| 0.90 | client | Sarah Williams (sarah.w@neviswealth.com) |
| 0.21 | document | Bank Statement (mentions "Nevis Wealth Management LLC") |

**Semantic search: "address proof"** - finds documents that could serve as proof of address, even though those exact words don't appear:

```bash
curl "http://localhost:8080/search?q=address+proof"
```

| Score | Type | Result |
|---|---|---|
| 0.17 | document | Passport Copy |
| 0.16 | document | Bank Statement |
| 0.11 | document | Utility Bill |
| 0.11 | document | Driver License Copy |

**Semantic search: "identity verification"** - finds ID documents and KYC reports:

```bash
curl "http://localhost:8080/search?q=identity+verification"
```

| Score | Type | Result |
|---|---|---|
| 0.33 | document | Passport Copy |
| 0.30 | document | KYC Verification Report |
| 0.20 | document | Driver License Copy |

**Semantic search: "compliance regulations"** - finds compliance-related documents:

```bash
curl "http://localhost:8080/search?q=compliance+regulations"
```

| Score | Type | Result |
|---|---|---|
| 0.31 | document | KYC Verification Report |
| 0.30 | document | AML Policy Update Memo |

**Mixed search: "fintech startup"** - finds the client by description AND the related investment document:

```bash
curl "http://localhost:8080/search?q=fintech+startup"
```

| Score | Type | Result |
|---|---|---|
| 0.70 | client | Michael Chen ("Tech entrepreneur, angel investor, focused on early-stage fintech startups") |
| 0.42 | document | Angel Investment Term Sheet - FinPay |

**Client search by description: "physician"** - finds Robert Martinez through his description:

```bash
curl "http://localhost:8080/search?q=physician"
```

| Score | Type | Result |
|---|---|---|
| 0.70 | client | Robert Martinez ("Retired physician, conservative investment profile") |
```

## Running Tests

```bash
./gradlew test
```

This automatically starts a PostgreSQL container, runs all tests (43 total: 25 unit + 18 integration), and stops the container.

## Architecture

### Tech Stack

- **Java 17** with Spring Boot 3.4
- **PostgreSQL 16** with pgvector (vector similarity) and pg_trgm (fuzzy text search)
- **Spring AI 1.1** with ONNX embedding model (all-MiniLM-L6-v2, runs locally)
- **Flyway** for database migrations
- **SpringDoc OpenAPI** for API documentation

### How Search Works

The `/search` endpoint runs two searches in parallel and merges the results:

1. **Client search** uses PostgreSQL `ILIKE` with `pg_trgm` indexes for substring and fuzzy matching across email, name, and description fields.

2. **Document search** uses vector embeddings. When a document is created, its text is embedded into a 384-dimensional vector using a local ONNX model (all-MiniLM-L6-v2) and stored in pgvector. At search time, the query is embedded and compared using cosine similarity.

Results from both searches are normalized to a 0-1 score, merged into a flat list, and sorted by relevance.

### Document Indexing

Document embedding happens asynchronously after creation. The POST returns immediately with a 201, and the document becomes searchable within a few seconds.

### Design Decisions

- **ONNX over OpenAI for embeddings**: runs locally with zero API keys, so `docker compose up` works out of the box.
- **pg_trgm for client search**: the correct tool for substring matching. Embeddings would be over-engineering for "NevisWealth" matching "john.doe@neviswealth.com".
- **Async indexing**: fast POST responses at the cost of a brief unsearchable window. Acceptable tradeoff for a non-blocking API.
- **Batch document fetch**: vector search results are fetched from the documents table in a single `WHERE id IN (...)` query, avoiding N+1.

### Production Considerations

- **Document chunking**: the ONNX model truncates input at ~256 tokens. For large documents, a chunking strategy would be needed to embed each chunk separately.
- **Pagination**: currently results are capped at a configurable limit (default 20). Cursor-based pagination could be added for larger datasets.
- **Caching**: Redis could cache frequent search queries and embedding results.
