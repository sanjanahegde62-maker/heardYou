import { getCondition } from "../services/conditions.js";
import { LIFE_STAGES, LIFE_STAGE_BACKEND_MAP } from "../utils/validation.js";

const root = document.getElementById("detail");
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const humanize = (id) => String(id || "").replace(/[-_]+/g, " ").replace(/^./, (c) => c.toUpperCase());

// Accepts a string, an array of strings, or an array of objects with a text-like field.
function asList(v) {
  const items = Array.isArray(v) ? v : typeof v === "string" ? [v] : [];
  return items
    .map((i) => (typeof i === "string" ? i : i && (i.name || i.text || i.message || i.label)) || "")
    .filter(Boolean);
}
const paragraphs = (list) => list.map((t) => `<p>${esc(t)}</p>`).join("");
const bullets    = (list) => `<ul>${list.map((t) => `<li>${esc(t)}</li>`).join("")}</ul>`;

function sourceLink(url, label) {
  if (typeof url !== "string" || !/^https?:\/\//i.test(url)) return esc(label || "");
  let text = label;
  if (!text) { try { text = new URL(url).hostname.replace(/^www\./, ""); } catch { text = url; } }
  return `<a href="${esc(url)}" target="_blank" rel="noopener noreferrer">${esc(text)}</a>`;
}

const section = (title, body) =>
  body ? `<section class="detail-section"><h2>${esc(title)}</h2>${body}</section>` : "";

function warningItem(w) {
  if (typeof w === "string") {
    return `<li class="warning-item"><p class="warning-item__name"><span aria-hidden="true">\u26a0\ufe0f</span> Warning sign</p><p>${esc(w)}</p></li>`;
  }
  return `
    <li class="warning-item">
      <p class="warning-item__name"><span aria-hidden="true">\u26a0\ufe0f</span> Warning sign${w.name ? `: ${esc(w.name)}` : ""}</p>
      ${w.message ? `<p>${esc(w.message)}</p>` : ""}
      ${w.sourceUrl || w.sourceLabel ? `<p class="note">Source: ${sourceLink(w.sourceUrl, w.sourceLabel)}</p>` : ""}
    </li>`;
}

/**
 * Maps a backend life-stage slug (e.g. "reproductive-age") to a display label.
 * Reverse of LIFE_STAGE_BACKEND_MAP.
 */
const BACKEND_STAGE_LABELS = {
  "teen":              "Teen",
  "adult":             "Adult",
  "reproductive-age":  "Reproductive age",
  "perimenopause":     "Perimenopause",
  "postmenopause":     "Postmenopause",
};
const stageLabel = (slug) => BACKEND_STAGE_LABELS[slug] || humanize(slug);

function render(c) {
  document.title = `${c.name || "Condition"} \u2013 HeardYou`;

  // ── field mapping: backend ConditionDetailDto ────────────────────────────
  // c.medicalDescription   → overview/detailed description
  // c.conventionalPresentation → textbook ("How it's usually described")
  // c.femalePresentation   → her version ("How it may present in women")
  // c.documentedPresentations  → list of documented presentations (formerly recognitionGaps)
  // c.educationalGuidance  → context / good-to-know text
  // c.symptoms             → ConditionSymptomDto[] {symptom:{name,...}, overlapWeight, evidenceNote}
  // c.commonlyMistakenFor  → list of conditions it's confused with
  // c.clinicianQuestions   → list of questions for clinician
  // c.suggestedTests       → list of suggested tests
  // c.timeframeAdvice      → when to seek help
  // c.sources              → SourceDto[] {url, title, publisher, ...}  (title not label)
  // c.lifeStages           → Set<String> of backend slugs (teen, reproductive-age, etc.)
  // c.bodySystem           → body system string

  const overview     = asList(c.medicalDescription || c.summary);
  const textbook     = asList(c.conventionalPresentation);
  const women        = asList(c.femalePresentation);
  const docPres      = asList(c.documentedPresentations);
  const guidance     = asList(c.educationalGuidance);
  const mistaken     = asList(c.commonlyMistakenFor);
  const questions    = asList(c.clinicianQuestions);
  const tests        = asList(c.suggestedTests);

  // symptoms: ConditionSymptomDto[] { symptom: SymptomSummaryDto, overlapWeight, evidenceNote }
  const symptomNames = Array.isArray(c.symptoms)
    ? c.symptoms.map((cs) => cs.symptom ? cs.symptom.name : (typeof cs === "string" ? cs : "")).filter(Boolean)
    : [];

  // lifeStages: backend sends backend slugs (teen, reproductive-age, etc.)
  const stages = (Array.isArray(c.lifeStages) ? [...c.lifeStages] : []).map(stageLabel);

  // warning signs: not in ConditionDetailDto (they come via check-in response).
  // This section is intentionally omitted on the condition detail page.
  const warnings = [];

  // sources: { url, title, publisher, ... } — use title as the link label
  const sources = Array.isArray(c.sources) ? c.sources : [];

  // ── textbook vs. her comparison ─────────────────────────────────────────
  const compare =
    textbook.length || women.length
      ? `<div class="compare ${textbook.length && women.length ? "compare--two" : ""}">
          ${textbook.length ? `<div class="compare__col compare__col--textbook"><h3>How it\u2019s usually described</h3>${paragraphs(textbook)}</div>` : ""}
          ${women.length ? `<div class="compare__col compare__col--her"><h3>How it may present in women</h3>${paragraphs(women)}</div>` : ""}
        </div>`
      : "";

  // ── good-to-know: documented presentations + educational guidance ────────
  const contextBody =
    (guidance.length ? paragraphs(guidance) : "") +
    (stages.length ? `<p><strong>Life stages most relevant:</strong> ${esc(stages.join(", "))}</p>` : "") +
    (mistaken.length ? `<p><strong>Commonly confused with:</strong> ${esc(mistaken.join(", "))}</p>` : "") +
    (c.avgDiagnosisDelay ? `<p><strong>Average diagnosis delay:</strong> ${esc(c.avgDiagnosisDelay)}</p>` : "") +
    (c.specialistType ? `<p><strong>Relevant specialist:</strong> ${esc(c.specialistType)}</p>` : "") +
    (c.timeframeAdvice ? `<p><strong>When to seek help:</strong> ${esc(c.timeframeAdvice)}</p>` : "");

  // ── sources ──────────────────────────────────────────────────────────────
  const sourceBody = sources.length
    ? `<ul class="source-list">${sources
        .map((s) => {
          // SourceDto has url and title (not label)
          const url   = typeof s === "string" ? s : s.url;
          const label = typeof s === "string" ? null : (s.title || s.label || s.name);
          return `<li>${sourceLink(url, label)}</li>`;
        })
        .join("")}</ul>`
    : "";

  const hasContent = overview.length || compare || symptomNames.length || docPres.length || contextBody || warnings.length;

  root.innerHTML = `
    ${c.bodySystem ? `<span class="ccard__tag">${esc(humanize(c.bodySystem))}</span>` : ""}
    <h2 class="page-title">${esc(c.name || "Condition")}</h2>

    ${section("Overview", paragraphs(overview))}
    ${section("Textbook vs. her version", compare)}
    ${section("Documented presentations", docPres.length ? paragraphs(docPres) : "")}
    ${section("Commonly reported symptoms", symptomNames.length ? bullets(symptomNames) : "")}
    ${section("Good to know", contextBody)}
    ${section("Tests you could ask about", tests.length ? bullets(tests) : "")}
    ${section("Questions for your clinician", questions.length ? bullets(questions) : "")}

    ${warnings.length
      ? `<section class="warning-box detail-section" aria-labelledby="warn-title">
           <h2 id="warn-title">Published warning signs</h2>
           <ul class="warning-list">${warnings.map(warningItem).join("")}</ul>
         </section>`
      : ""}

    ${hasContent ? "" : `<p class="panel">Detailed information for this condition isn\u2019t available yet.</p>`}
    ${section("Sources", sourceBody)}

    <p class="note">Educational information only. Differences are shown only where reliable sources document them.</p>
    <div class="detail-cta">
      <a class="btn" href="checkin.html">Start symptom check-in</a>
      <a class="btn btn--ghost" href="know.html">Back to all conditions</a>
    </div>`;
}

function showProblem(message) {
  root.innerHTML = `
    <div class="error-box" role="alert"><p>${esc(message)}</p></div>
    <p><a class="btn btn--ghost" href="know.html">Back to all conditions</a></p>`;
}

async function start() {
  const slug = new URLSearchParams(window.location.search).get("slug") || "";
  if (!/^[a-z0-9-]+$/i.test(slug)) {
    showProblem("We couldn\u2019t tell which condition you wanted. Please choose one from the list.");
    return;
  }
  try {
    render(await getCondition(slug));
  } catch (err) {
    showProblem(err.message || "We couldn\u2019t load this condition. Please try again.");
  }
}

start();