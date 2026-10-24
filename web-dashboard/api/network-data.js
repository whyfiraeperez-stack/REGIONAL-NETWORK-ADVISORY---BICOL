const ID = "1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const GID = "916324578";
function csv(s) {
  const rows=[]; let row=[], cell="", q=false;
  for(let i=0;i<s.length;i++){const c=s[i];if(c==='"'){if(q&&s[i+1]==='"'){cell+='"';i++;}else q=!q;}else if(c===","&&!q){row.push(cell);cell="";}else if((c==="\n"||c==="\r")&&!q){if(c==="\r"&&s[i+1]==="\n")i++;row.push(cell);if(row.some(x=>x.trim()))rows.push(row);row=[];cell="";}else cell+=c;}
  if(cell.length||row.length){row.push(cell);if(row.some(x=>x.trim()))rows.push(row);} return rows;
}
module.exports=async function(req,res){
 res.setHeader("Cache-Control","no-store, max-age=0");
 if(req.method!=="GET"){res.setHeader("Allow","GET");return res.status(405).json({ok:false,error:"Method not allowed"});}
 try{
  const r=await fetch("https://docs.google.com/spreadsheets/d/"+ID+"/export?format=csv&gid="+GID+"&_="+Date.now(),{cache:"no-store"});
  const text=await r.text();
  if(!r.ok||/<html|accounts\.google\.com|sign in/i.test(text.slice(0,1000)))return res.status(502).json({ok:false,error:"Google Sheets CSV is not accessible. Confirm link sharing is set to Viewer."});
  const matrix=csv(text);if(!matrix.length)return res.status(502).json({ok:false,error:"No rows returned from the selected sheet."});
  const headers=matrix[0].map((h,i)=>String(h||"COLUMN_"+(i+1)).trim());
  const rows=matrix.slice(1).map((cells,i)=>{const o={_rowNumber:i+2};headers.forEach((h,j)=>o[h]=cells[j]||"");return o;}).filter(o=>headers.some(h=>String(o[h]||"").trim()));
  return res.status(200).json({ok:true,spreadsheetId:ID,generatedAt:new Date().toISOString(),sheets:[{name:"DATABASE",found:true,headers,rows},{name:"DAILY REPORT",found:false,headers:[],rows:[]},{name:"BACKBONE LIST",found:false,headers:[],rows:[]},{name:"OUTAGE ADVISORY",found:false,headers:[],rows:[]}]});
 }catch(e){return res.status(502).json({ok:false,error:"Google Sheets connection failed."});}
};