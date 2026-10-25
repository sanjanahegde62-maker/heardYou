import { CONFIG } from "../config.js";
import { ApiError, getJson } from "./api.js";
import { MOCK_CONDITIONS, mockDetail } from "../mocks/mockConditions.js";

/**
 * GET /api/conditions?system=&stage=
 *
 * The backend returns a Spring Page object:
 *   { content: [...], totalElements: N, totalPages: N, ... }
 * We unwrap `.content` so callers receive a plain array.
 */
export async function listConditions({ system = "", stage = "" } = {}) {
  if (CONFIG.USE_MOCK) {
    return MOCK_CONDITIONS.filter(
      (c) => (!system || c.system === system) && (!stage || c.stages.includes(stage))
    );
  }
  const params = new URLSearchParams();
  if (system) params.set("system", system);
  if (stage)  params.set("stage", stage);
  const qs = params.toString();
  const result = await getJson(`/conditions${qs ? `?${qs}` : ""}`);
  // Unwrap Spring Page: { content: [...], totalElements, ... }
  if (result && Array.isArray(result.content)) return result.content;
  // Fallback: backend returned a plain array (should not happen in production).
  if (Array.isArray(result)) return result;
  return [];
}

// GET /api/conditions/{slug}
export async function getCondition(slug) {
  if (CONFIG.USE_MOCK) {
    const detail = mockDetail(slug);
    if (!detail) throw new ApiError("We couldn't find that condition.", 404);
    return detail;
  }
  return getJson(`/conditions/${encodeURIComponent(slug)}`);
}

// GET /api/search?q=
export async function searchContent(query) {
  if (CONFIG.USE_MOCK) {
    const term = query.trim().toLowerCase();
    return MOCK_CONDITIONS.filter(
      (c) => c.name.toLowerCase().includes(term) || c.systemLabel.toLowerCase().includes(term)
    ).map(({ slug, name, system, systemLabel, description }) => ({ slug, name, system, systemLabel, description }));
  }
  const data = await getJson(`/search?q=${encodeURIComponent(query)}`);
  // /api/search returns { conditions: [...], symptoms: [...], query: "..." }
  // The atlas shows only conditions from the search endpoint.
  if (data && Array.isArray(data.conditions)) return data.conditions;
  if (Array.isArray(data)) return data;
  return [];
}
