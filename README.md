# heardYou — Backend

A whole-body women's health **education and symptom-pattern exploration** platform.  
This repository contains the **Java / Spring Boot backend only**. The frontend is developed separately.

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.9 |
| Persistence | Spring Data JPA + Hibernate 7, PostgreSQL 18 |
| PDF generation | OpenPDF 1.3.43 |
| JSON | Jackson 3.x (`tools.jackson.databind`) |
| Build | Maven (wrapper included — use `./mvnw`) |

---

## Prerequisites

- Java 21 (Temurin recommended)
- PostgreSQL 18 running locally with a database named `heardYou`
- The `DB_PASSWORD` environment variable set to the postgres user password

```bash
export DB_PASSWORD=your_postgres_password   # bash / zsh
# or
$env:DB_PASSWORD = "your_postgres_password"  # PowerShell
```

The schema is managed by Hibernate (`ddl-auto=update`). **Never drop or recreate the database.**  
Reference data (conditions, symptoms, warning signs) is seeded automatically on first startup via `DataLoader`.

---

## Running the application

```bash
./mvnw spring-boot:run
# or
./mvnw package && java -jar target/heardYou-*.jar
```

The API is available at `http://localhost:8080`.

---

## Running tests

```bash
./mvnw test                          # all 99 tests (no live DB required)
./mvnw test -Dtest=BriefServiceTest  # single class
```

All unit tests run without a database connection.  
`HeardYouApplicationTests` loads the full Spring context using mocked repositories — no `DB_PASSWORD` needed.

---

## API reference

All routes are under `/api`. No authentication is required — the product is intentionally login-free.

### `GET /api/health`
Liveness probe.

**Response**
```json
{ "status": "UP", "service": "HeardYou API", "timestamp": "..." }
```

---

### `POST /api/checkin`
Symptom check-in: matches submitted symptoms against the educational condition catalogue and evaluates warning signs.  
Nothing is persisted.

**Request body**
```json
{
  "lifeStage": "reproductive-age",
  "symptoms": [
    { "slug": "pelvic-pain", "severity": 4, "duration": "OVER_1_MONTH" }
  ]
}
```

`lifeStage` allowed values: `teen` · `reproductive-age` · `perimenopause` · `postmenopause` · `adult`  
`duration` allowed values: `UNDER_1_WEEK` · `1_TO_4_WEEKS` · `OVER_1_MONTH`

**Response** — `CheckinResponse` with matched conditions, warning signs, overlap scores, and an `alwaysOnSafety` field.

---

### `GET /api/search?q={query}`
Cross-entity search across conditions and symptoms.  
Returns empty for queries shorter than 2 characters (no error).

**Response**
```json
{ "query": "endo", "conditions": [...], "symptoms": [...] }
```

---

### `GET /api/conditions`
Paginated condition listing.

| Param | Type | Description |
|---|---|---|
| `system` | string | Filter by body system (e.g. `REPRODUCTIVE`, `HORMONAL_METABOLIC`) |
| `stage` | string | Filter / sort by life stage |
| `page` | int | 0-based page index (default 0) |
| `size` | int | Page size (default 20) |

**Body systems:** `REPRODUCTIVE` · `HORMONAL_METABOLIC` · `CARDIOVASCULAR` · `BRAIN_MIND` · `AUTOIMMUNE` · `BONE_PAIN`

---

### `GET /api/conditions/{slug}`
Full condition detail including symptoms, sources, suggested tests, clinician questions, and specialist type.

---

### `GET /api/symptoms?q={query}`
Symptom listing or search.

- No `q` param → returns all symptoms
- `q` with ≥ 2 characters → full-text + alias search
- `q` with < 2 characters → returns empty list (no error)

---

### `POST /api/brief`
Generates a single-page A4 PDF brief to bring to a medical appointment.  
Nothing is persisted. The PDF is returned as `application/pdf`.

**Request body**
```json
{
  "lifeStage": "reproductive-age",
  "displayName": "Jane",
  "concerns": "Ongoing pelvic pain for 6 months",
  "symptoms": [
    { "slug": "pelvic-pain", "severity": 4, "duration": "OVER_1_MONTH" }
  ],
  "selectedSlugs": ["endometriosis"],
  "additionalNotes": "Family history of endometriosis"
}
```

`symptoms` is capped at 15 entries. `displayName` is never persisted or logged.

**Response** — binary PDF (`Content-Disposition: attachment; filename="appointment-brief.pdf"`)

---

## Privacy & safety

- Check-in data, symptom severity, display names, and PDF contents are **never persisted or logged**.
- SQL statement logging is disabled.
- The always-on safety line appears in every check-in response:  
  *"Feeling worried or something feels very wrong? Don't wait for this app. Seek care."*
- The disclaimer in every PDF and response:  
  *"Educational information, not medical advice. If you're worried or something feels very wrong, seek care now. In an emergency, call your local emergency number (112 in India)."*

---

## Branch

Active development branch: `backend/pdf-brief-fixes`
