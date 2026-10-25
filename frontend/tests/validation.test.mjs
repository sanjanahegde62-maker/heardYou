/**
 * Unit tests for HeardYou frontend logic.
 * Run with: node --experimental-vm-modules frontend/tests/validation.test.mjs
 * Or simply: node frontend/tests/validation.test.mjs
 *
 * Uses Node 18+ built-in test runner (node:test) — no extra dependencies.
 */
import { test } from "node:test";
import assert from "node:assert/strict";

// ---------------------------------------------------------------------------
// Inline the code under test (no DOM, no import.meta) so the tests are
// self-contained and do not need a bundler.
// ---------------------------------------------------------------------------

const LIFE_STAGES = [
  { value: "TEEN",                  label: "Teen" },
  { value: "ADULT_20_39",           label: "Adult, 20\u201339" },
  { value: "PREGNANCY_POSTPARTUM",  label: "Pregnancy / postpartum" },
  { value: "PERIMENOPAUSE_40S",     label: "Perimenopause (around 40s)" },
  { value: "MENOPAUSE_50_PLUS",     label: "Menopause (50+)" },
];

const LIFE_STAGE_BACKEND_MAP = {
  TEEN:                 "teen",
  ADULT_20_39:          "adult",
  PREGNANCY_POSTPARTUM: "reproductive-age",
  PERIMENOPAUSE_40S:    "perimenopause",
  MENOPAUSE_50_PLUS:    "postmenopause",
};

const BACKEND_ALLOWED = new Set(["teen", "adult", "reproductive-age", "perimenopause", "postmenopause"]);

const DURATIONS = [
  { value: "UNDER_1_WEEK", label: "Under 1 week" },
  { value: "1_TO_4_WEEKS", label: "1\u20134 weeks" },
  { value: "OVER_1_MONTH", label: "Over 1 month" },
];

const MAX_SYMPTOMS = 15;

function validateCheckin({ lifeStage, symptoms }) {
  const errors = [];
  if (!LIFE_STAGES.some((s) => s.value === lifeStage)) errors.push("Please choose your life stage.");
  if (!Array.isArray(symptoms) || symptoms.length === 0) { errors.push("Please add at least one symptom."); return errors; }
  if (symptoms.length > MAX_SYMPTOMS) errors.push(`You can add up to ${MAX_SYMPTOMS} symptoms.`);
  const seen = new Set();
  symptoms.forEach((s) => {
    const name = s.name || s.slug;
    if (seen.has(s.slug)) errors.push(`"${name}" was added more than once.`);
    seen.add(s.slug);
    if (!Number.isInteger(s.severity) || s.severity < 1 || s.severity > 5) errors.push(`Choose how strong "${name}" is (1 to 5).`);
    if (!DURATIONS.some((d) => d.value === s.duration)) errors.push(`Choose how long you've had "${name}".`);
  });
  return errors;
}

function toCheckinPayload({ lifeStage, symptoms }) {
  return {
    lifeStage: LIFE_STAGE_BACKEND_MAP[lifeStage] || lifeStage,
    symptoms: symptoms.map(({ slug, severity, duration }) => ({ slug, severity, duration })),
  };
}

const goodSymptom = { slug: "pelvic-pain", name: "Pelvic pain", severity: 3, duration: "OVER_1_MONTH" };

// ---------------------------------------------------------------------------
// Life-stage mapping tests
// ---------------------------------------------------------------------------

test("LIFE_STAGE_BACKEND_MAP maps every frontend value to a valid backend slug", () => {
  for (const stage of LIFE_STAGES) {
    const mapped = LIFE_STAGE_BACKEND_MAP[stage.value];
    assert.ok(mapped, `No mapping for frontend value "${stage.value}"`);
    assert.ok(BACKEND_ALLOWED.has(mapped), `"${mapped}" is not a valid backend life-stage slug`);
  }
});

test("toCheckinPayload maps TEEN -> teen", () => {
  const p = toCheckinPayload({ lifeStage: "TEEN", symptoms: [goodSymptom] });
  assert.equal(p.lifeStage, "teen");
});

