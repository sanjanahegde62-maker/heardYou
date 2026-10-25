export const LIFE_STAGES = [
  { value: "TEEN",                  label: "Teen" },
  { value: "ADULT_20_39",           label: "Adult, 20\u201339" },
  { value: "PREGNANCY_POSTPARTUM",  label: "Pregnancy / postpartum" },
  { value: "PERIMENOPAUSE_40S",     label: "Perimenopause (around 40s)" },
  { value: "MENOPAUSE_50_PLUS",     label: "Menopause (50+)" },
];

/**
 * Maps each frontend life-stage value to the slug the backend accepts.
 * Backend allowed values: teen | adult | reproductive-age | perimenopause | postmenopause
 *
 * PREGNANCY_POSTPARTUM has no exact backend equivalent; "reproductive-age" is the
 * closest category the backend supports and is used as the mapping.
 */
export const LIFE_STAGE_BACKEND_MAP = {
  TEEN:                 "teen",
  ADULT_20_39:          "adult",
  PREGNANCY_POSTPARTUM: "reproductive-age",
  PERIMENOPAUSE_40S:    "perimenopause",
  MENOPAUSE_50_PLUS:    "postmenopause",
};

export const DURATIONS = [
  { value: "UNDER_1_WEEK", label: "Under 1 week" },
  { value: "1_TO_4_WEEKS", label: "1\u20134 weeks" },
  { value: "OVER_1_MONTH", label: "Over 1 month" },
];

export const MAX_SYMPTOMS = 15;

// Returns a list of plain-language error messages. Empty list = valid.
export function validateCheckin({ lifeStage, symptoms }) {
  const errors = [];

  if (!LIFE_STAGES.some((s) => s.value === lifeStage)) {
    errors.push("Please choose your life stage.");
  }

  if (!Array.isArray(symptoms) || symptoms.length === 0) {
    errors.push("Please add at least one symptom.");
    return errors;
  }

  if (symptoms.length > MAX_SYMPTOMS) {
    errors.push(`You can add up to ${MAX_SYMPTOMS} symptoms.`);
  }

  const seen = new Set();
  symptoms.forEach((s) => {
    const name = s.name || s.slug;
    if (seen.has(s.slug)) errors.push(`"${name}" was added more than once.`);
    seen.add(s.slug);

    if (!Number.isInteger(s.severity) || s.severity < 1 || s.severity > 5) {
      errors.push(`Choose how strong "${name}" is (1 to 5).`);
    }
    if (!DURATIONS.some((d) => d.value === s.duration)) {
      errors.push(`Choose how long you've had "${name}".`);
    }
  });

  return errors;
}

// Only these fields are sent to the backend (no names, no extras).
// lifeStage is translated from the frontend enum value to the backend slug.
export function toCheckinPayload({ lifeStage, symptoms }) {
  return {
    lifeStage: LIFE_STAGE_BACKEND_MAP[lifeStage] || lifeStage,
    symptoms: symptoms.map(({ slug, severity, duration }) => ({ slug, severity, duration })),
  };
}
