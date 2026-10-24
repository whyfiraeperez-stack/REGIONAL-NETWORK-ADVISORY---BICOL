# Google AI Studio Build prompt — Bicol Network Monitoring

Build a production-minded, desktop-first **web application** called Bicol Network Monitoring. Do not build a mobile-only app. Use a sleek dirty-white/off-white theme (#F3F2ED), white cards, charcoal typography, subtle grey borders, and restrained blue accents. Make it responsive for smaller screens without compromising the desktop NOC layout.

## Source of truth
Google Sheets: https://docs.google.com/spreadsheets/d/1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg/edit?gid=916324578
Spreadsheet ID: `1DgYA7-nM4DoVg70YOEGlu4Au7nb7xNpdSzyW8BZFsOg`.
Expected tabs: `DATABASE`, `DAILY REPORT`, `BACKBONE LIST`, `OUTAGE ADVISORY`. Discover and use the actual headers; do not fabricate sample rows or claim the spreadsheet has been read if the API is not configured.

## Match reference numbering
1. Converge ICT branding at upper left. Use approved logo asset if available; otherwise use a restrained text mark and do not invent an official logo.
2. BICOL NETWORK MONITORING title, Asia/Manila date/time, year/month/date filters, automatic refresh toggle, manual refresh and last-sync indicator.
3. Network Fault Summary Report. Group by BB, OLT and LCP; display UP, DOWN, TOTAL and percentages only after the exact definitions are verified.
4. KPI cards for BB DOWN, OLT DOWN, LCP DOWN, NAP DOWN, calculated from the actual database rows using confirmed business rules.
5. Outage Advisory split into Pending and Completed. Selecting a ticket should populate Ticket, Status, Subject, Province, Municipality, Barangay, Type, Affected Nodes/Links, RFO, Action Taken, Downtime, Uptime and ETR. Do not allow writes until authorized editable columns and row identity are confirmed.
6. Database view showing actual spreadsheet columns and rows, with search, status/date filters, pagination and visible sync time.
7. GNOC Update: show running status for the requested columns A:AD only after validating the sheet and column mapping. Provide an update workflow only after the user confirms which fields can be changed.
8. Generate Report with date/month/year filters, print layout, CSV export, summary and generation timestamp.

## Architecture
Use the included Vercel serverless endpoint `/api/network-data` as a server-side proxy to Google Apps Script. The frontend must never contain the shared token. Environment variables: `GOOGLE_APPS_SCRIPT_URL` and `DASHBOARD_API_TOKEN`. Poll every 30 seconds and pause polling while the browser tab is hidden. Show loading, empty, error and stale-data states. Use Asia/Manila for display. Keep date strings as provided when ambiguous.

## Safety and delivery
The initial integration is read-only. Before adding write support, verify exact headers, row key, editable fields, allowed status transitions, validation, audit logging and conflict handling. Deliver a complete runnable project and document deployment steps for GitHub and Vercel. Never claim accounts are connected or a deployment succeeded unless actually tested.