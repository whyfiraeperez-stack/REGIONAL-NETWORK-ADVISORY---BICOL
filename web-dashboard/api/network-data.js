const ID="1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const TABS=["DATABASE","DAILY REPORT","BACKBONE LIST","OUTAGE ADVISORY"];

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
  const record={_rowNumber:i+2,_cells:cells.slice()};
  headers.forEach((h,j)=>record[h]=cells[j]??"");
  return record;
 }).filter((r,i)=>matrix[i+1].some(v=>String(v??"").trim()));
 return {name,found:true,headers,rows};
}
async function readTabCsv(name){
 const url="https://docs.google.com/spreadsheets/d/"+ID+"/gviz/tq?tqx=out:csv&sheet="+encodeURIComponent(name)+"&tq="+encodeURIComponent("select *")+"&_="+Date.now();
 const response=await fetch(url,{cache:"no-store"});
 const body=await response.text();
 if(!response.ok||/<html|accounts\.google\.com|sign in|google\.visualization\.Query\.setResponse/i.test(body.slice(0,500)))return {name,found:false,headers:[],rows:[],error:"Tab unavailable or not publicly readable"};
 return rowsToSheet(name,parseCsv(body));
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
module.exports=async function(req,res){
 res.setHeader("Cache-Control","no-store, max-age=0, must-revalidate");
 res.setHeader("X-Content-Type-Options","nosniff");
 if(req.method!=="GET"){res.setHeader("Allow","GET");return res.status(405).json({ok:false,error:"Method not allowed"});}
 try{
  let data,source="Google Sheets CSV export";
  try{data=await readFromAppsScript();if(data)source="Authenticated Google Apps Script";}catch(error){
   console.error("Apps Script read failed:",error.message);
   return res.status(502).json({ok:false,error:"The authenticated Google Sheets read failed. Check GOOGLE_APPS_SCRIPT_URL, DASHBOARD_API_TOKEN, and the Apps Script deployment.",details:error.message});
  }
  if(!data){
   const sheets=await Promise.all(TABS.map(async name=>{try{return await readTabCsv(name)}catch(e){return {name,found:false,headers:[],rows:[],error:"Read failed: "+e.message}}}));
   data={ok:true,spreadsheetId:ID,generatedAt:new Date().toISOString(),sheets};
  }
  data.sheets=(data.sheets||[]).map(s=>({...s,headers:Array.isArray(s.headers)?s.headers:[],rows:Array.isArray(s.rows)?s.rows:[]}));
  if(!data.sheets.some(s=>s.found))return res.status(502).json({ok:false,error:"Could not read the expected worksheet tabs. Verify sheet access and exact tab names.",sheets:data.sheets});
  return res.status(200).json({...data,ok:true,source:data.source||source,generatedAt:data.generatedAt||new Date().toISOString()});
 }catch(e){
  console.error("Google Sheets sync failed:",e);
  return res.status(502).json({ok:false,error:"Google Sheets sync failed.",details:e.message});
 }
};
