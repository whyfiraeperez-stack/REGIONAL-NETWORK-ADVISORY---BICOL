const SPREADSHEET_ID = "1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const PUBLISHED_ID = "2PACX-1vTojZur9YkReg6paEkkqvsNscFzIW1dH7WwYfG7NqJSw-0xAEUwRJse_ILc1qLXidGK8Sno5ADAbhTo";
const TABS = [
  { name: "DATABASE", gid: "564222368" },
  { name: "OUTAGE ADVISORY", gid: "504737769" },
  { name: "DAILY REPORT", gid: "916324578" },
  { name: "BACKBONE LIST", gid: "1526261613" },
  { name: "GNOC", gid: "1898344567" },
  { name: "NAP LOCATOR", gid: "1924056980" }
];
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store, max-age=0, must-revalidate", "X-Content-Type-Options": "nosniff" }
});
function parseCsv(s) {
  const rows=[]; let row=[], cell="", quoted=false;
  for(let i=0;i<s.length;i++) {
    const c=s[i];
    if(c === '"') { if(quoted && s[i+1] === '"') { cell+='"'; i++; } else quoted=!quoted; }
    else if(c === "," && !quoted) { row.push(cell); cell=""; }
    else if((c === "\n" || c === "\r") && !quoted) { if(c === "\r" && s[i+1] === "\n") i++; row.push(cell); if(row.some(x=>x.trim())) rows.push(row); row=[]; cell=""; }
    else cell+=c;
  }
  if(cell.length || row.length) { row.push(cell); if(row.some(x=>x.trim())) rows.push(row); }
  return rows;
}
function rowsToSheet(name, matrix) {
  if(!matrix.length) return { name, found:false, headers:[], rows:[], error:"Empty or unavailable tab" };
  const headers=matrix[0].map((h,i)=>String(h||"COLUMN_"+(i+1)).trim());
  const rows=matrix.slice(1).map((cells,i)=>{
    const record={_rowNumber:i+2,...(name==="DAILY REPORT"?{_cells:cells.slice()}:{})};
    headers.forEach((h,j)=>record[h]=cells[j]??"");
    return record;
  }).filter((r,i)=>matrix[i+1].some(v=>String(v??"").trim()));
  return { name, found:true, headers, rows };
}
async function readTabCsv(tab) {
  const url="https://docs.google.com/spreadsheets/d/e/"+PUBLISHED_ID+"/pub?gid="+tab.gid+"&single=true&output=csv&_="+Date.now();
  const response=await fetch(url,{headers:{"Accept":"text/csv"},cache:"no-store",redirect:"follow"});
  const body=await response.text();
  if(!response.ok || /<html|accounts\.google\.com|sign in|google\.visualization\.Query\.setResponse/i.test(body.slice(0,500)))
    return {name:tab.name,found:false,headers:[],rows:[],error:"Published CSV unavailable for gid "+tab.gid};
  return rowsToSheet(tab.name,parseCsv(body));
}
async function readFromAppsScript(env) {
  const endpoint=env.GOOGLE_APPS_SCRIPT_URL, token=env.DASHBOARD_API_TOKEN;
  if(!endpoint || !token) return null;
  const response=await fetch(endpoint,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({token}),cache:"no-store"});
  const body=await response.json();
  if(!response.ok || !body.ok || !Array.isArray(body.sheets)) throw new Error(body.error||"Apps Script returned an invalid response");
  return body;
}
export async function onRequestGet({ request, env }) {
  try {
    const url=new URL(request.url);
    const requested=String(url.searchParams.get("sheet")||"DATABASE").trim().toUpperCase();
    const tab=TABS.find(item=>item.name.toUpperCase()===requested);
    if(!tab) return json({ok:false,error:"Unknown worksheet. Request one of: "+TABS.map(t=>t.name).join(", ")},400);
    let data, source="Published Google Sheets CSV", appsScriptError="";
    try {
      const full=await readFromAppsScript(env);
      if(full) {
        const match=(full.sheets||[]).find(s=>String(s.name||"").trim().toUpperCase()===requested);
        if(match) { data={...full,sheets:[match]}; source="Authenticated Google Apps Script"; }
        else throw new Error("Apps Script response did not include worksheet "+tab.name);
      }
    } catch(error) { appsScriptError=error.message||"Apps Script read failed"; console.error("Apps Script read failed; using published CSV:",appsScriptError); }
    if(!data) {
      const sheet=await readTabCsv(tab);
      data={ok:true,spreadsheetId:SPREADSHEET_ID,generatedAt:new Date().toISOString(),sheets:[sheet]};
      if(appsScriptError) source="Published Google Sheets CSV (Apps Script fallback)";
    }
    const sheets=(data.sheets||[]).map(s=>({...s,headers:Array.isArray(s.headers)?s.headers:[],rows:(Array.isArray(s.rows)?s.rows:[]).map(row=>{
      if(tab.name==="DAILY REPORT") return row;
      const {_cells,...compactRow}=row||{};
      return compactRow;
    })}));
    if(!sheets.some(s=>s.found)) {
      const failed=sheets[0]||{};
      return json({ok:false,error:failed.error||("Could not read worksheet "+tab.name+". Confirm it is published and the worksheet gid is correct."),worksheet:tab.name,sheets},502);
    }
    const page=Math.max(1,Number.parseInt(url.searchParams.get("page")||"1",10)||1);
    const limit=Math.min(500,Math.max(50,Number.parseInt(url.searchParams.get("limit")||"250",10)||250));
    const fullRows=sheets[0].rows||[], totalRows=fullRows.length;
    const pageRows=fullRows.slice((page-1)*limit,page*limit);
    const payload={...data,ok:true,source:data.source||source,worksheet:tab.name,generatedAt:data.generatedAt||new Date().toISOString(),sheets:[{...sheets[0],rows:pageRows}],pagination:{page,limit,totalRows,totalPages:Math.max(1,Math.ceil(totalRows/limit)),hasMore:page*limit<totalRows}};
    const bytes=new TextEncoder().encode(JSON.stringify(payload)).length;
    if(bytes>5500000) return json({ok:false,error:"Page "+page+" of worksheet "+tab.name+" is too large ("+bytes+" bytes). Reduce the page size.",worksheet:tab.name,bytes,limit},413);
    return json(payload);
  } catch(error) {
    console.error("Google Sheets sync failed:",error);
    return json({ok:false,error:"Google Sheets sync failed.",details:error.message},502);
  }
}
export async function onRequest({ request, env }) {
  if(request.method!=="GET") return new Response("Method Not Allowed",{status:405,headers:{Allow:"GET"}});
  return onRequestGet({request,env});
}
