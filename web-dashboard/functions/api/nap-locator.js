const SPREADSHEET_ID = "1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const PUBLISHED_ID = "2PACX-1vTojZur9YkReg6paEkkqvsNscFzIW1dH7WwYfG7NqJSw-0xAEUwRJse_ILc1qLXidGK8Sno5ADAbhTo";
const NAP_GID = "1924056980";
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store, max-age=0, must-revalidate",
    "X-Content-Type-Options": "nosniff"
  }
});
const norm = value => String(value ?? "").toLowerCase().replace(/[^a-z0-9]/g, "");
function parseCsv(source) {
  const rows = []; let row = [], cell = "", quoted = false;
  for (let i = 0; i < source.length; i++) {
    const c = source[i];
    if (c === '"') {
      if (quoted && source[i + 1] === '"') { cell += '"'; i++; }
      else quoted = !quoted;
    } else if (c === "," && !quoted) { row.push(cell); cell = ""; }
    else if ((c === "\n" || c === "\r") && !quoted) {
      if (c === "\r" && source[i + 1] === "\n") i++;
      row.push(cell); if (row.some(v => v.trim())) rows.push(row);
      row = []; cell = "";
    } else cell += c;
  }
  if (cell.length || row.length) { row.push(cell); if (row.some(v => v.trim())) rows.push(row); }
  return rows;
}
function pick(headers, row, aliases) {
  for (const alias of aliases) {
    const i = headers.findIndex(h => norm(h) === norm(alias));
    if (i >= 0 && String(row[i] ?? "").trim() !== "") return row[i];
  }
  for (const alias of aliases) {
    const i = headers.findIndex(h => norm(h).includes(norm(alias)));
    if (i >= 0 && String(row[i] ?? "").trim() !== "") return row[i];
  }
  return "";
}
function pickObject(row, aliases) {
  const keys = Object.keys(row || {}).filter(k => !k.startsWith("_"));
  for (const alias of aliases) {
    const key = keys.find(k => norm(k) === norm(alias));
    if (key && String(row[key] ?? "").trim() !== "") return row[key];
  }
  for (const alias of aliases) {
    const key = keys.find(k => norm(k).includes(norm(alias)));
    if (key && String(row[key] ?? "").trim() !== "") return row[key];
  }
  return "";
}
function normalizeRecords(rows, headers, objectRows = false) {
  const aliases = {
    napCode: ["NAP CODE", "NAP NAME", "NAP", "NODE CODE", "NAP ID", "DESCRIPTION", "NAME"],
    latitude: ["LATITUDE", "LAT", "GPS LATITUDE", "Y COORDINATE"],
    longitude: ["LONGITUDE", "LON", "LONG", "GPS LONGITUDE", "X COORDINATE"],
    noOfPorts: ["NO OF PORTS", "NUMBER OF PORTS", "PORTS", "PORT COUNT", "CAPACITY", "NAP PORTS"],
    area: ["AREA", "BARANGAY", "LOCATION", "ADDRESS", "SITE"],
    lcpCode: ["LCP CODE", "LCP", "LCP NAME", "LCP ID"]
  };
  const normalized = rows.map(row => {
    const get = key => objectRows ? pickObject(row, aliases[key]) : pick(headers, row, aliases[key]);
    return {
      napCode: String(get("napCode") ?? "").trim(),
      latitude: String(get("latitude") ?? "").trim(),
      longitude: String(get("longitude") ?? "").trim(),
      noOfPorts: String(get("noOfPorts") ?? "").trim(),
      area: String(get("area") ?? "").trim(),
      lcpCode: String(get("lcpCode") ?? "").trim()
    };
  }).filter(r => r.napCode && r.latitude !== "" && r.longitude !== "" &&
    Number.isFinite(Number(r.latitude)) && Number.isFinite(Number(r.longitude)) &&
    Number(r.latitude) >= -90 && Number(r.latitude) <= 90 &&
    Number(r.longitude) >= -180 && Number(r.longitude) <= 180);
  const unique = new Map();
  normalized.forEach(r => {
    r.latitude = Number(r.latitude);
    r.longitude = Number(r.longitude);
    r.noOfPorts = r.noOfPorts || "—";
    r.area = r.area || "Unknown area";
    r.lcpCode = r.lcpCode || ((r.napCode.match(/\bLP\s*\d+\b/i) || [])[0] || "—").replace(/\s+/g, " ").toUpperCase();
    unique.set(r.napCode, r);
  });
  return [...unique.values()];
}
async function readAppsScript(env) {
  if (!env.GOOGLE_APPS_SCRIPT_URL || !env.DASHBOARD_API_TOKEN) return null;
  const response = await fetch(env.GOOGLE_APPS_SCRIPT_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token: env.DASHBOARD_API_TOKEN, sheet: "NAP LOCATOR" }),
    cache: "no-store"
  });
  const body = await response.json();
  if (!response.ok || !body.ok || !Array.isArray(body.sheets)) {
    throw new Error(body.error || "Apps Script returned an invalid response");
  }
  const sheet = body.sheets.find(s => String(s.name || "").trim().toUpperCase() === "NAP LOCATOR");
  if (!sheet || !sheet.found || !Array.isArray(sheet.rows)) {
    throw new Error("Apps Script response did not include the NAP LOCATOR worksheet");
  }
  return {
    records: normalizeRecords(sheet.rows, sheet.headers || [], true),
    source: "Authenticated Google Apps Script"
  };
}
async function readPublishedCsv() {
  const url = "https://docs.google.com/spreadsheets/d/e/" + PUBLISHED_ID +
    "/pub?gid=" + NAP_GID + "&single=true&output=csv&_=" + Date.now();
  const response = await fetch(url, {
    headers: { "Accept": "text/csv" }, cache: "no-store", redirect: "follow"
  });
  const body = await response.text();
  if (!response.ok || /<html|accounts\.google\.com|sign in|google\.visualization\.Query\.setResponse/i.test(body.slice(0, 500))) {
    throw new Error("Published NAP LOCATOR CSV is unavailable. Confirm the worksheet is published to the web.");
  }
  const matrix = parseCsv(body);
  if (!matrix.length) throw new Error("NAP LOCATOR worksheet is empty or unavailable.");
  const headers = matrix[0].map((v, i) => String(v || "COLUMN_" + (i + 1)).trim());
  return {
    records: normalizeRecords(matrix.slice(1), headers, false),
    source: "Published Google Sheets CSV"
  };
}
export async function onRequest({ request, env }) {
  if (request.method !== "GET") return new Response("Method Not Allowed", {
    status: 405, headers: { Allow: "GET" }
  });
  try {
    let result, appsScriptError = "";
    try { result = await readAppsScript(env); }
    catch (error) {
      appsScriptError = error.message || "Apps Script read failed";
      console.error("NAP LOCATOR Apps Script read failed; trying published CSV:", appsScriptError);
    }
    if (!result) {
      result = await readPublishedCsv();
      if (appsScriptError) result.source += " (Apps Script fallback)";
    }
    if (!result.records.length) {
      return json({
        ok: false,
        error: "NAP LOCATOR was read but no valid rows with NAP code and coordinates were found."
      }, 502);
    }
    return json({
      ok: true,
      source: result.source,
      spreadsheetId: SPREADSHEET_ID,
      worksheet: "NAP LOCATOR",
      generatedAt: new Date().toISOString(),
      count: result.records.length,
      records: result.records
    });
  } catch (error) {
    console.error("NAP LOCATOR sync failed:", error);
    return json({
      ok: false,
      error: "NAP LOCATOR spreadsheet sync failed.",
      details: error.message
    }, 502);
  }
}