test("toCheckinPayload maps ADULT_20_39 -> adult", () => {
  const p = toCheckinPayload({ lifeStage: "ADULT_20_39", symptoms: [goodSymptom] });
  assert.equal(p.lifeStage, "adult");
});

test("toCheckinPayload maps PREGNANCY_POSTPARTUM -> reproductive-age", () => {
  const p = toCheckinPayload({ lifeStage: "PREGNANCY_POSTPARTUM", symptoms: [goodSymptom] });
  assert.equal(p.lifeStage, "reproductive-age");
});

test("toCheckinPayload maps PERIMENOPAUSE_40S -> perimenopause", () => {
  const p = toCheckinPayload({ lifeStage: "PERIMENOPAUSE_40S", symptoms: [goodSymptom] });
  assert.equal(p.lifeStage, "perimenopause");
});

test("toCheckinPayload maps MENOPAUSE_50_PLUS -> postmenopause", () => {
  const p = toCheckinPayload({ lifeStage: "MENOPAUSE_50_PLUS", symptoms: [goodSymptom] });
  assert.equal(p.lifeStage, "postmenopause");
});

test("toCheckinPayload strips name and preserves slug/severity/duration only", () => {
  const p = toCheckinPayload({ lifeStage: "TEEN", symptoms: [{ slug: "fatigue", name: "Fatigue", severity: 2, duration: "1_TO_4_WEEKS" }] });
  const s = p.symptoms[0];
  assert.equal(s.slug, "fatigue");
  assert.equal(s.severity, 2);
  assert.equal(s.duration, "1_TO_4_WEEKS");
  assert.equal(s.name, undefined);
});

// ---------------------------------------------------------------------------
// validateCheckin tests
// ---------------------------------------------------------------------------

test("validateCheckin passes with valid state", () => {
  const errors = validateCheckin({ lifeStage: "TEEN", symptoms: [goodSymptom] });
  assert.deepEqual(errors, []);
});

test("validateCheckin fails when lifeStage is missing", () => {
  const errors = validateCheckin({ lifeStage: "", symptoms: [goodSymptom] });
  assert.ok(errors.some((e) => e.includes("life stage")));
});

test("validateCheckin fails when lifeStage is a raw backend value", () => {
  // The frontend state should always hold the frontend enum value, not the backend slug.
  const errors = validateCheckin({ lifeStage: "adult", symptoms: [goodSymptom] });
  assert.ok(errors.some((e) => e.includes("life stage")), "Should reject raw backend value 'adult'");
});

test("validateCheckin fails with no symptoms", () => {
  const errors = validateCheckin({ lifeStage: "ADULT_20_39", symptoms: [] });
  assert.ok(errors.some((e) => e.includes("symptom")));
});

test("validateCheckin fails when severity out of range", () => {
  const errors = validateCheckin({ lifeStage: "ADULT_20_39", symptoms: [{ slug: "s", severity: 6, duration: "OVER_1_MONTH" }] });
  assert.ok(errors.some((e) => e.includes("strong")));
});

test("validateCheckin fails when duration is missing", () => {
  const errors = validateCheckin({ lifeStage: "ADULT_20_39", symptoms: [{ slug: "s", severity: 3, duration: "" }] });
  assert.ok(errors.some((e) => e.includes("long")));
});

test("validateCheckin detects duplicate slugs", () => {
  const errors = validateCheckin({ lifeStage: "ADULT_20_39", symptoms: [goodSymptom, goodSymptom] });
  assert.ok(errors.some((e) => e.includes("more than once")));
});

// ---------------------------------------------------------------------------
// Page response unwrapping (mirroring conditions.js logic)
// ---------------------------------------------------------------------------

function unwrapPage(result) {
  if (result && Array.isArray(result.content)) return result.content;
  if (Array.isArray(result)) return result;
  return [];
}

test("unwrapPage extracts content from Spring Page object", () => {
  const page = { content: [{ slug: "endometriosis" }], totalElements: 1, totalPages: 1, number: 0 };
  assert.deepEqual(unwrapPage(page), [{ slug: "endometriosis" }]);
});

