import { CONFIG } from "../config.js";

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

function defaultMessage(status) {
  if (status === 400) return "Some of your input wasn't valid. Please check it and try again.";
  if (status === 404) return "We couldn't find that information.";
  if (status >= 500) return "Something went wrong on our side. Please try again in a moment.";
  return "Something went wrong. Please try again.";
}

// Single place for all requests. Never log request or response bodies (health data).
export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(CONFIG.API_BASE_URL + path, options);
  } catch {
    throw new ApiError("Can't reach the server. Check your connection and try again.", 0);
  }
  if (!response.ok) {
    let detail = "";
    try {
      const body = await response.json();
      detail = body.message || "";
    } catch {
      /* response was not JSON */
    }
    throw new ApiError(detail || defaultMessage(response.status), response.status);
  }
  return response;
}

export async function getJson(path) {
  const response = await request(path);
  return response.json();
}

export function postJson(path, body) {
  return request(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}