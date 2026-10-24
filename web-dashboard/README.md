# Bicol Network Monitoring web dashboard

Desktop-first web application based on the supplied design references. It uses an off-white / dirty-white palette and reads live rows from the provided Google Sheet through a Google Apps Script backend and a Vercel serverless proxy.

## Included
- Dashboard sections corresponding to requirements 1–8: Converge ICT branding, Bicol title/date filters, network fault summary, BB/OLT/LCP/NAP counters, pending/completed outage advisories, database table, GNOC update navigation placeholder, report/CSV export.
- Spreadsheet headers and rows are discovered at runtime rather than hard-coded.
- 30-second polling, manual refresh, hidden-tab pause, search/status/date filters, CSV export.
- Apps Script read-only endpoint and Vercel serverless proxy. The shared token remains on the server; do not add it to frontend JavaScript.
- Google AI Studio prompt in `GOOGLE_AI_STUDIO_PROMPT.md`.

## Google Sheets / Apps Script setup
1. Open the linked spreadsheet, choose **Extensions → Apps Script**.
2. Paste `apps-script/Code.gs` from this folder into the script editor.
3. In **Project Settings → Script properties**, add `DASHBOARD_API_TOKEN` with a long random secret.
4. In Apps Script, choose **Deploy → New deployment → Web app**. Set execution as your account and access according to your organization's security policy. The Vercel server function must be allowed to call the deployment. Copy the deployed `/exec` URL.
5. Verify the script owner has access to the spreadsheet. The expected worksheet tabs are `DATABASE`, `DAILY REPORT`, `BACKBONE LIST`, and `OUTAGE ADVISORY`; absent tabs return an empty result.

## Vercel deployment
1. In Vercel, import the GitHub repository `whyfiraeperez-stack/REGIONAL-NETWORK-ADVISORY---BICOL`.
2. Set **Root Directory** to `web-dashboard`.
3. Add server-side environment variables:
   - `GOOGLE_APPS_SCRIPT_URL` = the Apps Script deployment URL
   - `DASHBOARD_API_TOKEN` = the same secret as the Apps Script script property
4. Deploy. The static page calls `/api/network-data`, which calls Apps Script; no credentials are placed in browser code.

## GitHub
This folder is designed to be reviewed in a feature branch before merging. GitHub integration does not automatically deploy until a Vercel project is imported and linked to this repository/branch.

## Important limitations
- This initial integration is **read-only**. It does not edit outage advisories or write GNOC updates. Those require verified row keys, editable-field permissions, input validation, concurrency protection and audit logging.
- KPI classification is a best-effort text match against the database's type/subject/affected-node fields. Confirm the exact BB/OLT/LCP/NAP business definitions and adjust them before operational use.
- Header row is configured as row 1; change `HEADER_ROW` if your workbook has title rows above the column headings.
- Data is polled every 30 seconds, not pushed instantaneously.
