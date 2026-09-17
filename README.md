# SecureSync AI – ISMS SmartFlow MVP

SecureSync AI is a runnable full-stack MVP for centralized ISMS document management. It combines controlled document workflows, auditability, reminder tracking, and AI-assisted search in one role-aware application.

## Highlights

- Spring Boot backend with Java 21
- React + Material UI frontend in `frontend/`
- Email and password login for seeded application users
- ISMS workflow: `DRAFT -> SUBMITTED -> UNDER_REVIEW -> APPROVED/REJECTED`
- Version history, checksum tracking, and audit logs
- Notification inbox with overdue review reminder sweeps
- Admin archive/restore for soft-deleted documents
- Review-cycle and next-review metadata for expiry tracking
- Floating `Ask AI` assistant launcher for role-aware document search
- PDF previews with a recorded digital approval signature
- Label-specific review controls for `Internal`, `Confidential`, and `Secret` documents
- Swagger UI, PostgreSQL as the default database, and Docker Compose

## Feature matrix

| Area | Included in MVP | Notes |
| --- | --- | --- |
| Authentication | Yes | Email and password login for registered users |
| Role-based access | Yes | `EMPLOYEE`, `SDM`, `PD_HEAD`, `SUB_ADMIN`, `ADMIN`, `AUDITOR` |
| Document creation | Yes | Manual content, supported file upload, and AI draft generation |
| Workflow approvals | Yes | Submit, start review, digitally sign and approve, or reject |
| Versioning | Yes | Change summary and checksum tracking |
| AI analysis | Yes | Heuristic compliance-assist endpoint |
| AI search assistant | Yes | Floating `Ask AI` launcher on dashboard |
| Notifications | Yes | Read/unread state and reminder sweeps |
| Audit logs | Yes | Filterable evidence trail by document |
| Soft delete / restore | Yes | Admin archive and restore flow |
| PostgreSQL runtime | Yes | Default datasource via environment variables |
| Docker Compose | Yes | Backend + frontend local container run |

## Architecture overview

```text
┌─────────────────────────────────────────────────────────────┐
│                        React Frontend                      │
│  Dashboard • Documents • Notifications • Audit • Ask AI   │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP / JSON / Multipart
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot REST API                    │
│ Auth • Documents • Dashboard • Notifications • Audit • AI │
└────────────────────────────┬────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
      ┌─────────────┐  ┌─────────────┐  ┌──────────────┐
      │  Services   │  │  Security   │  │  Scheduler   │
      │ Workflow AI │  │ Email login │  │ Reminders    │
      └──────┬──────┘  └──────┬──────┘  └──────┬───────┘
             │                │                │
             └──────────┬─────┴────────────────┘
                        ▼
               ┌──────────────────┐
               │ JPA + Flyway DB  │
               │    PostgreSQL    │
               └──────────────────┘
```

### Main backend building blocks

- Controllers:
  - `AuthController`
  - `UserController`
  - `DocumentController`
  - `DashboardController`
  - `NotificationController`
  - `AuditController`
  - `ChatbotController`
- Core services:
  - document workflow and versioning
  - AI analysis and role-aware document search
  - dashboard aggregation
  - notifications and review reminders
  - audit logging
  - application user lookup and role handling
- Data layer:
  - Spring Data JPA repositories
  - Flyway-managed schema migrations
  - PostgreSQL for local, container, and deployment runtimes

## Current feature set

### Authentication and access control

- Email and password login for registered users
- Role-aware document access for:
  - `EMPLOYEE`
  - `SDM`
  - `PD_HEAD`
  - `SUB_ADMIN`
  - `ADMIN`
  - `AUDITOR`

### Dashboard

- Live summary cards for:
  - total documents
  - draft documents
  - submitted documents
  - under-review documents
  - approved documents
  - unread alerts
  - overdue reminders
- Documents-by-label breakdown
- Recent documents feed
- Recent approval activity feed
- Floating bottom-right `Ask AI` launcher with modern assistant-style UI

### Ask AI assistant

- Compact floating launcher instead of a large inline chatbot section
- Robot icon branding is consistently shown before `Ask AI` labels in the launcher panel and trigger
- Role-aware document search after login
- Smart prompt examples such as:
  - `Show me the latest Password Policy.`
  - `Which ISMS documents expire this month?`
- Expiry-style questions are interpreted using `nextReviewAt`
- Result list includes:
  - title
  - owner
  - relevance score
  - updated date
  - next review date
  - snippet preview
- Clicking a result opens the related document details
- Create an AI draft with a title, label, reviewer, and drafting instructions

### Document management

- Create documents using manual content, supported-file upload, or AI draft generation
- Document labels: `Internal`, `Confidential`, and `Secret`
- Supported upload types include:
  - `.pdf`
  - `.docx`
  - `.xls`
  - `.xlsx`
