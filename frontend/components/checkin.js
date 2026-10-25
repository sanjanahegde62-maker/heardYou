import { searchSymptoms } from "../services/symptoms.js";
import { submitCheckin } from "../services/checkin.js";
import { LIFE_STAGES, DURATIONS, MAX_SYMPTOMS, validateCheckin } from "../utils/validation.js";
import { saveCheckin, loadCheckin, saveResults, clearAll } from "../utils/session.js";

const SEVERITY_LABELS = ["Very mild", "Mild", "Moderate", "Strong", "Very strong"];

const $ = (id) => document.getElementById(id);
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

// ---- state (kept in this tab only) ----
const empty = () => ({ lifeStage: "", symptoms: [] });
let state = loadCheckin();
if (!state || !Array.isArray(state.symptoms)) state = empty();

let lastResults = [];
let latestSearch = 0;
let timer;

const persist = () => saveCheckin(state);

// ---- life stage ----
function renderLifeStage() {
  $("life-stage").innerHTML = LIFE_STAGES.map(
    (s) => `
    <label class="choice">
      <input type="radio" name="lifeStage" value="${s.value}" ${state.lifeStage === s.value ? "checked" : ""} />
      <span>${esc(s.label)}</span>
    </label>`
  ).join("");
}
$("life-stage").addEventListener("change", (e) => {
  if (e.target.name !== "lifeStage") return;
  state.lifeStage = e.target.value;
  persist();
  renderSummary();
});

// ---- symptom search (debounced, ignores stale responses) ----
const setStatus = (msg) => { $("search-status").textContent = msg; };

$("symptom-search").addEventListener("input", (e) => {
  clearTimeout(timer);
  const q = e.target.value.trim();
  if (!q) {
    latestSearch++;
    lastResults = [];
    $("search-results").innerHTML = "";
    setStatus("");
    return;
  }
  timer = setTimeout(() => runSearch(q), 250);
});

async function runSearch(q) {
  const id = ++latestSearch;
  setStatus("Searching…");
  try {
    const results = await searchSymptoms(q);
    if (id !== latestSearch) return; // a newer search replaced this one
    lastResults = results;
    renderResults();
  } catch (err) {
    if (id !== latestSearch) return;
    lastResults = [];
    $("search-results").innerHTML = "";
    setStatus(err.message || "We couldn't search symptoms. Please try again.");
  }
}

function renderResults() {
  if (!lastResults.length) {
    $("search-results").innerHTML = "";
    setStatus("No symptoms found. Try a different word.");
    return;
  }
  setStatus(`${lastResults.length} result${lastResults.length === 1 ? "" : "s"} found.`);
  $("search-results").innerHTML = lastResults
    .map((r) => {
      const added = state.symptoms.some((s) => s.slug === r.slug);
      return `
      <li>
        <button type="button" class="result-btn" data-slug="${esc(r.slug)}" data-name="${esc(r.name)}" ${added ? "disabled" : ""}>
          ${esc(r.name)} <span>${added ? "Added" : "+ Add"}</span>
        </button>
      </li>`;
    })
    .join("");
}

$("search-results").addEventListener("click", (e) => {
  const btn = e.target.closest(".result-btn");
  if (!btn) return;
  const { slug, name } = btn.dataset; // slug comes from the backend, never made up here
  if (state.symptoms.some((s) => s.slug === slug)) return;
  if (state.symptoms.length >= MAX_SYMPTOMS) {
    showErrors([`You can add up to ${MAX_SYMPTOMS} symptoms.`]);
    return;
  }
  state.symptoms.push({ slug, name, severity: null, duration: "" });
  persist();
  renderResults();
  renderSelected();
  renderSummary();
});

// ---- selected symptoms ----
function renderSelected() {
  $("count").textContent = `${state.symptoms.length} of ${MAX_SYMPTOMS} symptoms added`;
  if (!state.symptoms.length) {
    $("selected").innerHTML = `<li class="empty">No symptoms added yet. Search above and add the ones you're having.</li>`;
    return;
  }
  $("selected").innerHTML = state.symptoms
    .map(
      (s, i) => `
    <li class="sym card" data-index="${i}">
      <div class="sym__head">
        <strong>${esc(s.name)}</strong>
        <button type="button" class="link-btn" data-remove="${i}" aria-label="Remove ${esc(s.name)}">Remove</button>
      </div>
      <div class="sym__fields">
        <label>How strong is it?
          <select data-field="severity">
            <option value="">Choose…</option>
            ${SEVERITY_LABELS.map(
              (label, n) => `<option value="${n + 1}" ${s.severity === n + 1 ? "selected" : ""}>${n + 1} – ${label}</option>`
            ).join("")}
          </select>
        </label>
        <label>For how long?
          <select data-field="duration">
            <option value="">Choose…</option>
            ${DURATIONS.map(
              (d) => `<option value="${d.value}" ${s.duration === d.value ? "selected" : ""}>${esc(d.label)}</option>`
            ).join("")}
          </select>
        </label>
      </div>
    </li>`
    )
    .join("");
}

$("selected").addEventListener("change", (e) => {
  const li = e.target.closest("[data-index]");
  const field = e.target.dataset.field;
  if (!li || !field) return;
  const symptom = state.symptoms[Number(li.dataset.index)];
  if (field === "severity") symptom.severity = e.target.value ? Number(e.target.value) : null;
  if (field === "duration") symptom.duration = e.target.value;
  persist();
});

$("selected").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-remove]");
  if (!btn) return;
  state.symptoms.splice(Number(btn.dataset.remove), 1);
  persist();
  renderResults();
  renderSelected();
  renderSummary();
  $("symptom-search").focus();
});

// ---- review, errors, submit ----
function renderSummary() {
  const stage = LIFE_STAGES.find((s) => s.value === state.lifeStage);
  const n = state.symptoms.length;
  $("summary").textContent =
    `Life stage: ${stage ? stage.label : "not chosen yet"}. ` +
    `Symptoms: ${n === 0 ? "none added yet" : n}.`;
}

function showErrors(list) {
  const box = $("errors");
  if (!list.length) {
    box.hidden = true;
    box.innerHTML = "";
    return;
  }
  box.hidden = false;
  box.innerHTML = `<strong>Please fix the following:</strong><ul>${list.map((m) => `<li>${esc(m)}</li>`).join("")}</ul>`;
  box.focus();
}

$("checkin-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const errors = validateCheckin(state);
  showErrors(errors);
  if (errors.length) return;

  const btn = $("submit-btn");
  btn.disabled = true;
  btn.textContent = "Working…";
  try {
    const result = await submitCheckin(state);
    saveCheckin(state);
    saveResults(result);
    window.location.href = "results.html"; // built in the next step
  } catch (err) {
    // Show the error; answers stay on the page so the user can retry. No health data is logged.
    showErrors([err.message || "Something went wrong. Your answers are still here, please try again."]);
    btn.disabled = false;
    btn.textContent = "See my results";
  }
});

$("start-over").addEventListener("click", () => {
  clearAll();
  state = empty();
  lastResults = [];
  latestSearch++;
  $("symptom-search").value = "";
  $("search-results").innerHTML = "";
  setStatus("");
  showErrors([]);
  renderLifeStage();
  renderSelected();
  renderSummary();
});

// ---- first paint ----
renderLifeStage();
renderSelected();
renderSummary();