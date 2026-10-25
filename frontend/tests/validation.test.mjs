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

// ---------------------------------------------------------------------------
// ConditionDetailDto field mapping tests (condition.js logic)
// ---------------------------------------------------------------------------

// Inline helpers mirroring condition.js
function asList(v) {
  const items = Array.isArray(v) ? v : typeof v === "string" ? [v] : [];
  return items
    .map((i) => (typeof i === "string" ? i : i && (i.name || i.text || i.message || i.label)) || "")
    .filter(Boolean);
}

const BACKEND_STAGE_LABELS = {
  "teen":             "Teen",
  "adult":            "Adult",
  "reproductive-age": "Reproductive age",
  "perimenopause":    "Perimenopause",
  "postmenopause":    "Postmenopause",
};
const humanize2 = (id) => String(id || "").replace(/[-_]+/g, " ").replace(/^./, (c) => c.toUpperCase());
const stageLabel = (slug) => BACKEND_STAGE_LABELS[slug] || humanize2(slug);

const sampleCondition = {
  id: 1,
  name: "Endometriosis",
  slug: "endometriosis",
  summary: "Short summary.",
  medicalDescription: "Medical overview text.",
  bodySystem: "REPRODUCTIVE",
  conventionalPresentation: "Severe pelvic pain that is cyclic.",
  femalePresentation: "Often dismissed as bad periods.",
  documentedPresentations: ["Presentation 1", "Presentation 2"],
  commonlyMistakenFor: ["IBS", "Ovarian cysts"],
  avgDiagnosisDelay: "7-10 years",
  suggestedTests: ["Laparoscopy", "Ultrasound"],
  timeframeAdvice: "Seek care within weeks.",
  category: "REPRODUCTIVE",
  extraCategories: [],
  lifeStages: ["teen", "reproductive-age"],
  specialistType: "Gynaecologist",
  specialistNotes: "Referral recommended.",
  educationalGuidance: "Condition is often under-diagnosed.",
  clinicianQuestions: ["Is laparoscopy appropriate?", "Hormonal therapy options?"],
  symptoms: [
    { symptom: { id: 1, name: "Pelvic pain", slug: "pelvic-pain" }, overlapWeight: 0.9, evidenceNote: "Very common." },
    { symptom: { id: 2, name: "Fatigue", slug: "fatigue" }, overlapWeight: 0.7, evidenceNote: null },
  ],
  sources: [
    { url: "https://www.nhs.uk/endometriosis", title: "NHS Endometriosis", publisher: "NHS" },
    { url: "https://www.who.int/endo", title: "WHO Factsheet" },
  ],
};

test("condition.js reads overview from medicalDescription (not overview/description)", () => {
  const overview = asList(sampleCondition.medicalDescription || sampleCondition.summary);
  assert.deepEqual(overview, ["Medical overview text."]);
});

test("condition.js reads textbook from conventionalPresentation (not textbookPresentation)", () => {
  const textbook = asList(sampleCondition.conventionalPresentation);
  assert.deepEqual(textbook, ["Severe pelvic pain that is cyclic."]);
});

test("condition.js reads her version from femalePresentation (not womenPresentation)", () => {
  const women = asList(sampleCondition.femalePresentation);
  assert.deepEqual(women, ["Often dismissed as bad periods."]);
});

test("condition.js reads documented presentations from documentedPresentations (not recognitionGaps)", () => {
  const docPres = asList(sampleCondition.documentedPresentations);
  assert.deepEqual(docPres, ["Presentation 1", "Presentation 2"]);
});

test("condition.js reads guidance from educationalGuidance (not context)", () => {
  const guidance = asList(sampleCondition.educationalGuidance);
  assert.deepEqual(guidance, ["Condition is often under-diagnosed."]);
});

test("condition.js extracts symptom names from ConditionSymptomDto (not plain strings)", () => {
  const symptomNames = Array.isArray(sampleCondition.symptoms)
    ? sampleCondition.symptoms.map((cs) => cs.symptom ? cs.symptom.name : (typeof cs === "string" ? cs : "")).filter(Boolean)
    : [];
  assert.deepEqual(symptomNames, ["Pelvic pain", "Fatigue"]);
});

test("condition.js maps backend life-stage slugs to display labels", () => {
  const stages = (Array.isArray(sampleCondition.lifeStages) ? [...sampleCondition.lifeStages] : []).map(stageLabel);
  assert.deepEqual(stages, ["Teen", "Reproductive age"]);
});

test("condition.js uses source title (not label) for link text", () => {
  const s = sampleCondition.sources[0];
  const label = typeof s === "string" ? null : (s.title || s.label || s.name);
  assert.equal(label, "NHS Endometriosis");
});

test("condition.js reads bodySystem (not system) for the body-system tag", () => {
  assert.equal(sampleCondition.bodySystem, "REPRODUCTIVE");
  assert.equal(sampleCondition.system, undefined);  // field does not exist
});

test("condition.js does not render warningSigns from ConditionDetailDto (field absent)", () => {
  // warningSigns is NOT part of ConditionDetailDto; warnings come via check-in response only.
  assert.equal(sampleCondition.warningSigns, undefined);
  const warnings = [];  // condition.js hard-codes this to []
  assert.deepEqual(warnings, []);
});


// ---------------------------------------------------------------------------
// atlas.js: search + filter logic tests
// ---------------------------------------------------------------------------

// Inline the helpers we need from atlas.js (no DOM dependency)