- Optional owner assignment for admins
- Reviewer email autocomplete from the user directory and distribution lists
- Review cycle in days
- Optional next review date
- Document detail drawer for workflow actions and inspection
- Inline-only PDF preview opened in a new browser tab; document download is not available

### Workflow and compliance tracking

- Submission flow from author to reviewer
- For `Internal` and `Confidential` documents, a reviewer and any higher reviewer role can act:
  - `SDM` -> SDM, PD Head, Sub Admin, or Admin
  - `PD Head` -> PD Head, Sub Admin, or Admin
  - `Sub Admin` -> Sub Admin or Admin
  - `Admin` -> Admin only
- For `Secret` documents, only the owner and the explicitly assigned individual reviewer can view the document; only that reviewer can review or approve it
- Approvals require a typed digital signature, which is recorded in the approval history and PDF preview
- Reject with remarks
- Version creation with change summaries
- Heuristic AI analysis endpoint with:
  - confidence
  - detected controls
  - compliance coverage indicators

### Notifications and reminders

- Notification inbox for reminders and workflow updates
- Read/unread toggling
- Overdue highlighting in the UI
- Manual reminder sweep action in the frontend
- Scheduled overdue reminder processing in the backend

### Audit and admin features

- Audit trail view with optional document ID filtering
- Document archive via soft delete
- Admin restore for archived documents
- Optional archived document visibility toggle for admins

## Demo users

All demo users use the same password:

- Password: `Password1!`

| Role | Login email |
| --- | --- |
| Employee | `employee1@securesync.local` |
| SDM | `sdm1@securesync.local` |
| PD Head | `pdhead1@securesync.local` |
| Sub Admin | `subadmin1@securesync.local` |
| Admin | `admin1@securesync.local` |
| Auditor | `auditor1@securesync.local` |

## Tech stack

### Backend

- Spring Boot `3.5.16`
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Actuator
- Flyway
- PostgreSQL driver
- SpringDoc OpenAPI

### Frontend

- React `18`
- Vite
- Material UI `6`
- MUI Icons

## Project structure

- Backend application: repository root
- Frontend application: `frontend/`
- Flyway migrations: `src/main/resources/db/migration/`
- Main application config: `src/main/resources/application.yml`

## Prerequisites

- Java `21`
- Maven
- Node.js and npm
- Docker / Docker Compose (recommended for the local PostgreSQL instance)

## Run locally

### 1) Start PostgreSQL

The default datasource is PostgreSQL. Start only the database service from the repository root:

```bash
docker compose up -d postgres
```

This creates database `securesync`, user `securesync`, password `securesync`, and persists data in the `securesync-postgres-data` Docker volume. Flyway creates and upgrades the schema automatically when the backend starts.

### 2) Start the backend

```powershell
mvn spring-boot:run
```

The backend defaults to `jdbc:postgresql://localhost:5432/securesync`. Override the connection with:

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/securesync'
$env:DB_USERNAME='securesync'
$env:DB_PASSWORD='securesync'
```

### 3) Start the frontend

```powershell
Set-Location frontend
npm install
npm run dev
```

If needed, the frontend uses `VITE_API_BASE_URL` and defaults to `http://localhost:8080`.

## Application URLs

- Frontend: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Screenshots

Add your latest UI images here when ready.

- [Login](docs/screenshots/login.png)
- [Dashboard](docs/screenshots/dashboard.png)
- [Documents](docs/screenshots/documents.png)
- [Ask AI assistant](docs/screenshots/ask-ai.png)
- [Notifications](docs/screenshots/notifications.png)
- [Audit trail](docs/screenshots/audit.png)

## Typical user flow

1. Sign in using a registered email and password
2. Open the `Documents` page and create a new ISMS document
3. Select the document label: `Internal`, `Confidential`, or `Secret`
4. Optionally upload a supported file or create an AI draft instead of manual content entry
5. Assign a reviewer or allow auto-assignment; `Secret` requires one individual reviewer
6. Set review cycle / next review date if required
7. Submit the document for review
8. An authorized reviewer starts review, then digitally signs and approves or rejects the document
9. Open `Preview PDF` to view the rendered document and approval signature in a new tab
10. Review history, reminders, and audit evidence in the related pages
11. Use the floating `Ask AI` launcher on the dashboard to search role-visible documents

## Example API flow

