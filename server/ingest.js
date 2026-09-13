// Zero-dependency ingestion server for open-analytics-android.
// Extends the open-sdk-analytics server (server/ingest.js by Sohan Ananthula)
// with a dedicated /api/v1/crash-report route for user-submitted crash reports.
//
//   node server/ingest.js
//
// Routes:
//   POST /api/v1/track          -> appends analytics events   to events.jsonl
//   POST /api/v1/crash-report   -> appends user crash reports  to crash-reports.jsonl
//   GET  /health                -> { status: "ok" }

import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = process.env.PORT || 4000;
const EVENTS_FILE = path.join(__dirname, 'events.jsonl');
const CRASH_FILE = path.join(__dirname, 'crash-reports.jsonl');

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', (c) => { body += c; });
    req.on('end', () => resolve(body));
    req.on('error', reject);
  });
}

function appendJsonl(file, records, req) {
  const lines = records.map((r) => JSON.stringify({
    ...r,
    _server_received_at: new Date().toISOString(),
    _client_ip: req.headers['x-forwarded-for'] || req.socket.remoteAddress
  })).join('\n') + '\n';
  return new Promise((resolve, reject) => {
    fs.appendFile(file, lines, (err) => (err ? reject(err) : resolve()));
  });
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Idempotency-Key');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

  const url = new URL(req.url, `http://${req.headers.host}`);

  // ---- Crash reports -------------------------------------------------
  if (req.method === 'POST' && url.pathname.endsWith('/crash-report')) {
    try {
      const payload = JSON.parse(await readBody(req));
      const reports = Array.isArray(payload.reports) ? payload.reports : [payload];
      await appendJsonl(CRASH_FILE, reports, req);
      console.log(`[Crash] Stored ${reports.length} report(s).`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ accepted: reports.length, ids: reports.map((r) => r.crash_id) }));
    } catch (err) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid JSON payload' }));
    }
    return;
  }

  // ---- Analytics events (same contract as open-sdk-analytics) --------
  if (req.method === 'POST' && (url.pathname.endsWith('/track') || url.pathname === '/track')) {
    try {
      const payload = JSON.parse(await readBody(req));
      const events = Array.isArray(payload.events) ? payload.events : [payload];
      await appendJsonl(EVENTS_FILE, events, req);
      console.log(`[Ingest] Stored ${events.length} event(s).`);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ accepted: events.length, accepted_ids: events.map((e) => e.event_id) }));
    } catch (err) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid JSON payload' }));
    }
    return;
  }

  if (req.method === 'GET' && url.pathname === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok', timestamp: new Date().toISOString() }));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
});

server.listen(PORT, () => {
  console.log(`Open Analytics (Android) ingestion server on http://localhost:${PORT}`);
  console.log(`  events:        POST http://localhost:${PORT}/api/v1/track`);
  console.log(`  crash reports: POST http://localhost:${PORT}/api/v1/crash-report`);
});
