import { CONFIG } from "../config.js";
import { ApiError, getJson } from "./api.js";
import { MOCK_CONDITIONS, mockDetail } from "../mocks/mockConditions.js";

// GET /api/conditions?system=&stage=
export async function listConditions({ system = "", stage = "" } = {}) {
  if (CONFIG.USE_MOCK) {
    return MOCK_CONDITIONS.filter(
      (c) => (!system || c.system === system) && (!stage || c.stages.includes(stage))
    );
  }
  const params = new URLSearchParams();
  if (system) params.set("system", system);
  if (stage) params.set("stage", stage);
  const qs = params.toString();
  return getJson(`/conditions${qs ? `?${qs}` : ""}`);
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
  return getJson(`/search?q=${encodeURIComponent(query)}`);
}