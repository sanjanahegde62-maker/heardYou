import { CONFIG } from "../config.js";
import { getJson } from "./api.js";
import { MOCK_SYMPTOMS } from "../mocks/mockData.js";

export async function searchSymptoms(query) {
  if (CONFIG.USE_MOCK) {
    const term = query.trim().toLowerCase();
    return MOCK_SYMPTOMS.filter((s) => s.name.toLowerCase().includes(term));
  }
  return getJson(`/symptoms?q=${encodeURIComponent(query)}`);
}