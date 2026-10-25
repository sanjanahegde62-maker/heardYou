import { loadCheckin, loadResults, clearAll } from "../utils/session.js";
import { validateCheckin, LIFE_STAGES, DURATIONS } from "../utils/validation.js";
import { downloadBrief } from "../services/brief.js";

const SEVERITY_LABELS = ["Very mild", "Mild", "Moderate", "Strong", "Very strong"];
const OVERLAP = {
  HIGH:     { label: "High",     dots: "\u25cf\u25cf\u25cf" },
  MODERATE: { label: "Moderate", dots: "\u25cf\u25cf\u25cb" },
  LOW:      { label: "Low",      dots: "\u25cf\u25cb\u25cb" },
};

const $ = (id) => document.getElementById(id);
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const bulletList = (items) =>
  Array.isArray(items) && items.length ? `<ul>${items.map((i) => `<li>${esc(i)}</li>`).join("")}</ul>` : "";

// Only real http(s) links become clickable
function sourceLink(url, label) {
  if (typeof url !== "string" || !/^https?:\/\//i.test(url)) return esc(label || "");
  let text = label;
  if (!text) {
    try { text = new URL(url).hostname.replace(/^www\./, ""); } catch { text = url; }
  }
  return `<a href="${esc(url)}" target="_blank" rel="noopener noreferrer">${esc(text)}</a>`;
}

const checkin = loadCheckin();
const results = loadResults();

function renderAnswers() {
  const stage = LIFE_STAGES.find((s) => s.value === checkin.lifeStage);
  const items = checkin.symptoms
    .map((s) => {
      const dur = DURATIONS.find((d) => d.value === s.duration);
      const sev = s.severity ? `${s.severity} \u2013 ${SEVERITY_LABELS[s.severity - 1] || ""}` : "not given";
      return `<li>${esc(s.name || s.slug)}: strength ${esc(sev)}; ${esc(dur ? dur.label.toLowerCase() : "duration not given")}</li>`;
    })
    .join("");
  $("answers").innerHTML = `<p><strong>Life stage:</strong> ${esc(stage ? stage.label : "not given")}</p><ul>${items}</ul>`;
}

function renderWarnings(items) {
  const box = $("warnings");
  if (!items.length) { box.hidden = true; return; }
  box.hidden = false;
  $("warning-list").innerHTML = items
    .map(
      (w) => `
    <li class="warning-item">
      <p class="warning-item__name"><span aria-hidden="true">\u26a0\ufe0f</span> Warning sign: ${esc(w.name)}</p>
      <p>${esc(w.message || "")}</p>
      ${w.sourceUrl || w.sourceLabel ? `<p class="note">Source: ${sourceLink(w.sourceUrl, w.sourceLabel)}</p>` : ""}
    </li>`
    )
    .join("");
}

function renderPatterns(patterns, names) {
  const list = $("pattern-list");
  if (!patterns.length) {
    list.innerHTML = `
      <li class="panel">
        Symptom overlap could not identify any supported patterns from the available data.
        This does not rule out any health condition. If you are worried about your symptoms,
        consider talking to a healthcare professional.
      </li>`;
    return;
  }

  list.innerHTML = patterns
    .map((p) => {
      // Backend PatternMatch field names:
      //   conditionSlug, conditionName, overlapLabel, overlapExplanation,
      //   specialistType, clinicianQuestions, matchedSymptoms (List<MatchedSymptomDetail>)
      const slug     = p.conditionSlug;
      const name     = p.conditionName;
      const o        = OVERLAP[p.overlapLabel] || { label: "Not stated", dots: "" };

      // matchedSymptoms is a List<MatchedSymptomDetail> with { symptomSlug, symptomName, ... }
      // Resolve display name: prefer symptomName from DTO, fall back to checkin names map.
      const matched = (p.matchedSymptoms || []).map((m) => {
        if (typeof m === "string") return names[m] || m;           // defensive: plain string
        return m.symptomName || names[m.symptomSlug] || m.symptomSlug || "";
      }).filter(Boolean);

      const sources = (p.sources || []).map((s) => {
        const url   = typeof s === "string" ? s : s.url;
        const label = typeof s === "string" ? null : (s.label || s.name);
        return `<li>${sourceLink(url, label)}</li>`;
      }).join("");

      return `
      <li class="pattern">
        <label class="pattern__pick">
          <input type="checkbox" value="${esc(slug)}" checked />
          Include in my PDF
        </label>
        <h3>${esc(name)}</h3>
        <p class="overlap"><span class="overlap__dots" aria-hidden="true">${o.dots}</span> Pattern overlap: ${o.label}</p>
        ${matched.length ? `<p><strong>Matched symptoms (${matched.length}):</strong> ${esc(matched.join(", "))}</p>` : ""}

        ${p.overlapExplanation ? `<h4>Why this appeared</h4><p>${esc(p.overlapExplanation)}</p>` : ""}
        ${p.specialistType ? `<h4>Relevant specialist</h4><p>${esc(p.specialistType)}</p>` : ""}
        ${p.suggestedTests && p.suggestedTests.length ? `<h4>Tests you could ask about</h4>${bulletList(p.suggestedTests)}` : ""}
        ${p.clinicianQuestions && p.clinicianQuestions.length ? `<h4>Questions to ask your clinician</h4>${bulletList(p.clinicianQuestions)}` : ""}
        ${p.timeframeAdvice ? `<h4>Timeframe</h4><p>${esc(p.timeframeAdvice)}</p>` : ""}
        ${sources ? `<h4>Sources</h4><ul>${sources}</ul>` : ""}
      </li>`;
    })
    .join("");
}

// ---- PDF ----
function showPdfError(message) {
  const box = $("pdf-error");
  if (!message) { box.hidden = true; box.textContent = ""; return; }
  box.hidden = false;
  box.textContent = message;
  box.focus();
}

$("download-btn").addEventListener("click", async () => {
  showPdfError(null);
  $("pdf-status").textContent = "";

  const problems = validateCheckin(checkin);
  if (problems.length) {
    showPdfError("Your check-in is incomplete. Please go back and finish it before downloading the brief.");
    return;
  }

  const selectedSlugs = [...document.querySelectorAll(".pattern__pick input:checked")].map((i) => i.value);
  const btn = $("download-btn");
  btn.disabled = true;
  btn.textContent = "Preparing your PDF\u2026";

  try {
    await downloadBrief({
      lifeStage: checkin.lifeStage,
      symptoms: checkin.symptoms,
      selectedSlugs,
      displayName: $("display-name").value.trim(),
    });
    $("pdf-status").textContent = "Your brief has been downloaded.";
  } catch (err) {
    // Results stay on the page so the user can retry. Nothing health-related is logged.
    showPdfError(err.message || "We couldn't create your PDF. Please try again.");
  } finally {
    btn.disabled = false;
    btn.textContent = "Download PDF brief";
  }
});

$("start-over").addEventListener("click", () => {
  clearAll();
  window.location.href = "checkin.html";
});

// ---- start ----
if (!checkin || !results || !Array.isArray(checkin.symptoms)) {
  $("expired").hidden = false;
} else {
  const names = Object.fromEntries(checkin.symptoms.map((s) => [s.slug, s.name || s.slug]));

  // Backend CheckinResponse field names:
  //   warningSignsEvaluated (not warningSigns)
  //   matches               (not patterns)
  const warningSigns = Array.isArray(results.warningSignsEvaluated) ? results.warningSignsEvaluated : [];
  const patterns     = Array.isArray(results.matches)              ? results.matches              : [];

  $("content").hidden = false;
  if (results.disclaimer) $("disclaimer").textContent = results.disclaimer;
  renderAnswers();
  renderWarnings(warningSigns);
  renderPatterns(patterns, names);
}
