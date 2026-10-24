import { NEWS } from "../mocks/newsData.js";

// Escape text before putting it in HTML (important once real data arrives)
const esc = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const isLink = (u) => typeof u === "string" && /^https?:\/\//i.test(u);

const bow = (extra = "") =>
  `<svg class="bow ${extra}" viewBox="0 0 64 40" aria-hidden="true" focusable="false"><use href="#bow"></use></svg>`;

function cardHtml(item, i) {
  return `
    <li>
      <button type="button" class="rcard" data-index="${i}" aria-haspopup="dialog">
        <span class="rcard__script">Health News</span>
        <span class="rcard__meta"><span>No. ${esc(item.no)}</span><span>${esc(item.date)}</span></span>
        <span class="rcard__tag">♡ ${esc(item.tag)}</span>
        <span class="rcard__title">${esc(item.title)}</span>
        <span class="rcard__summary">${esc(item.summary)}</span>
        <span class="rcard__more">Open receipt ›</span>
        <span class="rcard__barcode" aria-hidden="true"></span>
      </button>
    </li>`;
}

function receiptHtml(item) {
  const points = item.points.map((p) => `<li>${esc(p)}</li>`).join("");
  const link = isLink(item.url)
    ? `<a class="receipt__link" href="${esc(item.url)}" target="_blank" rel="noopener noreferrer">Read the full story ↗</a>`
    : `<span class="receipt__link receipt__link--off">Link will be added</span>`;

  return `
    <div class="receipt">
      <div class="receipt__top">
        ${bow()}
        <h3 class="receipt__script">Health News</h3>
        <button type="button" class="receipt__close" aria-label="Close">✕</button>
      </div>
      <p class="receipt__thanks">Thank you for reading!</p>
      <div class="receipt__row"><span>No.: ${esc(item.no)}</span><span>Date: ${esc(item.date)}</span></div>
      <div class="receipt__cols"><span>Story</span><span>♡ ${esc(item.tag)}</span></div>
      <h4 class="receipt__title" id="receipt-title">${esc(item.title)}</h4>
      <p class="receipt__summary">${esc(item.summary)}</p>
      <ol class="receipt__points">${points}</ol>
      <div class="receipt__total"><span class="receipt__script-sm">Source:</span><span>${esc(item.source)}</span></div>
      <p class="receipt__notify">Want more? Read the full story:</p>
      ${link}
      <div class="receipt__barcode" aria-hidden="true"></div>
      <div class="receipt__bottom">${bow("bow--flip")}${bow("bow--flip-x")}</div>
      <p class="receipt__disclaimer">Educational information, not medical advice. If you're worried or something feels very wrong, seek care now. In an emergency, call your local emergency number (112 in India).</p>
    </div>`;
}

const list = document.getElementById("news-list");
const dialog = document.getElementById("receipt-dialog");

if (list && dialog) {
  list.innerHTML = NEWS.map(cardHtml).join("");

  list.addEventListener("click", (e) => {
    const btn = e.target.closest(".rcard");
    if (!btn) return;
    dialog.innerHTML = receiptHtml(NEWS[Number(btn.dataset.index)]);
    dialog.setAttribute("aria-labelledby", "receipt-title");
    dialog.showModal();
  });

  // Close with the X button or by clicking the blurred background. Esc works automatically.
  dialog.addEventListener("click", (e) => {
    if (e.target === dialog || e.target.closest(".receipt__close")) dialog.close();
  });
}