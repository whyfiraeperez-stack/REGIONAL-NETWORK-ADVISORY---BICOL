# Cloudflare Pages deployment

This is a static dashboard with Cloudflare Pages Functions for the Google Sheets API.

## Pages project settings

- Connect GitHub repository: `whyfiraeperez-stack/REGIONAL-NETWORK-ADVISORY---BICOL`
- Production branch: `main`
- Root directory: `web-dashboard`
- Build command: leave blank (no build step is required)
- Build output directory: `.`

The `wrangler.toml` file in this directory records the Pages project name, output directory, and compatibility date. Pages Functions are in `functions/api/` and provide:

- `GET /api/network-data?sheet=DATABASE&page=1&limit=500`
- `GET /api/nap-locator`

## Google Sheets connection

The API can read from the spreadsheet's published CSV endpoint when no secrets are configured. This fallback is read-only and requires the relevant worksheets to be published to the web.

For the authenticated Apps Script bridge, add these under Pages project **Settings → Variables and Secrets → Production**:

- `GOOGLE_APPS_SCRIPT_URL`: the deployed Google Apps Script Web App URL (the URL ending in `/exec`)
- `DASHBOARD_API_TOKEN`: the same secret stored in Apps Script Script Properties under `DASHBOARD_API_TOKEN`

Set the token as a secret, not a plain-text variable. Do not commit it to GitHub or put it in frontend JavaScript. The Pages Function uses these values server-side and falls back to the published CSV if the authenticated read fails.

The NAP Locator endpoint reads the published NAP LOCATOR worksheet CSV, matching the previous Netlify implementation.

## Deployment and verification

A Git-connected Pages project deploys from `main` after each push. After the first deployment, verify:

1. The Pages deployment reports **Success**.
2. Open the site's `/api/network-data?sheet=DATABASE&page=1&limit=2`; it should return JSON with `ok: true`, a found DATABASE worksheet, headers, rows, and pagination.
3. Open `/api/network-data?sheet=OUTAGE%20ADVISORY&page=1&limit=2` and `/api/network-data?sheet=GNOC&page=1&limit=2`.
4. Open `/api/nap-locator`; it should return `ok: true` and a records array.
5. Open the dashboard and verify the KPI cards, province breakdown, database, outage advisory, GNOC, report, and NAP Locator.

The dashboard polls the three main worksheets every 30 seconds. Cloudflare Pages Functions execute under the Workers runtime and are subject to the account's Workers plan limits; this is not an unlimited-request service.
