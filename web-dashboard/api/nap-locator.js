const ID = "1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg";
const PUBLISHED_ID = "2PACX-1vTojZur9YkReg6paEkkqvsNscFzIW1dH7WwYfG7NqJSw-0xAEUwRJse_ILc1qLXidGK8Sno5ADAbhTo";
const NAP_GID = "1924056980";
function parseCsv(s) {
  const rows=[]; let row=[], cell="", quoted=false;
  for(let i=0;i<s.length;i++){
    const c=s[i];
    if(c=='"'){ if(quoted && s[i+1]=='"'){cell+='"';i++;} else quoted=!quoted; }
    else if(c===',' && !quoted){row.push(cell);cell="";}
    else if((c==='\\n'||c==='\\r')&&!quoted){if(c==='\\r'&&s[i+1]==='\\n')i++;row.push(cell);if(row.some(v=>v.trim()))rows.push(row);row=[];cell="";}
    else cell+=c;
  }
  if(cell.length||row.length){row.push(cell);if(row.some(v=>v.trim()))rows.push(row);}
  return rows;
}
const norm = v => String(v??"").toLowerCase().replace(/[^a-z0-9]/g,"");
function pick(row, headers, aliases) {
  for(const a of aliases){const i=headers.findIndex(h=>norm(h)===norm(a));if(i>=0&&String(row[i]??"").trim()!=="")return row[i];}
  for(const a of aliases){const i=headers.findIndex(h=>norm(h).includes(norm(a)));if(i>=0&&String(row[i]??"").trim()!=="")return row[i];}
  return "";
}
module.exports = async function(req,res) {
  res.setHeader("Cache-Control","no-store, max-age=0, must-revalidate");
  res.setHeader("X-Content-Type-Options","nosniff");
  if(req.method!=="GET"){res.setHeader("Allow","GET");return res.status(405).json({ok:false,error:"Method not allowed"});}
  try {
    const url="https://docs.google.com/spreadsheets/d/e/"+PUBLISHED_ID+"/pub?gid="+NAP_GID+"&single=true&output=csv&_="+Date.now();
    const response=await fetch(url,{cache:"no-store",redirect:"follow"});
    const body=await response.text();
    if(!response.ok||/<html|accounts\\.google\\.com|sign in|google\\.visualization\\.Query\\.setResponse/i.test(body.slice(0,500)))
      return res.status(502).json({ok:false,error:"Published NAP LOCATOR CSV is unavailable. Confirm the spreadsheet is published to the web."});
    const matrix=parseCsv(body);
    if(!matrix.length)return res.status(502).json({ok:false,error:"NAP LOCATOR worksheet is empty or unavailable."});
    const headers=matrix[0].map((v,i)=>String(v||"COLUMN_"+(i+1)).trim());
    const records=matrix.slice(1).map(row=>({
      napCode:String(pick(row,headers,["NAP CODE","NAP NAME","NAP","NODE CODE","NAP ID","DESCRIPTION","NAME"])).trim(),
      latitude:String(pick(row,headers,["LATITUDE","LAT","GPS LATITUDE","Y COORDINATE"])).trim(),
      longitude:String(pick(row,headers,["LONGITUDE","LON","LONG","GPS LONGITUDE","X COORDINATE"])).trim(),
      noOfPorts:String(pick(row,headers,["NO OF PORTS","NUMBER OF PORTS","PORTS","PORT COUNT","CAPACITY","NAP PORTS"])).trim(),
      area:String(pick(row,headers,["AREA","BARANGAY","LOCATION","ADDRESS","SITE"])).trim(),
      lcpCode:String(pick(row,headers,["LCP CODE","LCP","LCP NAME","LCP ID"])).trim()
    })).filter(r=>r.napCode&&r.latitude!==""&&r.longitude!==""&&Number.isFinite(Number(r.latitude))&&Number.isFinite(Number(r.longitude))&&Number(r.latitude)>=-90&&Number(r.latitude)<=90&&Number(r.longitude)>=-180&&Number(r.longitude)<=180);
    const unique=new Map();records.forEach(r=>{r.latitude=Number(r.latitude);r.longitude=Number(r.longitude);r.noOfPorts=r.noOfPorts||"—";r.area=r.area||"Unknown area";r.lcpCode=r.lcpCode||((r.napCode.match(/\\bLP\\s*\\d+\\b/i)||[])[0]||"—").replace(/\\s+/g," ").toUpperCase();unique.set(r.napCode,r);});
    const finalRecords=[...unique.values()];
    if(!finalRecords.length)return res.status(502).json({ok:false,error:"NAP LOCATOR was read but no valid rows with NAP code and coordinates were found.",headers});
    return res.status(200).json({ok:true,source:"Published Google Sheets CSV · NAP LOCATOR",spreadsheetId:ID,worksheet:"NAP LOCATOR",generatedAt:new Date().toISOString(),count:finalRecords.length,headers,records:finalRecords});
  } catch(e) {
    console.error("NAP LOCATOR sync failed:",e);
    return res.status(502).json({ok:false,error:"NAP LOCATOR spreadsheet sync failed.",details:e.message});
  }
};