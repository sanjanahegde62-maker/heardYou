import { listConditions, searchContent } from "../services/conditions.js";
import { LIFE_STAGES, LIFE_STAGE_BACKEND_MAP } from "../utils/validation.js";

const $ = (id) => document.getElementById(id);
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

// Use the backend's label if it sends one; otherwise tidy the identifier for display only.
const humanize = (id) => String(id || "").replace(/[-_]+/g, " ").replace(/^./, (c) => c.toUpperCase());

/**
 * Returns a displayable system label for a condition summary DTO.
 * Backend sends `bodySystem` (e.g. "REPRODUCTIVE"), not `system`.
 * The mock data sends `systemLabel` and `system`.
 */
const systemLabel = (c) => c.systemLabel || humanize(c.bodySystem || c.system || "");

const filters = { system: "", stage: "" };
let requestId = 0;      // newest request wins; older responses are ignored
let searchTimer;
let optionsBuilt = false;

const setStatus = (msg) => { $("atlas-status").textContent = msg; };

function showError(message, retry) {
  const box = $("atlas-error");
  if (!message) { box.hidden = true; box.innerHTML = ""; return; }
  box.hidden = false;
  box.innerHTML = `<p>${esc(message)}</p><button type="button" class="btn btn--ghost" id="atlas-retry">Try again</button>`;
  $("atlas-retry").addEventListener("click", retry);
  box.focus();
}

// Category options come from what the backend returns, so ids always match.
function buildSystemOptions(items) {
  if (!Array.isArray(items)) return;
  const seen = new Map();
  items.forEach((c) => {
    // Backend DTO uses bodySystem; mock data uses system.
    const sys = c.bodySystem || c.system;
    if (sys && !seen.has(sys)) seen.set(sys, systemLabel(c));
  });
  $("filter-system").innerHTML =
    `<option value="">All body systems</option>` +
    [...seen].map(([id, label]) => `<option value="${esc(id)}">${esc(label)}</option>`).join("");
  optionsBuilt = true;
}

function card(c) {
  const slug = typeof c.slug === "string" && /^[a-z0-9-]+$/i.test(c.slug) ? c.slug : "";
  // Backend ConditionSummaryDto uses `summary` (not `description`).
  // Mock data uses `description`.
  const text = c.summary || c.description || c.snippet || "";
  const inner = `
    ${(c.bodySystem || c.system) ? `<span class="ccard__tag">${esc(systemLabel(c))}</span>` : ""}
    <h3>${esc(c.name || "Untitled")}</h3>
    ${text ? `<p>${esc(text)}</p>` : ""}
    ${slug ? `<span class="ccard__more">Read more \u2192</span>` : ""}`;
  return `<li>${
    slug
      ? `<a class="ccard" href="condition.html?slug=${encodeURIComponent(slug)}">${inner}</a>`
      : `<div class="ccard">${inner}</div>`
  }</li>`;
}

function renderCards(items, isSearch) {
  const grid = $("atlas-grid");
  if (!Array.isArray(items) || !items.length) {
    grid.innerHTML = "";
    setStatus(isSearch ? "No results found for that search." : "No conditions match these filters.");
    return;
  }
  setStatus(`${items.length} ${isSearch ? "result" : "condition"}${items.length === 1 ? "" : "s"} shown.`);
  grid.innerHTML = items.map(card).join("");
}

async function load(task, isSearch) {
  const id = ++requestId;
  showError(null);
  setStatus(isSearch ? "Searching\u2026" : "Loading conditions\u2026");
  try {
    const items = await task();
    if (id !== requestId) return;
    if (!isSearch && !optionsBuilt && !filters.system && !filters.stage) buildSystemOptions(items);
    renderCards(items, isSearch);
  } catch (err) {
    if (id !== requestId) return;
    $("atlas-grid").innerHTML = "";
    setStatus("");
    showError(err.message || "We couldn't load this. Please try again.", () => load(task, isSearch));
  }
}

const loadGrid = () => load(() => listConditions(filters), false);

$("filter-system").addEventListener("change", (e) => {
  filters.system = e.target.value;
  $("atlas-search").value = "";
  loadGrid();
});
$("filter-stage").addEventListener("change", (e) => {
  // The select value is the frontend enum (e.g. ADULT_20_39).
  // Translate it to the backend slug (e.g. "adult") before sending.
  const frontendVal = e.target.value;
  filters.stage = frontendVal ? (LIFE_STAGE_BACKEND_MAP[frontendVal] || frontendVal) : "";
  $("atlas-search").value = "";
  loadGrid();
});
$("atlas-search").addEventListener("input", (e) => {
  clearTimeout(searchTimer);
  const q = e.target.value.trim();
  if (!q) { loadGrid(); return; }
  searchTimer = setTimeout(() => load(() => searchContent(q), true), 300);
});

// first paint — populate the stage filter dropdown with frontend display values
$("filter-stage").innerHTML =
  `<option value="">All life stages</option>` +
  LIFE_STAGES.map((s) => `<option value="${s.value}">${esc(s.label)}</option>`).join("");
loadGrid();
