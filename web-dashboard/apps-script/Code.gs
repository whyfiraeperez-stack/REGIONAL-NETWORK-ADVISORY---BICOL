const SPREADSHEET_ID = '1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg';
const SHEET_NAMES = ['DATABASE', 'DAILY REPORT', 'BACKBONE LIST', 'OUTAGE ADVISORY'];
const HEADER_ROW = 1;
function doPost(e) {
  try {
    const body = JSON.parse(e && e.postData && e.postData.contents || '{}');
    const expected = PropertiesService.getScriptProperties().getProperty('DASHBOARD_API_TOKEN');
    if (!expected || !body.token || !safeEqual_(expected, body.token)) return output_({ok:false,error:'Unauthorized'});
    const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    const sheets = SHEET_NAMES.map(name => {
      const sh = ss.getSheetByName(name);
      if (!sh) return {name,found:false,headers:[],rows:[]};
      const values = sh.getDataRange().getDisplayValues();
      if (values.length < HEADER_ROW) return {name,found:true,headers:[],rows:[]};
      const headers = values[HEADER_ROW - 1].map((v,i) => String(v || ('COLUMN_' + (i + 1))).trim());
      const rows = values.slice(HEADER_ROW).map((values,rowIndex) => {
        if (!values.some(v => String(v).trim() !== '')) return null;
        const record = {_rowNumber: rowIndex + HEADER_ROW + 1};
        headers.forEach((h,i) => record[h || ('COLUMN_' + (i + 1))] = values[i] || '');
        return record;
      }).filter(Boolean);
      return {name,found:true,headers,rows};
    });
    return output_({ok:true,spreadsheetId:SPREADSHEET_ID,generatedAt:new Date().toISOString(),sheets});
  } catch (err) { return output_({ok:false,error:String(err && err.message || err)}); }
}
function doGet() { return output_({ok:false,error:'Use the authenticated dashboard endpoint.'}); }
function safeEqual_(a,b) { a=String(a); b=String(b); let diff=a.length^b.length; for(let i=0,n=Math.max(a.length,b.length);i<n;i++) diff|=(a.charCodeAt(i)||0)^(b.charCodeAt(i)||0); return diff===0; }
function output_(obj) { return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON); }