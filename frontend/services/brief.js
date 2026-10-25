import { CONFIG } from "../config.js";
import { ApiError, postJson } from "./api.js";

const FILENAME = "heardyou-health-brief.pdf";

export async function downloadBrief({ lifeStage, symptoms, selectedSlugs, displayName }) {
  // The PDF is made by the backend. We never fake a medical document in the browser.
  if (CONFIG.USE_MOCK) {
    throw new ApiError(
      "The PDF is created by the backend. Set USE_MOCK to false in config.js once the backend is running.",
      0
    );
  }

  const payload = {
    lifeStage,
    symptoms: symptoms.map(({ slug, severity, duration }) => ({ slug, severity, duration })),
    selectedSlugs,
    displayName: displayName || "",
  };

  const response = await postJson("/brief", payload);

  const type = response.headers.get("Content-Type") || "";
  if (!type.includes("application/pdf")) {
    throw new ApiError("We didn't get a PDF back from the server. Please try again.", response.status);
  }

  const blob = await response.blob();            // binary, never parsed as JSON
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = FILENAME;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000); // release the temporary link
}