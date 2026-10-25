import { getCondition } from "../services/conditions.js";
import { LIFE_STAGES } from "../utils/validation.js";

const root = document.getElementById("detail");
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const humanize = (id) => String(id || "").replace(/[-_]+/g, " ").replace(/^./, (c) => c.toUpperCase());

// Accepts a string, an array of strings, or an array of {name|text|message}. Never invents content.
function asList(v) {
  const items = Array.isArray(v) ? v : typeof v === "string" ? [v] : [];
  return items
    .map((i) => (typeof i === "string" ? i : i && (i.name || i.text || i.message || i.label)) || "")
    .filter(Boolean);
}
const paragraphs = (list) => list.map((t) => `<p>${esc(t)}</p>`).join("");
const bullets = (list) => `<ul>${list.map((t) => `<li>${esc(t)}</li>`).join("")}</ul>`;

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
    return `<li class="warning-item"><p class="warning-item__name"><span aria-hidden="true">⚠</span> Warning sign</p><p>${esc(w)}</p></li>`;
  }
  return `
    <li class="warning-item">
      <p class="warning-item__name"><span aria-hidden="true">⚠</span> Warning sign${w.name ? `: ${esc(w.name)}` : ""}</p>
      ${w.message ? `<p>${esc(w.message)}</p>` : ""}
      ${w.sourceUrl || w.sourceLabel ? `<p class="note">Source: ${sourceLink(w.sourceUrl, w.sourceLabel)}</p>` : ""}
    </li>`;
}

function render(c) {
  document.title = `${c.name || "Condition"} · Textbook vs. Her`;

  const overview = asList(c.overview);
  const textbook = asList(c.textbookPresentation);
  const women = asList(c.womenPresentation);
  const symptoms = asList(c.symptoms);
  const gaps = asList(c.recognitionGaps);
  const context = asList(c.context);
  const stages = (Array.isArray(c.lifeStages) ? c.lifeStages : []).map(
    (v) => (LIFE_STAGES.find((s) => s.value === v) || {}).label || v
  );
  const warnings = Array.isArray(c.warningSigns) ? c.warningSigns.filter(Boolean) : [];
  const sources = Array.isArray(c.sources) ? c.sources : [];

  // Differences are shown only when the backend provides them.
  const compare =
    textbook.length || women.length
      ? `<div class="compare ${textbook.length && women.length ? "compare--two" : ""}">
          ${textbook.length ? `<div class="compare__col compare__col--textbook"><h3>How it's usually described</h3>${paragraphs(textbook)}</div>` : ""}
          ${women.length ? `<div class="compare__col compare__col--her"><h3>How it may present in women</h3>${paragraphs(women)}</div>` : ""}
        </div>`
      : "";

  const contextBody =
    (context.length ? paragraphs(context) : "") +
    (stages.length ? `<p><strong>Life stages mentioned:</strong> ${esc(stages.join(", "))}</p>` : "");

  const sourceBody = sources.length
    ? `<ul class="source-list">${sources
        .map((s) => `<li>${typeof s === "string" ? sourceLink(s) : sourceLink(s.url, s.label)}</li>`)
        .join("")}</ul>`
    : "";

  const hasContent = overview.length || compare || symptoms.length || gaps.length || contextBody || warnings.length;

  root.innerHTML = `
    ${c.system ? `<span class="ccard__tag">${esc(c.systemLabel || humanize(c.system))}</span>` : ""}
    <h2 class="page-title">${esc(c.name || "Condition")}</h2>

    ${section("Overview", paragraphs(overview))}
    ${section("Textbook vs. her", compare)}
    ${section("Commonly reported symptoms", symptoms.length ? bullets(symptoms) : "")}
    ${section("Good to know", contextBody)}
    ${section("Why it can be missed", gaps.length ? paragraphs(gaps) : "")}

    ${warnings.length
      ? `<section class="warning-box detail-section" aria-labelledby="warn-title">
           <h2 id="warn-title">Published warning signs</h2>
           <ul class="warning-list">${warnings.map(warningItem).join("")}</ul>
         </section>`
      : ""}

    ${hasContent ? "" : `<p class="panel">Detailed information for this condition isn't available yet.</p>`}
    ${section("Sources", sourceBody)}

    <p class="note">Educational information only. Differences are shown only where reliable sources document them.</p>
    <div class="detail-cta">
      <a class="btn" href="checkin.html">Start symptom check-in</a>
      <a class="btn btn--ghost" href="../index.html">Back to all conditions</a>
    </div>`;
}

function showProblem(message) {
  root.innerHTML = `
    <div class="error-box" role="alert"><p>${esc(message)}</p></div>
    <p><a class="btn btn--ghost" href="../index.html">Back to all conditions</a></p>`;
}

async function start() {
  const slug = new URLSearchParams(window.location.search).get("slug") || "";
  if (!/^[a-z0-9-]+$/i.test(slug)) {
    showProblem("We couldn't tell which condition you wanted. Please choose one from the list.");
    return;
  }
  try {
    render(await getCondition(slug));
  } catch (err) {
    showProblem(err.message || "We couldn't load this condition. Please try again.");
  }
}

start();