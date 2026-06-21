# Emeritus Edge — Masterclass Scheduling API

Spring Boot backend for the Emeritus take-home exercise (masterclass scheduling).

## Prerequisites

- **Java 21** (JDK) installed and on your `PATH`
- Internet on first run (Maven Wrapper downloads Maven automatically)

Check Java:

```bash
java -version
```

Should show version 21.x.

## Run the API (one command)

### Windows (Command Prompt or PowerShell)

```bat
mvnw.cmd spring-boot:run
```

Or double-click / run:

```bat
run.bat
```

### Mac / Linux

```bash
chmod +x mvnw run.sh
./run.sh
```

The API starts at **http://localhost:8080**

You should see `Started EdgeBackendApplication` in the console.

## Run tests (one command)

### Windows

```bat
mvnw.cmd test
```

Or:

```bat
test.bat
```

### Mac / Linux

```bash
./mvnw test
```

All tests should pass (green build).

## Test guide

### Automated tests

```bat
test.bat
```

**Expected:** `BUILD SUCCESS` — full suite (Round 1 + Round 2 upload), 0 failures.

| Test class | What it verifies |
|------------|------------------|
| `MultiTenancyTests` | Cross-tenant access blocked; global catalogue needs no header |
| `TwoLevelCapTests` | Bucket cap (1/3/8) and per-topic allocation cap |
| `ConcurrentSchedulingTests` | Only one request wins the last slot under race |
| `SchedulingConstraintTests` | 14-day rule, speaker–topic eligibility, audience XOR |
| `IdempotencyTests` | Same key returns original session; no duplicate event |
| `EventPublicationTests` | Events on schedule, cancel, allocation update |
| `InvitationReadTests` | Employee sees cohort + audience-all sessions correctly |
| `FileUploadServiceReconciliationTests` | CSV reconcile: create/update, cohort diff, conflicts (unit) |
| `FileUploadIntegrationTests` | Multipart upload end-to-end against H2 seed data |
| `FileUploadControllerTests` | Upload auth guard (non-admin forbidden) |

### Manual API tests (curl)

With the app running (`run.bat`), open a second terminal.

Quick smoke test (Round 1):

```bat
curl -i http://localhost:8080/v1/topics
```

| Guide | Scope |
|-------|--------|
| **[CURL.md](CURL.md)** | Round 1 — scheduling, allocations, idempotency, error cases |
| **[CURL_ROUND2.md](CURL_ROUND2.md)** | Round 2 — employee/cohort CSV upload preview (happy + error paths) |

Sample CSV files for Round 2 manual tests live in **[samples/](samples/)** (`upload-happy.csv`, `upload-update.csv`, `upload-errors.csv`).

Round 2 design and reconciler rules → **[ROUND2_README.md](ROUND2_README.md)**.

## Auth assumption

No JWT or SSO in this slice. Every tenant-scoped request carries:

- `X-Tenant-Id` — which customer the caller acts for
- `X-User-Role` — `LND_ADMIN` required for POST/PUT/DELETE writes
- `Idempotency-Key` — required on `POST /v1/sessions`

`TenantAccessInterceptor` enforces the header on `/v1/sessions`, `/v1/employees`, and `/v1/tenants/{id}/...` paths. For allocation URLs, the header must match the `{id}` in the path.

## Stack and why

| Choice | Why |
|--------|-----|
| **Java 21 + Spring Boot 4** | Mature transactional model (`@Transactional`), dependency injection, and REST support — matches how production Emeritus services would likely be built. |
| **Spring Data JPA** | Encodes the two-level cap and scheduling rules in one service transaction with pessimistic row locks. |
| **H2 in-memory** | Clone-and-run with no Docker or Postgres install. Schema and SQL are portable to Postgres with minimal changes. |
| **Layered packages** | `controller → service → repository` keeps HTTP, business rules, and persistence separate and testable. |
| **In-process `EventPublisher`** | Shows event shape and publish points without Kafka setup; swap for a real bus behind the same interface. |

`run.bat` / `test.bat` auto-use a local `jdk/` folder if present; otherwise Java 21 must be on `PATH`.

## Data model (sketch)

Two zones:

```
GLOBAL (shared)          TENANT-SCOPED (isolated by tenant_id)
───────────────          ─────────────────────────────────────
topic                    tenant_bucket      ← commercial cap (1/3/8)
speaker                  tenant_allocation  ← L&D slot plan per topic
speaker_topic            masterclass_session
                         session_cohorts    ← when audience_all = false
                         employee
                         employee_cohorts
```

**Two-level cap:** `SUM(allocated_slots) ≤ bucket.max_limit` and `COUNT(sessions per topic) ≤ allocated_slots for that topic`.

Full ERD, indexes, and table notes → **[SCHEMA.md](SCHEMA.md)**.

## Event schema

Events publish via `EventPublisher.publish(event)` (stub logs to console). Downstream services (notifications, CSM dashboard, calendar) would subscribe to these.

