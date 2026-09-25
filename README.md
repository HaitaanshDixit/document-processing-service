# Document Processing Service — Backend

Spring Boot service implementing the SuretySeven SDE-1 take-home assignment:
async document upload, mock extraction, validation, retries, duplicate
detection, processing history, search/filtering and observability.

Frontend will be added separately under `/frontend` once the backend is verified.

## Stack

| Concern | Choice | Why |
|---|---|---|
| Language / framework | Java 21, Spring Boot 3.3 | Team's chosen stack, mature ecosystem |
| Persistence | PostgreSQL | Durable, relational, easy to query/filter/paginate |
| Queue + coordination | Redis | Free, in-memory, gives us a work queue (`LPUSH`/`BRPOP`), duplicate-detection (`SETNX`), and a distributed lock in one dependency |
| Retry | Spring Retry (`RetryTemplate`) | Declarative backoff/retry policy instead of hand-rolled loops |
| Build | Maven | Standard for Spring Boot / IntelliJ |

Everything used (Postgres, Redis, Spring Boot, Spring Retry) is free and open source.

## Running locally

1. Start infrastructure:
   ```bash
   docker compose up -d
   ```
2. Run the app from IntelliJ (run `DocumentProcessingServiceApplication`) or:
   ```bash
   mvn spring-boot:run
   ```
3. The API is available at `http://localhost:8080`.

Uploaded files are stored under `./uploads` (configurable via `app.storage.upload-dir`).

## API

### `POST /documents` (multipart/form-data)
Fields: `file`, `documentType`, `metadata` (optional string).
```json
{ "documentId": "DOC-AB12CD34", "status": "UPLOADED", "duplicate": false }
```
If the same file content was uploaded before, `duplicate: true` is returned with the **existing** document's id — no new record or processing job is created.

### `GET /documents/{documentId}`
```json
{
  "documentId": "DOC-AB12CD34",
  "status": "PROCESSED",
  "documentType": "FINANCIAL_STATEMENT",
  "originalFilename": "statement.pdf",
  "failureReason": null,
  "retryCount": 1,
  "createdAt": "...",
  "updatedAt": "...",
  "result": {
    "companyName": "ABC Construction Pvt Ltd",
    "registrationNumber": "U12345DL2020PTC123456",
    "address": "New Delhi",
    "annualRevenue": 12500000,
    "documentDate": "2026-08-15"
  },
  "validationErrors": null
}
```

### `GET /documents/{documentId}/history`
```json
[
  {"status": "UPLOADED", "reason": null, "timestamp": "..."},
  {"status": "PROCESSING", "reason": null, "timestamp": "..."},
  {"status": "FAILED", "reason": "PROCESSOR_TIMEOUT", "timestamp": "..."},
  {"status": "PROCESSING", "reason": null, "timestamp": "..."},
  {"status": "PROCESSED", "reason": null, "timestamp": "..."}
]
```

### `GET /documents?status=FAILED&documentType=FINANCIAL_STATEMENT&page=0&size=20`
Paginated list, filterable by status and/or document type.

## Processing state machine

```
UPLOADED -> PROCESSING -> PROCESSED
                       -> FAILED (retried up to N times for transient errors)
```

The mock processor (`MockRandomDocumentProcessor`) returns `SUCCESS`, `TIMEOUT`,
`ERROR`, or `INVALID_RESULT` at random, and occasionally produces structurally
valid-but-business-invalid data (e.g. negative revenue) to exercise the
validation path.

**Retry policy** (`app.processing.*` in `application.yml`):
- `TIMEOUT`, `ERROR`, `INVALID_RESULT` → retried, up to `max-attempts` (default 3) with exponential backoff (1s, 2s, capped at 10s).
- A validation failure on otherwise-successful extraction (e.g. `annualRevenue < 0`) is **not** retried — the data is deterministic, so retrying the mock processor won't fix it. The document is marked `FAILED` with `VALIDATION_FAILED` immediately and the specific field errors are stored.
- After transient retries are exhausted, the document is marked `FAILED` with `MAX_RETRIES_EXCEEDED`.

## Duplicate detection

SHA-256 hash of the file's bytes, reserved atomically in Redis with `SETNX`
(`dup:filehash:{hash} -> documentId`). This was chosen over filename+size
because it's exact-content based (renames don't fool it, byte-identical files
under different names are still correctly deduplicated) and the check is a
single O(1) Redis call before we ever touch Postgres. A DB index on
`file_hash` exists as a fallback if Redis is ever unavailable/flushed.

## Observability

Every processing log line is tagged with `documentId` via SLF4J's MDC
(`[documentId=DOC-...]`). Combined with the persisted `document_history`
table (status, reason, timestamp per attempt), answering "why did DOC-12345
fail" only requires `GET /documents/{id}/history` or grepping logs for that
id — no document content or extracted business data is ever logged.

## Engineering questions

See `/docs/ENGINEERING_QUESTIONS.md` (added once the full assignment,
including frontend, is complete).