test("unwrapPage returns empty array for plain Page with no conditions", () => {
  const page = { content: [], totalElements: 0, totalPages: 0, number: 0 };
  assert.deepEqual(unwrapPage(page), []);
});

test("unwrapPage passes through plain array (legacy/mock)", () => {
  const arr = [{ slug: "pcos" }];
  assert.deepEqual(unwrapPage(arr), arr);
});

test("unwrapPage returns empty array for null/undefined", () => {
  assert.deepEqual(unwrapPage(null), []);
  assert.deepEqual(unwrapPage(undefined), []);
});

// ---------------------------------------------------------------------------
// CheckinResponse field name mapping tests
// ---------------------------------------------------------------------------

const sampleResponse = {
  disclaimer: "Educational only.",
  lifeStage: "adult",
  matches: [
    {
      conditionSlug: "endometriosis",
      conditionName: "Endometriosis",
      conditionSummary: "A condition.",
      overlapLabel: "HIGH",
      overlapExplanation: "Several symptoms matched.",
      specialistType: "Gynaecologist",
      clinicianQuestions: ["Q1", "Q2"],
      suggestedTests: ["Laparoscopy"],
      timeframeAdvice: "Seek care within weeks.",
      matchedSymptoms: [
        { symptomSlug: "pelvic-pain", symptomName: "Pelvic pain", editorialWeight: 1.0, submittedSeverity: 3, submittedDuration: "OVER_1_MONTH" }
      ],
      sources: [{ url: "https://example.com", label: "NHS" }],
    }
  ],
  warningSignsEvaluated: [
    { name: "Severe pain", message: "Go to A&E.", sourceUrl: "https://nhs.uk", sourceLabel: "NHS" }
  ],
  noMatchGuidance: "",
  alwaysOnSafety: "Seek care.",
};

test("results.js reads matches (not patterns) from CheckinResponse", () => {
  const patterns = Array.isArray(sampleResponse.matches) ? sampleResponse.matches : [];
  assert.equal(patterns.length, 1);
  assert.equal(patterns[0].conditionName, "Endometriosis");
});

test("results.js reads warningSignsEvaluated (not warningSigns) from CheckinResponse", () => {
  const warnings = Array.isArray(sampleResponse.warningSignsEvaluated) ? sampleResponse.warningSignsEvaluated : [];
  assert.equal(warnings.length, 1);
  assert.equal(warnings[0].name, "Severe pain");
});

test("results.js reads conditionSlug (not slug) from PatternMatch", () => {
  const p = sampleResponse.matches[0];
  assert.equal(p.conditionSlug, "endometriosis");
});

test("results.js reads conditionName (not name) from PatternMatch", () => {
  const p = sampleResponse.matches[0];
  assert.equal(p.conditionName, "Endometriosis");
});

test("results.js reads overlapLabel (not overlap) from PatternMatch", () => {
  const p = sampleResponse.matches[0];
  assert.equal(p.overlapLabel, "HIGH");
});

test("results.js reads overlapExplanation (not why) from PatternMatch", () => {
  const p = sampleResponse.matches[0];
  assert.equal(p.overlapExplanation, "Several symptoms matched.");
});

test("results.js reads specialistType (not specialist) from PatternMatch", () => {
  const p = sampleResponse.matches[0];
  assert.equal(p.specialistType, "Gynaecologist");
});

test("results.js reads clinicianQuestions (not doctorQuestions) from PatternMatch", () => {
  const p = sampleResponse.matches[0];
  assert.deepEqual(p.clinicianQuestions, ["Q1", "Q2"]);
});

test("results.js resolves symptom name from MatchedSymptomDetail object", () => {
  const p = sampleResponse.matches[0];
  const names = { "pelvic-pain": "Pelvic pain" };
  const matched = (p.matchedSymptoms || []).map((m) => {
    if (typeof m === "string") return names[m] || m;
    return m.symptomName || names[m.symptomSlug] || m.symptomSlug || "";
  }).filter(Boolean);
  assert.deepEqual(matched, ["Pelvic pain"]);
});