| eventType | When fired |
|-----------|------------|
| `session.scheduled` | New session created (not on idempotent retry) |
| `session.cancelled` | Session deleted |
| `allocation.updated` | PUT allocations succeeds |

Example — `session.scheduled`:

```json
{
  "eventType": "session.scheduled",
  "occurredAt": "2026-06-06T10:30:00Z",
  "tenantId": "vantage-fi",
  "sessionId": 1,
  "topicId": 1,
  "speakerId": 1,
  "sessionDate": "2026-08-01",
  "title": "AI Workshop",
  "theme": "Leadership",
  "audienceAll": true,
  "cohortIds": []
}
```

`session.cancelled` carries `sessionId`, `topicId`, `sessionDate`, `title`.  
`allocation.updated` carries the full post-update list: `{ topicId, allocatedSlots }[]`.

## Invitation read pattern

`GET /v1/employees/{id}/upcoming-sessions` uses two indexed queries merged in memory:

1. Upcoming `audience_all` sessions for the tenant (`session_date >= today`)
2. Upcoming cohort-targeted sessions where `session_cohorts` overlaps the employee's cohorts

Results are de-duplicated and sorted by date. Index on `(tenant_id, session_date)` supports the date filter.

**Cohort membership after scheduling:** we use **current** membership, not a snapshot at schedule time. If an employee joins a cohort after a session was scheduled for that cohort, they see the invitation on their next LMS read. This matches live LMS expectations. A point-in-time snapshot would be better for audit but needs a materialized `session_invitation` table — the right move at scale (1000+ cohorts).

## Trade-offs

1. **H2 vs Postgres** — H2 keeps setup friction at zero for this exercise. Postgres would add connection pooling, real concurrency tuning, and `SELECT … FOR UPDATE` behaviour closer to production. I would switch for any shared environment.

2. **Pessimistic DB locks vs distributed lock (Redis)** — Pessimistic locks on `tenant_bucket` and `tenant_allocation` inside a single DB transaction are correct for one database and two admins racing for the last slot. Speaker time-slot conflicts would be owned by a separate availability service (see below), not merged into this transaction.

3. **Current cohort membership vs schedule-time snapshot** — Current membership is simpler to query and better UX when people join cohorts late. Snapshot-at-schedule is stricter for compliance; I would add an event-driven invitation table if legal/compliance required immutable invite lists.

4. **Simulated header role vs entitlement-based RBAC** — `X-User-Role: LND_ADMIN` gates writes in controllers. Production would authenticate via JWT/SSO, then evaluate **entitlements** per logged-in user: which actions (schedule, cancel, allocate) and views (employee invitations, CSM dashboards) are allowed for that tenant. UI and API would consult a policy service — not a hard-coded header string.

5. **Date-only vs start/end datetime** — Sessions store `LocalDate` and enforce a 14-calendar-day rule only. Real masterclasses need `start_at` / `end_at` in UTC plus display timezone. Time-of-day and speaker overlap checks belong in a speaker-availability service (out of scope per brief), not duplicated here.

6. **Minimal logging vs production observability** — Domain events log to console via `LoggingEventPublisher`; there is no structured request logging, correlation IDs, or audit trail. Production needs JSON logs (SLF4J → ELK/Datadog), a `traceId` on every HTTP request, and audit records for schedule/cancel/allocation (who, tenant, session id).

7. **In-process events vs outbox + message bus** — `EventPublisher.publish()` is a local stub. Production needs an outbox row in the same DB transaction as the booking, async delivery via Kafka/SQS, retries, and a dead-letter queue for failed consumers.

8. **Local JAR vs CI/CD + containers** — Builds and tests run manually via `test.bat` / `mvnw`. Production would gate every PR with automated `mvnw verify`, build a Docker image, and deploy through a pipeline — same commands, enforced on every merge.

## With more time

### Platform and delivery

- **CI/CD** — GitHub Actions (or GitLab CI) on every PR: `mvnw -B verify`; on merge, build Docker image, push to registry, deploy to staging, smoke-test `GET /v1/topics`.
- **Docker + Kubernetes** — Containerize the Spring Boot app; run on K8s with HPA as tenant count grows; managed Postgres per environment instead of H2.
- **Outbox pattern** — Reliable event delivery alongside session/allocation commits.
- **Postgres + Flyway** migrations instead of H2 `data.sql`.
- **OpenAPI spec** generated from controllers.

### Speaker availability coordination

Tenant caps (this service) and speaker calendar (availability service) are separate concerns. Production booking would be two-phase:

1. **This service** — lock `tenant_bucket` + `tenant_allocation`, validate caps, create session in `PENDING` state.
2. **Availability service** — `ReserveSpeakerSlot(speakerId, startUtc, endUtc)` under its own lock; return `reservation_id` or conflict.
3. **Confirm or compensate** — on success, mark `CONFIRMED` and publish `session.scheduled`; on failure, release the pending slot and return `409`. Pass the same `Idempotency-Key` to both services so retries stay safe.

