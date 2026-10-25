# HeardYou — Frontend

A whole-body women's health education and symptom-pattern exploration platform.
This repository section contains the HTML, CSS, and JavaScript frontend only. The Java / Spring Boot backend is maintained separately.

## Tech stack

| Layer | Technology |
|---|---|
| Markup | HTML5 |
| Styling | CSS3 |
| Language | JavaScript (ES modules) |
| API communication | Fetch API |
| Session state | Browser `sessionStorage` |
| Fonts | Google Fonts — Plus Jakarta Sans, Great Vibes |
| Development server | VS Code Live Server or Python HTTP server |

## Prerequisites

- A modern web browser.
- Visual Studio Code with Live Server, or Python installed.
- The repository cloned or downloaded locally.

The frontend uses mock data by default, allowing supported features to be explored without a running backend.

## Frontend features

### Condition Atlas
- Browse available health condition information.
- Search and filter condition entries.
- Navigate to individual condition details.

### Condition Details
- Explore information about individual health conditions.
- View relevant symptom and educational information provided by the data source.

### Symptom Check-in
- Select a life stage.
- Search for and select symptoms.
- Record symptom severity and duration.
- Submit symptom information for pattern exploration.

### Results
- View symptom-pattern results.
- Explore related explanations and information supplied by the configured data source.

### News Component
- Provides the frontend component for news-related content.

### Doctor-Ready Brief
- Includes frontend functionality for requesting a PDF brief.
- PDF generation requires a compatible backend endpoint.

## Configuration

The frontend configuration is defined in `config.js`.

- **API Base URL:** `http://localhost:8080/api`
- **Mock Mode:** Enabled by default (`USE_MOCK: true`).

Mock mode allows supported services to use local demonstration data.

To use the real API, configure the correct API base URL and set `USE_MOCK` to `false`. The backend must implement compatible endpoints and response formats.

The frontend references the following API routes:

- `GET /api/conditions`
- `GET /api/conditions/{slug}`
- `GET /api/search?q={query}`
- `POST /api/checkin`
- `POST /api/brief`

## State Management

- Check-in and results data are stored temporarily in browser `sessionStorage`.
- Session utilities manage browser-side state.
- The “Start over” action clears saved check-in and result data.
- The frontend check-in flow does not require users to create an account.

## Testing and Troubleshooting

- If JavaScript modules fail to load, use Live Server or another local HTTP server.
- If API requests fail, verify the API base URL and backend availability.
- If demonstration data appears, check whether mock mode is enabled.
- If PDF generation fails, verify that the backend endpoint is available.
- Check the browser developer console for frontend errors.

## Privacy & Safety

- Temporary check-in and result information is stored in browser session storage.
- Mock health content is intended for demonstration and is not clinically validated.
- HeardYou is an educational platform, not a diagnostic tool or a substitute for professional medical advice.
- The absence of a displayed warning sign does not establish that symptoms are safe.
- Frontend storage choices alone do not guarantee application-wide privacy; backend behavior must also be reviewed.

## Branch

**Active frontend branch:** `frontend`
Frontend development covers page layouts, styling, user interactions, browser-side state management, and integration with backend APIs.

