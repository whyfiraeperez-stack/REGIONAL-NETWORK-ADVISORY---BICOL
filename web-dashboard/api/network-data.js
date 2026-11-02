const ID="1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const PUBLISHED_ID="2PACX-1vTojZur9YkReg6paEkkqvsNscFzIW1dH7WwYfG7NqJSw-0xAEUwRJse_ILc1qLXidGK8Sno5ADAbhTo";
const TABS=[
 {name:"DATABASE",gid:"564222368"},
 {name:"OUTAGE ADVISORY",gid:"504737769"},
 {name:"DAILY REPORT",gid:"916324578"},
 {name:"BACKBONE LIST",gid:"1526261613"},
 {name:"GNOC",gid:"1898344567"},
 {name:"NAP LOCATOR",gid:"1924056980"}
];
function parseCsv(s){
 const rows=[];let row=[],cell="",quoted=false;
 for(let i=0;i<s.length;i++){
  const c=s[i];
  if(c==='"'){if(quoted&&s[i+1]==='"'){cell+='"';i++;}else quoted=!quoted;}
  else if(c===","&&!quoted){row.push(cell);cell="";}
  else if((c==="\n"||c==="\r")&&!quoted){if(c==="\r"&&s[i+1]==="\n")i++;row.push(cell);if(row.some(x=>x.trim()))rows.push(row);row=[];cell="";}
  else cell+=c;
 }
 if(cell.length||row.length){row.push(cell);if(row.some(x=>x.trim()))rows.push(row);}
 return rows;
}
function rowsToSheet(name,matrix){
 if(!matrix.length)return {name,found:false,headers:[],rows:[],error:"Empty or unavailable tab"};
 const headers=matrix[0].map((h,i)=>String(h||"COLUMN_"+(i+1)).trim());
 const rows=matrix.slice(1).map((cells,i)=>{
  const record={_rowNumber:i+2,...(name==="DAILY REPORT"?{_cells:cells.slice()}:{})};
  headers.forEach((h,j)=>record[h]=cells[j]??"");
  return record;
 }).filter((r,i)=>matrix[i+1].some(v=>String(v??"").trim()));
 return {name,found:true,headers,rows};
}
async function readTabCsv(tab){
 const url="https://docs.google.com/spreadsheets/d/e/"+PUBLISHED_ID+"/pub?gid="+tab.gid+"&single=true&output=csv&_="+Date.now();
 const response=await fetch(url,{cache:"no-store",redirect:"follow"});
 const body=await response.text();
 if(!response.ok||/<html|accounts\.google\.com|sign in|google\.visualization\.Query\.setResponse/i.test(body.slice(0,500)))
  return {name:tab.name,found:false,headers:[],rows:[],error:"Published CSV unavailable for gid "+tab.gid};
 return rowsToSheet(tab.name,parseCsv(body));
}
async function readFromAppsScript(){
 const endpoint=process.env.GOOGLE_APPS_SCRIPT_URL;
 const token=process.env.DASHBOARD_API_TOKEN;
 if(!endpoint||!token)return null;
 const response=await fetch(endpoint,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({token}),cache:"no-store"});
 const body=await response.json();
 if(!response.ok||!body.ok||!Array.isArray(body.sheets))throw new Error(body.error||"Apps Script returned an invalid response");
 return body;
}
// Each request returns one worksheet only; the frontend loads tabs on demand.
module.exports=async function(req,res){
 res.setHeader("Cache-Control","no-store, max-age=0, must-revalidate");
 res.setHeader("X-Content-Type-Options","nosniff");
 if(req.method!=="GET"){res.setHeader("Allow","GET");return res.status(405).json({ok:false,error:"Method not allowed"});}
 try{
  const requested=String(req.query?.sheet||"DATABASE").trim().toUpperCase();
  const tab=TABS.find(item=>item.name.toUpperCase()===requested);
  if(!tab)return res.status(400).json({ok:false,error:"Unknown worksheet. Request one of: "+TABS.map(t=>t.name).join(", ")});
  let data,source="Published Google Sheets CSV",appsScriptError="";
  try{
   const full=await readFromAppsScript();
   if(full){
    const match=(full.sheets||[]).find(s=>String(s.name||"").trim().toUpperCase()===requested);
    if(match){data={...full,sheets:[match]};source="Authenticated Google Apps Script";}
    else throw new Error("Apps Script response did not include worksheet "+tab.name);
   }
  }catch(error){
   appsScriptError=error.message||"Apps Script read failed";
   console.error("Apps Script read failed; falling back to published CSV:",appsScriptError);
  }
  if(!data){
   const sheet=await readTabCsv(tab);
   data={ok:true,spreadsheetId:ID,generatedAt:new Date().toISOString(),sheets:[sheet]};
   if(appsScriptError)source="Published Google Sheets CSV (Apps Script fallback)";
  }
  const sheets=(data.sheets||[]).map(s=>({...s,headers:Array.isArray(s.headers)?s.headers:[],rows:(Array.isArray(s.rows)?s.rows:[]).map(row=>{
   if(tab.name==="DAILY REPORT")return row;
   const {_cells,...compactRow}=row||{};
   return compactRow;
  })}));
  if(!sheets.some(s=>s.found)){
   const failed=sheets[0]||{};
   return res.status(502).json({ok:false,error:failed.error||("Could not read worksheet "+tab.name+". Confirm it is published and the worksheet gid is correct."),worksheet:tab.name,sheets});
  }
  const payload={...data,ok:true,source:data.source||source,worksheet:tab.name,generatedAt:data.generatedAt||new Date().toISOString(),sheets};
  const serialized=JSON.stringify(payload);
  const bytes=Buffer.byteLength(serialized,"utf8");
  console.log("Google Sheets worksheet response",tab.name,"rows",sheets[0]?.rows?.length||0,"bytes",bytes);
  if(bytes>5_500_000){
   return res.status(413).json({ok:false,error:"Worksheet "+tab.name+" is still too large for a single response ("+bytes+" bytes). It needs row pagination or narrower columns.",worksheet:tab.name,bytes});
  }
  return res.status(200).json(payload);
 }catch(e){
  console.error("Google Sheets sync failed:",e);
  return res.status(502).json({ok:false,error:"Google Sheets sync failed.",details:e.message});
 }
};
