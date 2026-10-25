// PLACEHOLDER catalogue: real condition names, but ALL descriptive text is placeholder.
// This is NOT medical content. Real, sourced content comes from the backend.
const PLACEHOLDER = "Placeholder text. Verified, sourced content comes from the backend.";
const ALL_STAGES = ["TEEN", "ADULT_20_39", "PREGNANCY_POSTPARTUM", "PERIMENOPAUSE_40S", "MENOPAUSE_50_PLUS"];

export const MOCK_CONDITIONS = [
  { slug: "heart-attack", name: "Heart attack and SCAD", system: "cardiovascular", systemLabel: "Heart and cardiovascular", stages: ALL_STAGES },
  { slug: "thyroid-disorders", name: "Thyroid disorders", system: "hormonal-metabolic", systemLabel: "Hormonal and metabolic", stages: ALL_STAGES },
  { slug: "pcos-pcod", name: "PCOS and PCOD", system: "hormonal-metabolic", systemLabel: "Hormonal and metabolic", stages: ALL_STAGES },
  { slug: "perimenopause", name: "Perimenopause", system: "hormonal-metabolic", systemLabel: "Hormonal and metabolic", stages: ["PERIMENOPAUSE_40S", "MENOPAUSE_50_PLUS"] },
  { slug: "endometriosis", name: "Endometriosis", system: "reproductive", systemLabel: "Reproductive and pelvic", stages: ALL_STAGES },
  { slug: "migraine", name: "Migraine", system: "neurological", systemLabel: "Brain and neurological", stages: ALL_STAGES },
  { slug: "ibs", name: "Irritable bowel syndrome (IBS)", system: "digestive", systemLabel: "Digestive", stages: ALL_STAGES },
  { slug: "pmdd", name: "PMDD", system: "mental-health", systemLabel: "Mental health", stages: ALL_STAGES },
  { slug: "perinatal-anxiety-ocd", name: "Perinatal anxiety and OCD", system: "mental-health", systemLabel: "Mental health", stages: ["PREGNANCY_POSTPARTUM"] },
  { slug: "adhd-autism-women", name: "ADHD and autism in women", system: "mental-health", systemLabel: "Mental health", stages: ALL_STAGES },
].map((c) => ({ ...c, description: PLACEHOLDER }));

export function mockDetail(slug) {
  const c = MOCK_CONDITIONS.find((x) => x.slug === slug);
  if (!c) return null;
  return {
    slug: c.slug,
    name: c.name,
    system: c.system,
    systemLabel: c.systemLabel,
    overview: PLACEHOLDER,
    textbookPresentation: PLACEHOLDER,
    womenPresentation: PLACEHOLDER,
    symptoms: ["Placeholder symptom one", "Placeholder symptom two"],
    recognitionGaps: [PLACEHOLDER],
    // one placeholder warning sign so you can see how the red section looks
    warningSigns:
      c.slug === "heart-attack"
        ? [{ name: "Example warning sign (placeholder)", message: PLACEHOLDER, sourceLabel: "Example source", sourceUrl: "https://example.org/source" }]
        : [],
    sources: [{ label: "Example source", url: "https://example.org/source" }],
  };
}