// sessionStorage only: it clears when the tab closes. Never use localStorage for health data.
const CHECKIN_KEY = "tvh-checkin";
const RESULTS_KEY = "tvh-results";

function save(key, value) {
  try { sessionStorage.setItem(key, JSON.stringify(value)); } catch { /* storage unavailable */ }
}
function load(key) {
  try {
    const raw = sessionStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  } catch { return null; }
}

export const saveCheckin = (state) => save(CHECKIN_KEY, state);
export const loadCheckin = () => load(CHECKIN_KEY);
export const saveResults = (results) => save(RESULTS_KEY, results);
export const loadResults = () => load(RESULTS_KEY);

// Call this for "Start over" / "Clear my check-in"
export function clearAll() {
  try {
    sessionStorage.removeItem(CHECKIN_KEY);
    sessionStorage.removeItem(RESULTS_KEY);
  } catch { /* ignore */ }
}