```bash
curl -u employee1@securesync.local:Password1! -H 'Content-Type: application/json' \
  -d '{"title":"Access Control Policy","category":"Internal","reviewerUsername":"sdm1","content":"This document defines scope, owner, review, approval, control and compliance obligations."}' \
  http://localhost:8080/api/documents

curl -u employee1@securesync.local:Password1! -F title='Uploaded Control Standard' -F category='Confidential' \
  -F reviewerUsername='pdhead1' -F changeSummary='Uploaded from file' \
  -F file=@./sample-policy.docx \
  http://localhost:8080/api/documents/upload

curl -u employee1@securesync.local:Password1! -H 'Content-Type: application/json' \
  -d '{"reviewerUsername":"sdm1","remarks":"Ready for review"}' \
  http://localhost:8080/api/documents/1/submit

curl -u sdm1@securesync.local:Password1! -H 'Content-Type: application/json' \
  -d '{"remarks":"Starting review"}' \
  http://localhost:8080/api/documents/1/start-review

curl -u sdm1@securesync.local:Password1! -H 'Content-Type: application/json' \
  -d '{"approved":true,"remarks":"Approved for release","signature":"SDM One"}' \
  http://localhost:8080/api/documents/1/review

curl -u admin1@securesync.local:Password1! -H 'Content-Type: application/json' \
  -d '{"query":"Which ISMS documents expire this month?","maxResults":5}' \
  http://localhost:8080/api/chatbot/query
```

## API summary

### Authentication and users

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/auth/me` | Return the currently authenticated user |
| `GET` | `/api/users?reviewersOnly=false` | List users for owner/reviewer selection |

### Documents and workflow

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/documents` | List documents, optionally including deleted items |
| `GET` | `/api/documents/{id}` | Load a single document with its detail view data |
| `POST` | `/api/documents` | Create a document from JSON content |
| `POST` | `/api/documents/upload` | Create a document from multipart text-file upload |
| `POST` | `/api/documents/{id}/submit` | Submit a draft into review workflow |
| `POST` | `/api/documents/{id}/start-review` | Start the reviewer workflow stage |
| `POST` | `/api/documents/{id}/review` | Approve or reject a document |
| `GET` | `/api/documents/{id}/pdf-preview` | Render an inline PDF preview; no download endpoint is available |
| `POST` | `/api/documents/{id}/versions` | Create a new document version |
| `GET` | `/api/documents/{id}/ai-analysis` | Return heuristic compliance analysis |
| `DELETE` | `/api/documents/{id}` | Soft-delete a document |
| `POST` | `/api/documents/{id}/archive` | Archive fallback endpoint |
| `POST` | `/api/documents/{id}/restore` | Restore an archived document |

### Dashboard, AI, notifications, and audit

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/dashboard` | Return role-aware dashboard metrics and summaries |
| `POST` | `/api/chatbot/query` | Query the `Ask AI` assistant for role-scoped document search |
| `GET` | `/api/notifications` | List notifications for the signed-in user |
| `GET` | `/api/notifications/overdue/count` | Return overdue reminder count |
| `POST` | `/api/notifications/{id}/read` | Mark a notification as read or unread |
| `POST` | `/api/notifications/reminders/run` | Trigger reminder sweep processing |
| `GET` | `/api/audit-logs` | List audit logs, optionally filtered by `documentId` |

### Operational endpoints

| Path | Purpose |
| --- | --- |
| `/swagger-ui.html` | Interactive API documentation |
| `/v3/api-docs` | OpenAPI schema |
| `/actuator/health` | Health endpoint |
| PostgreSQL | `localhost:5432`, database `securesync`, user `securesync` |

## Run tests

### Backend tests

```powershell
mvn test
```

### Frontend production build

```powershell
Set-Location frontend
npm run build
```

## Run with Docker Compose

```powershell
docker compose up --build
```

Docker Compose exposes:

- PostgreSQL on `localhost:5432`
- backend on `http://localhost:8080`
- frontend on `http://localhost:5173`

The backend waits for PostgreSQL to pass its healthcheck before starting. To run in the background, use `docker compose up --build -d`. After a code or Dockerfile change, rebuild and restart with `docker compose up --build`.

Stop containers without deleting data:

```powershell
docker compose down
```

Reset the database completely, including uploaded documents, workflow history, notifications, and user records:

```powershell
docker compose down -v
docker compose up --build
```

The next backend startup creates the database schema with Flyway and reseeds the six default users because the `users` table is empty. Any users added after startup must be recreated after a volume reset.

## Deployment notes

PostgreSQL is the default runtime datasource. Provide these variables when connecting to an external database:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

```powershell
mvn spring-boot:run
```

Additional notes:

- Schema lifecycle is managed by Flyway migrations in `src/main/resources/db/migration`
- JPA runs in `ddl-auto: validate` mode to catch schema drift
- The `test` Spring profile uses an in-memory H2 database only for automated tests; it is not used by the normal application or Docker Compose runtime
- PostgreSQL must be reachable before startup; a missing or incorrect password produces a datasource authentication error

