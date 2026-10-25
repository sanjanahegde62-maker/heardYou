import { NEWS } from "../mocks/newsData.js";

const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const isLink = (u) => typeof u === "string" && /^https?:\/\//i.test(u);
const tone = (i) => (i % 4) + 1; // cycles the four gradient themes
const reduceMotion = () => window.matchMedia("(prefers-reduced-motion: reduce)").matches;

// ---- small ticket on the board ----
function ticket(item, i) {
  return `
    <li>
      <div class="tk-wrap tk-wrap--${tone(i)}">
        <button type="button" class="tk" data-index="${i}" aria-haspopup="dialog">
          <span class="tk__top">
            <span class="tk__num">${esc(item.no)}</span>
            <span class="tk__tag">${esc(item.tag)}</span>
            <span class="tk__title">${esc(item.title)}</span>
            <span class="tk__summary">${esc(item.summary)}</span>
          </span>
          <span class="tk__stub">
            <span class="tk__date">${esc(item.date)}</span>
            <span class="tk__open">Open ticket ›</span>
            <span class="tk__barcode" aria-hidden="true"></span>
          </span>
        </button>
      </div>
    </li>`;
}

// ---- expanded (horizontal) ticket inside the dialog ----
function bigTicket(item, i) {
  const points = (item.points || []).map((p) => `<li>${esc(p)}</li>`).join("");
  const link = isLink(item.url)
    ? `<a class="tk__link" href="${esc(item.url)}" target="_blank" rel="noopener noreferrer">Read the news ↗</a>`
    : `<span class="tk__link tk__link--off">News link coming soon</span>`;

  return `
    <div class="tk-wrap tk-wrap--big tk-wrap--${tone(i)}">
      <div class="tk tk--big">
        <div class="tk__top tk__top--big">
          <div class="tk__bar">
            <span class="tk__num">No. ${esc(item.no)}</span>
            <button type="button" class="tk__close" aria-label="Close">✕</button>
          </div>
          <span class="tk__tag">${esc(item.tag)}</span>
          <h3 class="tk__title" id="tk-title">${esc(item.title)}</h3>
          <p class="tk__summary">${esc(item.summary)}</p>
          ${points ? `<ol class="tk__points">${points}</ol>` : ""}
          <p class="tk__disclaimer">Educational information, not medical advice. If you're worried or something feels very wrong, seek care now. In an emergency, call your local emergency number (112 in India).</p>
        </div>
        <div class="tk__stub tk__stub--big">
          <div class="tk__meta">
            <span class="tk__date">${esc(item.date)}</span>
            <span>Source: ${esc(item.source)}</span>
          </div>
          ${link}
          <span class="tk__barcode" aria-hidden="true"></span>
        </div>
      </div>
    </div>`;
}

const list = document.getElementById("news-list");
const dialog = document.getElementById("receipt-dialog");

if (list && dialog) {
  let opener = null;   // the small ticket that was clicked
  let closing = false;

  const morph = (from, to) => ({
    dx: from.left - to.left,
    dy: from.top - to.top,
    sx: from.width / to.width,
    sy: from.height / to.height,
  });

  // The small ticket expands into the big one (FLIP-style morph)
  function openTicket(btn) {
    const i = Number(btn.dataset.index);
    opener = btn;
    const from = btn.getBoundingClientRect();

    dialog.innerHTML = bigTicket(NEWS[i], i);
    dialog.setAttribute("aria-labelledby", "tk-title");
    dialog.showModal();
    if (reduceMotion()) return;

    const el = dialog.querySelector(".tk-wrap--big");
    const { dx, dy, sx, sy } = morph(from, el.getBoundingClientRect());
    el.animate(
      [
        { transformOrigin: "top left", transform: `translate(${dx}px, ${dy}px) scale(${sx}, ${sy})` },
        { transformOrigin: "top left", transform: "translate(0, 0) scale(1, 1)" },
      ],
      { duration: 380, easing: "cubic-bezier(0.2, 0.8, 0.2, 1)" }
    );
    // text fades in while the shape grows, so it never looks stretched
    dialog.querySelectorAll(".tk__top--big, .tk__stub--big").forEach((part) =>
      part.animate([{ opacity: 0 }, { opacity: 1 }], { duration: 240, delay: 150, easing: "ease-out", fill: "backwards" })
    );
  }

  // ...and shrinks back into the ticket it came from
  function closeTicket() {
    if (closing || !dialog.open) return;
    const el = dialog.querySelector(".tk-wrap--big");
    if (reduceMotion() || !el || !opener || !opener.isConnected) {
      dialog.close();
      return;
    }
    closing = true;
    const { dx, dy, sx, sy } = morph(opener.getBoundingClientRect(), el.getBoundingClientRect());
    const done = () => { dialog.close(); closing = false; };
    el.animate(
      [
        { transformOrigin: "top left", transform: "translate(0, 0) scale(1, 1)", opacity: 1 },
        { transformOrigin: "top left", transform: `translate(${dx}px, ${dy}px) scale(${sx}, ${sy})`, opacity: 0 },
      ],
      { duration: 280, easing: "cubic-bezier(0.4, 0, 0.2, 1)", fill: "forwards" }
    ).finished.then(done, done);
  }

  list.innerHTML = NEWS.map(ticket).join("");

  list.addEventListener("click", (e) => {
    const btn = e.target.closest(".tk");
    if (btn) openTicket(btn);
  });

  // X button, or a click on the dimmed background
  dialog.addEventListener("click", (e) => {
    if (e.target === dialog || e.target.closest(".tk__close")) closeTicket();
  });

  // Esc key: use the same shrink-back animation
  dialog.addEventListener("cancel", (e) => {
    e.preventDefault();
    closeTicket();
  });
}