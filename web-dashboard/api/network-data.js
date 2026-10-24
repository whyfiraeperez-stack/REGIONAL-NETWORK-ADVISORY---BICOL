const ID="1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const TABS=["DATABASE","DAILY REPORT","BACKBONE LIST","OUTAGE ADVISORY"];
function parseCsv(s){const rows=[];let row=[],cell="",q=false;for(let i=0;i<s.length;i++){const c=s[i];if(c==='"'){if(q&&s[i+1]==='"'){cell+='"';i++;}else q=!q;}else if(c===","&&!q){row.push(cell);cell="";}else if((c==="\n"||c==="\r")&&!q){if(c==="\r"&&s[i+1]==="\n")i++;row.push(cell);if(row.some(x=>x.trim()))rows.push(row);row=[];cell="";}else cell+=c;}if(cell.length||row.length){row.push(cell);if(row.some(x=>x.trim()))rows.push(row);}return rows;}
async function readTab(name){
 const url="https://docs.google.com/spreadsheets/d/"+ID+"/gviz/tq?tqx=out:csv&sheet="+encodeURIComponent(name)+"&tq="+encodeURIComponent("select *")+"&_="+Date.now();
 try{
  const r=await fetch(url,{cache:"no-store"});
  const text=await r.text();
  if(!r.ok||/<html|accounts\.google\.com|sign in|google\.visualization\.Query\.setResponse/i.test(text.slice(0,500)))return {name,found:false,headers:[],rows:[],error:"Tab unavailable"};
  const matrix=parseCsv(text);if(!matrix.length)return {name,found:false,headers:[],rows:[]};
  const rawHeaders=matrix[0].map((h,i)=>String(h||"COLUMN_"+(i+1)).trim());
  const namedByColumn={4:"BB UP",5:"BB DOWN",6:"BB TOTAL",7:"BB % Restored",10:"OLT UP",11:"OLT DOWN",12:"OLT TOTAL",13:"OLT % Restored",16:"LCP UP",17:"LCP Impact Type",18:"LCP DOWN",19:"LCP TOTAL",20:"LCP % Down",23:"NAP UP",24:"NAP DOWN",25:"NAP TOTAL",26:"NAP % Down"};
  const headers=rawHeaders.map((h,i)=>namedByColumn[i]||h);
  const rows=matrix.slice(1).map((cells,i)=>{const o={_rowNumber:i+2};headers.forEach((h,j)=>o[h]=cells[j]||"");return o;}).filter(o=>headers.some(h=>String(o[h]||"").trim()));
  return {name,found:true,headers,rows};
 }catch(e){return {name,found:false,headers:[],rows:[],error:"Read failed"};}
}
module.exports=async function(req,res){
 res.setHeader("Cache-Control","no-store, max-age=0");
 if(req.method!=="GET"){res.setHeader("Allow","GET");return res.status(405).json({ok:false,error:"Method not allowed"});}
 try{
  const sheets=await Promise.all(TABS.map(readTab));
  if(!sheets.some(s=>s.found))return res.status(502).json({ok:false,error:"Could not read the named tabs. Verify sheet sharing and exact tab names.",sheets});
  return res.status(200).json({ok:true,spreadsheetId:ID,generatedAt:new Date().toISOString(),sheets});
 }catch(e){return res.status(502).json({ok:false,error:"Google Sheets read failed."});}
};