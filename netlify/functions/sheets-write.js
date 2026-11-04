const crypto = require("crypto");

module.exports.handler = async (event) => {
  const headers = { "Content-Type": "application/json", "Cache-Control": "no-store" };
  if (event.httpMethod !== "POST") {
    return { statusCode: 405, headers: { ...headers, Allow: "POST" }, body: JSON.stringify({ ok: false, error: "Method not allowed" }) };
  }

  const endpoint = process.env.GOOGLE_APPS_SCRIPT_URL;
  const token = process.env.DASHBOARD_API_TOKEN;
  if (!endpoint || !token) {
    return { statusCode: 503, headers, body: JSON.stringify({ ok: false, error: "Spreadsheet write integration is not configured. Set GOOGLE_APPS_SCRIPT_URL and DASHBOARD_API_TOKEN in Netlify." }) };
  }

  try {
    const input = JSON.parse(event.body || "{}");
    const sheet = String(input.sheet || "").toUpperCase();
    const allowedSheets = ["OUTAGE ADVISORY", "GNOC", "DATABASE", "BACKBONE LIST", "DAILY REPORT", "NAP LOCATOR"];
    if (!allowedSheets.includes(sheet)) {
      return { statusCode: 400, headers, body: JSON.stringify({ ok: false, error: "Unknown worksheet" }) };
    }
    if (!input.range || !Array.isArray(input.values)) {
      return { statusCode: 400, headers, body: JSON.stringify({ ok: false, error: "A range and values array are required" }) };
    }

    const payload = { action: "update", token, sheet, range: input.range, values: input.values };
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
      redirect: "follow",
      cache: "no-store"
    });
    const result = await response.json();
    if (!response.ok || !result.ok) {
      return { statusCode: response.status >= 400 ? response.status : 403, headers, body: JSON.stringify(result) };
    }
    return { statusCode: 200, headers, body: JSON.stringify(result) };
  } catch (error) {
    console.error("Spreadsheet write failed:", error);
    return { statusCode: 502, headers, body: JSON.stringify({ ok: false, error: "Could not update the Google Spreadsheet", details: error.message }) };
  }
};
