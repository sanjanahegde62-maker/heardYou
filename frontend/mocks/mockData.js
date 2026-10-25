// PLACEHOLDER data in the shape of the API spec. This is NOT real medical content.
// It is only used while config.js has USE_MOCK = true.

export const MOCK_SYMPTOMS = [
  { slug: "unusual-fatigue", name: "Unusual fatigue" },
  { slug: "nausea", name: "Nausea" },
  { slug: "pelvic-pain", name: "Pelvic pain" },
  { slug: "heavy-periods", name: "Heavy periods" },
  { slug: "brain-fog", name: "Brain fog" },
  { slug: "headache", name: "Headache" },
  { slug: "mood-changes", name: "Mood changes" },
  { slug: "bloating", name: "Bloating" },
  { slug: "jaw-or-back-pain", name: "Jaw or back pain" },
];

export const MOCK_CHECKIN_RESULT = {
  warningSigns: [
    {
      id: "example-warning",
      name: "Example warning sign (placeholder)",
      message: "Placeholder text. Real, sourced warning-sign wording comes from the backend.",
      sourceLabel: "Example source",
      sourceUrl: "https://example.org/source",
    },
  ],
  patterns: [
    {
      slug: "example-condition-1",
      name: "Example condition one",
      overlap: "MODERATE",
      score: 0.42,
      matchedSymptoms: ["unusual-fatigue", "nausea"],
      why: "Placeholder explanation of why this pattern appeared.",
      specialist: "Example specialist",
      suggestedTests: ["Example test to discuss with a clinician"],
      doctorQuestions: ["What could explain these symptoms?"],
      timeframeAdvice: "",
      sourceUrls: ["https://example.org/source"],
    },
    {
      slug: "example-condition-2",
      name: "Example condition two",
      overlap: "LOW",
      score: 0.24,
      matchedSymptoms: ["unusual-fatigue", "brain-fog"],
      why: "Placeholder explanation of why this pattern appeared.",
      specialist: "",
      suggestedTests: [],
      doctorQuestions: ["What else could explain this?"],
      timeframeAdvice: "",
      sourceUrls: ["https://example.org/source"],
    },
  ],
  disclaimer:
    "Educational information, not medical advice. If you're worried or something feels very wrong, seek care now. In an emergency, call your local emergency number (112 in India).",
};