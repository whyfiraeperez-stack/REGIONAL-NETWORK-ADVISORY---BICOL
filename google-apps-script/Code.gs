/**
 * Bicol Network Monitoring — secure Google Sheets bridge.
 *
 * Deploy as a Web App:
 *   Execute as: Me
 *   Who has access: Anyone
 *
 * Store Script Properties:
 *   DASHBOARD_API_TOKEN = same random secret as Netlify DASHBOARD_API_TOKEN
 *   ALLOWED_EDIT_RANGES = JSON map of approved A1 ranges, e.g.
 *     {"OUTAGE ADVISORY":["B2:B200","D2:D200"],"GNOC":["A2:Z500"]}
 *
 * Only ranges explicitly listed in ALLOWED_EDIT_RANGES can be changed.
 */
const SPREADSHEET_ID = "1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const SHEET_GIDS = {
  "DATABASE": 564222368,
  "OUTAGE ADVISORY": 504737769,
  "DAILY REPORT": 916324578,
  "BACKBONE LIST": 1526261613,
  "GNOC": 1898344567,
  "NAP LOCATOR": 1924056980
};

function doPost(e) {
  try {
    const body = JSON.parse((e && e.postData && e.postData.contents) || "{}");
    const expected = PropertiesService.getScriptProperties().getProperty("DASHBOARD_API_TOKEN");
    if (!expected || !body.token || !constantTimeEquals_(String(body.token), expected)) {
      return json_({ ok: false, error: "Unauthorized" });
    }

    const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    const action = String(body.action || "read").toLowerCase();

    if (action === "read") {
      const requested = body.sheet ? [String(body.sheet).toUpperCase()] : Object.keys(SHEET_GIDS);
      const sheets = requested.map(name => {
        const sheet = sheetByName_(ss, name);
        if (!sheet) return { name, found: false, headers: [], rows: [], error: "Worksheet not found" };
        return readSheet_(sheet);
      });
      return json_({ ok: true, source: "Authenticated Google Apps Script", generatedAt: new Date().toISOString(), sheets });
    }

    if (action === "update") {
      const name = String(body.sheet || "").toUpperCase();
      const sheet = sheetByName_(ss, name);
      if (!sheet) return json_({ ok: false, error: "Worksheet not allowed" });

      const rangeA1 = String(body.range || "");
      const values = body.values;
      if (!rangeA1 || !Array.isArray(values) || !values.length || !values.every(Array.isArray)) {
        return json_({ ok: false, error: "Provide a range and a two-dimensional values array" });
      }

      if (name !== "DATABASE") {
        return json_({ ok: false, error: "Dashboard editing is enabled for DATABASE only" });
      }
      const allowed = JSON.parse(PropertiesService.getScriptProperties().getProperty("ALLOWED_EDIT_RANGES") || '{"DATABASE":["A2:AD50000"]}');
      const ranges = allowed[name] || [];
      const target = sheet.getRange(rangeA1);
      const targetA1 = target.getA1Notation();
      if (target.getRow() < 2 || !ranges.some(rule => isRangeInside_(sheet, target, String(rule)))) {
        return json_({ ok: false, error: "This cell range is not enabled for dashboard editing" });
      }
      const formulaColumns = formulaColumns_(sheet);
      for (let col = target.getColumn(); col <= target.getLastColumn(); col++) {
        if (formulaColumns.indexOf(col) !== -1) {
          return json_({ ok: false, error: "Update blocked: column " + columnLetter_(col) + " contains formulas and is protected" });
        }
      }
      if (target.getNumRows() !== values.length || target.getNumColumns() !== values[0].length ||
          !values.every(row => row.length === values[0].length)) {
        return json_({ ok: false, error: "Values dimensions do not match the selected range" });
      }

      const lock = LockService.getDocumentLock();
      lock.waitLock(10000);
      try {
        target.setValues(values);
        SpreadsheetApp.flush();
      } finally {
        lock.releaseLock();
      }
      return json_({ ok: true, action: "update", sheet: name, range: targetA1, rowsUpdated: target.getNumRows(), columnsUpdated: target.getNumColumns(), updatedAt: new Date().toISOString() });
    }

    return json_({ ok: false, error: "Unsupported action" });
  } catch (err) {
    console.error(err);
    return json_({ ok: false, error: String(err && err.message || err) });
  }
}

function sheetByName_(ss, name) {
  if (!Object.prototype.hasOwnProperty.call(SHEET_GIDS, name)) return null;
  return ss.getSheets().find(s => s.getSheetId() === SHEET_GIDS[name]) || null;
}

function readSheet_(sheet) {
  const values = sheet.getDataRange().getDisplayValues();
  if (!values.length) return { name: sheet.getName(), found: true, headers: [], rows: [] };
  const headers = values[0].map((v, i) => String(v || "COLUMN_" + (i + 1)).trim());
  const formulas = formulaColumns_(sheet);
  const editableColumns = headers.map((h, i) => ({ name: h, index: i + 1 }))
    .filter(col => sheet.getName() === "DATABASE" && formulas.indexOf(col.index) === -1)
    .map(col => col.name);
  const rows = values.slice(1).map((line, i) => {
    const row = { _rowNumber: i + 2 };
    headers.forEach((h, j) => row[h] = line[j] == null ? "" : line[j]);
    return row;
  }).filter(row => headers.some(h => String(row[h] || "").trim()));
  return { name: sheet.getName(), found: true, headers, rows, editableColumns };
}

function formulaColumns_(sheet) {
  const lastRow = sheet.getLastRow();
  const lastCol = sheet.getLastColumn();
  if (lastRow < 2 || lastCol < 1) return [];
  const formulas = sheet.getRange(2, 1, lastRow - 1, lastCol).getFormulas();
  const protectedCols = [];
  for (let c = 0; c < lastCol; c++) {
    if (formulas.some(row => row[c] !== "")) protectedCols.push(c + 1);
  }
  return protectedCols;
}

function columnLetter_(column) {
  let result = "";
  while (column > 0) {
    const rem = (column - 1) % 26;
    result = String.fromCharCode(65 + rem) + result;
    column = Math.floor((column - 1) / 26);
  }
  return result;
}

function isRangeInside_(sheet, target, allowedA1) {
  try {
    const allowed = sheet.getRange(allowedA1);
    return target.getRow() >= allowed.getRow() &&
      target.getColumn() >= allowed.getColumn() &&
      target.getLastRow() <= allowed.getLastRow() &&
      target.getLastColumn() <= allowed.getLastColumn();
  } catch (_) {
    return false;
  }
}

function constantTimeEquals_(a, b) {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

function json_(data) {
  return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}