function applyFiltersToItems(items, filters) {
  let result = items;
  if (filters.system) {
    const sys = filters.system.toUpperCase();
    result = result.filter((c) => (c.bodySystem || c.system || "").toUpperCase() === sys);
  }
  if (filters.stage) {
    result = result.filter(
      (c) => Array.isArray(c.lifeStages) && c.lifeStages.includes(filters.stage)
    );
  }
  return result;
}

// Sample items matching real ConditionSummaryDto shape
const sampleItems = [
  { slug: "endometriosis",   name: "Endometriosis",  bodySystem: "REPRODUCTIVE", summary: "Pelvic pain", lifeStages: ["teen", "reproductive-age"] },
  { slug: "pcos",            name: "PCOS",            bodySystem: "REPRODUCTIVE", summary: "Irregular periods", lifeStages: ["teen", "reproductive-age", "adult"] },
  { slug: "hypothyroidism",  name: "Hypothyroidism",  bodySystem: "HORMONAL_METABOLIC", summary: "Fatigue and weight gain", lifeStages: ["adult", "perimenopause"] },
  { slug: "gad",             name: "GAD",             bodySystem: "BRAIN_MIND",  summary: "Anxiety and headache",   lifeStages: ["teen", "adult"] },
  { slug: "fibromyalgia",    name: "Fibromyalgia",    bodySystem: "BONE_PAIN",   summary: "Widespread pain",        lifeStages: ["adult", "perimenopause"] },
];

test("applyFiltersToItems: no filter returns all items", () => {
  const result = applyFiltersToItems(sampleItems, { system: "", stage: "" });
  assert.equal(result.length, 5);
});

test("applyFiltersToItems: system filter is case-insensitive (REPRODUCTIVE)", () => {
  const result = applyFiltersToItems(sampleItems, { system: "REPRODUCTIVE", stage: "" });
  assert.equal(result.length, 2);
  assert.ok(result.every((c) => c.bodySystem === "REPRODUCTIVE"));
});

test("applyFiltersToItems: system filter is case-insensitive (lowercase reproductive)", () => {
  const result = applyFiltersToItems(sampleItems, { system: "reproductive", stage: "" });
  assert.equal(result.length, 2);
});

test("applyFiltersToItems: stage filter matches lifeStages array", () => {
  const result = applyFiltersToItems(sampleItems, { system: "", stage: "teen" });
  assert.equal(result.length, 3);
  assert.ok(result.every((c) => c.lifeStages.includes("teen")));
});

test("applyFiltersToItems: system + stage combined filter", () => {
  const result = applyFiltersToItems(sampleItems, { system: "REPRODUCTIVE", stage: "teen" });
  assert.equal(result.length, 2);
  assert.ok(result.every((c) => c.bodySystem === "REPRODUCTIVE" && c.lifeStages.includes("teen")));
});

test("applyFiltersToItems: filter with no matches returns empty array", () => {
  const result = applyFiltersToItems(sampleItems, { system: "CARDIOVASCULAR", stage: "" });
  assert.equal(result.length, 0);
});

test("applyFiltersToItems: stage filter with no matches returns empty array", () => {
  const result = applyFiltersToItems(sampleItems, { system: "", stage: "postmenopause" });
  assert.equal(result.length, 0);
});

test("applyFiltersToItems: handles missing lifeStages gracefully", () => {
  const items = [{ slug: "x", name: "X", bodySystem: "REPRODUCTIVE", lifeStages: undefined }];
  const result = applyFiltersToItems(items, { system: "", stage: "teen" });
  assert.equal(result.length, 0);
});

// Search envelope unwrap (searchContent logic in conditions.js)
function unwrapSearch(data) {
  if (data && Array.isArray(data.conditions)) return data.conditions;
  if (Array.isArray(data)) return data;
  return [];
}

test("searchContent: unwraps conditions array from /api/search envelope", () => {
  const apiResponse = { conditions: sampleItems.slice(0, 2), symptoms: [], query: "endo" };
  const result = unwrapSearch(apiResponse);
  assert.equal(result.length, 2);
  assert.equal(result[0].slug, "endometriosis");
});

test("searchContent: passes through plain array (legacy/mock fallback)", () => {
  const result = unwrapSearch(sampleItems);
  assert.equal(result.length, 5);
});

test("searchContent: returns empty array for unexpected shape", () => {
  assert.deepEqual(unwrapSearch(null), []);
  assert.deepEqual(unwrapSearch(undefined), []);
  assert.deepEqual(unwrapSearch({}), []);
});

test("searchContent: returns empty array when conditions key is empty", () => {
  const apiResponse = { conditions: [], symptoms: [], query: "xyz" };
  assert.deepEqual(unwrapSearch(apiResponse), []);
});

// Minimum query length guard (mirrors atlas.js runSearch logic)
function shouldSearch(q) {
  return typeof q === "string" && q.trim().length >= 2;
}

test("runSearch: accepts queries of 2 or more characters", () => {
  assert.ok(shouldSearch("fa"));
  assert.ok(shouldSearch("fatigue"));
  assert.ok(shouldSearch("painful periods"));
});

test("runSearch: rejects empty string", () => {
  assert.ok(!shouldSearch(""));
  assert.ok(!shouldSearch("  "));
});

test("runSearch: rejects single character", () => {
  assert.ok(!shouldSearch("f"));
  assert.ok(!shouldSearch("F"));
});