### Global deployment and time zones

- **EU data residency** — assign each tenant a `home_region` (EU / US / APAC); route `X-Tenant-Id` to the regional Postgres cluster so employee PII and session data stay in-region.
- **UTC storage, local display** — replace `sessionDate` with `start_at_utc` / `end_at_utc` (TIMESTAMPTZ) and a display `timezone`; employee upcoming filter uses `start_at_utc >= now()`.
- **Regional event buses** — Kafka topics per region; consumers process data in the same jurisdiction as the tenant.

### Observability and access control

- **Structured logging + metrics** — `sessions_scheduled_total`, cap-rejection counters, scheduling latency histograms, trace propagation.
- **RBAC / entitlements service** — Spring Security or OPA policies mapping roles (L&D Admin, Employee, CSM) to permitted API actions per tenant.
- **Materialized invitation table** — event-driven, for O(1) employee reads at scale (1000+ cohorts).

### Masterclass delivery (out of scope for this API)

Scheduling ends at “session exists, audience known, event published.” Running the live session is downstream:

| Capability | Typical production stack |
|------------|-------------------------|
| Live video | WebRTC or vendor SDK (LiveKit, Zoom, Teams) or HLS via CDN (Mux, Cloudflare Stream) |
| Recordings | Provider cloud recording → object storage (S3) + metadata linked to session |
| Live / group chat | WebSocket channels per `session_id` or `cohort_id` (Socket.io, API Gateway WebSocket, Ably) |
| Past sessions | Recording catalogue API + signed playback URLs in the LMS |

`session.scheduled` / `session.cancelled` events trigger room creation, chat channels, and calendar invites in those services.

## Assumptions and spec notes

- Speaker availability and calendar integration are out of scope — the API trusts the date and speaker in the request; no start/end time or overlap checks.
- Sessions use **calendar date only** (`"date": "2026-08-01"`), not a time slot.
- Seed data has **3 speakers** (brief mentions ~20); the brief says seeding is flexible — logic is what matters.
- `PUT /v1/tenants/{id}/allocations` replaces the full allocation set for topics in the body; topics omitted are removed if they have no scheduled sessions.
- Cancellation removes the session row only; `tenant_allocation.allocated_slots` is unchanged so the slot can be re-booked.
- No structured request/audit logging or CI pipeline in this repository — intentional scope limit for the exercise.

## Round 2 — Employee / cohort CSV upload (preview)

L&D admins upload a CSV; the API parses and reconciles against tenant data **read-only** (no DB writes in Phase 1).

- **Endpoint:** `POST /v1/upload/emp-with-cohort-assignment` (multipart `file`)
- **Manual tests:** **[CURL_ROUND2.md](CURL_ROUND2.md)** + **[samples/](samples/)**
- **Design / schema:** **[ROUND2_README.md](ROUND2_README.md)**, **[SCHEMA_ROUND2.md](SCHEMA_ROUND2.md)**

## Seed data (loaded automatically)

| Tenant | Bucket | Max sessions |
|--------|--------|--------------|
| `vantage-fi` | BUCKET_02 | 3 |
| `apex-edu` | BUCKET_01 | 1 |
| `abc-edu` | BUCKET_03 | 8 |

- 8 topics, 3 speakers (with speaker–topic mappings)
- Sample employees: `emp-001`, `emp-002` (tenant `vantage-fi`)
- Round 2: `tenant_cohort` catalog (4 cohort display names for `vantage-fi`)

## Required headers (tenant-scoped APIs)

| Header | When required |
|--------|----------------|
| `X-Tenant-Id` | Sessions, employees, tenant allocations, CSV upload |
| `X-User-Role` | `LND_ADMIN` for POST/PUT/DELETE write operations and CSV upload |
| `Idempotency-Key` | `POST /v1/sessions`, `POST /v1/upload/emp-with-cohort-assignment` |

## H2 console (optional)

While running: http://localhost:8080/h2-console  
JDBC URL: `jdbc:h2:mem:edgedb` — user `sa`, password empty.

## Project layout

```
src/main/java/com/emeritus/edge_backend/
  controller/   REST APIs
  service/      Business logic
  repository/   Data access
  entity/       JPA models
  event/        Domain event stubs
src/test/java/  Automated tests
src/main/resources/data.sql  Seed data
samples/        Round 2 sample CSV files for manual upload tests
CURL.md         Round 1 manual curl test commands
CURL_ROUND2.md  Round 2 CSV upload curl test commands
ROUND2_README.md  Round 2 design and implementation notes
SCHEMA.md       Round 1 data model reference
SCHEMA_ROUND2.md  Round 2 data model reference
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `'java' is not recognized` | Install JDK 21 and add to PATH |
| `JAVA_HOME not defined` | Set `JAVA_HOME` to your JDK folder |
| Port 8080 in use | Stop other apps or add `server.port=8081` to `application.properties` |
| Empty GET results | Restart app; `data.sql` loads on startup |
