module.exports = async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store, max-age=0");
  if (req.method !== "GET") { res.setHeader("Allow", "GET"); return res.status(405).json({ok:false,error:"Method not allowed"}); }
  const endpoint = process.env.GOOGLE_APPS_SCRIPT_URL;
  const token = process.env.DASHBOARD_API_TOKEN;
  if (!endpoint || !token) return res.status(500).json({ok:false,error:"Missing GOOGLE_APPS_SCRIPT_URL or DASHBOARD_API_TOKEN environment variable."});
  try {
    const upstream = await fetch(endpoint, {method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({token}),cache:"no-store"});
    const data = await upstream.json();
    if (!upstream.ok || !data.ok) return res.status(502).json({ok:false,error:data.error || "Google Sheets API request failed."});
    return res.status(200).json(data);
  } catch (e) { return res.status(502).json({ok:false,error:"Could not reach Google Apps Script. Verify the deployment URL and permissions."}); }
};