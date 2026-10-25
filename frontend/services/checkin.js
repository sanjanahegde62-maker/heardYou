import { CONFIG } from "../config.js";
import { postJson } from "./api.js";
import { MOCK_CHECKIN_RESULT } from "../mocks/mockData.js";
import { toCheckinPayload } from "../utils/validation.js";

// Mock is used ONLY when USE_MOCK is true. A real API failure never falls back to mock data.
export async function submitCheckin(state) {
  if (CONFIG.USE_MOCK) return structuredClone(MOCK_CHECKIN_RESULT);
  const response = await postJson("/checkin", toCheckinPayload(state));
  return response.json();
}