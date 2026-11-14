// Horizon Nav shared reports: a small Cloudflare Worker with a D1 database.
// Drivers share police/accident/hazard/... reports; others confirm ("still there") or dismiss them.
// API (JSON): POST /v1/reports, GET /v1/reports?s=&w=&n=&e=, POST /v1/reports/<id>/vote, DELETE /v1/reports/<id> (own only), GET /v1/health.
// Speed limits reported by drivers: POST /v1/limits, GET /v1/limits?s=&w=&n=&e=.
// Shared trips: POST /v1/trips (new link), POST /v1/trips/<id> (update or end, with X-Trip-Key), GET /v1/trips/<id>,
// and the page for the link: GET /t/<id>.

const MIN = 60 * 1000;
const HOUR = 60 * MIN;

// lifetime of each kind (must match the app)
const LIFETIME = {
  police: 3 * HOUR,
  accident: 2 * HOUR,
  hazard: 1 * HOUR,
  breakdown: 2 * HOUR,
  mobilecam: 4 * HOUR,
  jam: 30 * MIN,
  roadworks: 12 * HOUR,
  closed: 3 * HOUR,
};

const MERGE_RADIUS_M = 300;
const MAX_BBOX_DEG = 6;
const MAX_RESULTS = 500;
const MAX_BODY_BYTES = 1024;

const LIMIT_REPORTS_PER_DEVICE = 10; // per hour
const LIMIT_REPORTS_PER_IP = 30; // per hour
const LIMIT_VOTES_PER_DEVICE = 60; // per hour

const YEAR = 365 * 24 * HOUR;
const LIMIT_SPEED_PER_DEVICE = 30; // per hour
const LIMIT_SPEED_PER_IP = 60; // per hour
const SPEED_REPLACE_M = 80; // a device's newer report this close (same direction) replaces its older one
const SPEED_MAX_BBOX_DEG = 0.2;
const SPEED_MAX_RESULTS = 2000;

const TRIP_STALE = 30 * MIN; // no update for this long: the trip shows as over
const LIMIT_TRIPS_PER_DEVICE = 20; // new links per hour
const LIMIT_TRIP_UPDATES_PER_DEVICE = 400; // updates per hour (one every 30 s is 120)
const TRIP_ID_RE = /^[A-Za-z0-9]{12,32}$/;

const DEVICE_RE = /^[A-Za-z0-9-]{8,64}$/;

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}

const round5 = (x) => Math.round(x * 1e5) / 1e5;

function haversineMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const rad = Math.PI / 180;
  const dLat = (lat2 - lat1) * rad;
  const dLon = (lon2 - lon1) * rad;
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
}

function deviceOf(request) {
  const d = request.headers.get("X-Device") || "";
  if (!DEVICE_RE.test(d)) throw new HttpError(400, "missing or invalid X-Device header");
  return d;
}

async function readJson(request) {
  const text = await request.text();
  if (text.length > MAX_BODY_BYTES) throw new HttpError(400, "body too large");
  try {
    const body = JSON.parse(text);
    if (body === null || typeof body !== "object" || Array.isArray(body)) throw new Error();
    return body;
  } catch {
    throw new HttpError(400, "invalid JSON body");
  }
}

// Counts one more request for `key` in the current hour; returns false when the limit is exceeded.
async function underLimit(env, key, limit, now) {
  const hour = Math.floor(now / HOUR);
  const row = await env.DB.prepare(
    "INSERT INTO rate (k, n, hour) VALUES (?1, 1, ?2) ON CONFLICT(k) DO UPDATE SET n = n + 1 RETURNING n"
  )
    .bind(`${key}:${hour}`, hour)
    .first();
  return !row || row.n <= limit;
}

// Applies a "still there" (up) or "gone" (down) vote to an active report row. Returns the new exp.
async function applyVote(env, report, still, now) {
  const life = LIFETIME[report.kind] || HOUR;
  let row;
  if (still) {
    row = await env.DB.prepare(
      "UPDATE reports SET up = up + 1, exp = MIN(MAX(exp, ?2), t + ?3) WHERE id = ?1 RETURNING exp"
    )
      .bind(report.id, now + Math.floor(life / 2), 3 * life)
      .first();
  } else {
    row = await env.DB.prepare(
      "UPDATE reports SET down = down + 1, " +
        "exp = CASE WHEN down + 1 >= 2 OR down + 1 > up THEN ?2 ELSE exp END WHERE id = ?1 RETURNING exp"
    )
      .bind(report.id, now)
      .first();
  }
  return row ? row.exp : now;
}

// Records the device's vote once; returns the new exp, or the unchanged exp when the device may not / already voted.
async function vote(env, report, device, still, now) {
  if (report.device === device) return report.exp; // the reporter can't vote on their own report
  const ins = await env.DB.prepare(
    "INSERT OR IGNORE INTO votes (report_id, device, still, t) VALUES (?1, ?2, ?3, ?4)"
  )
    .bind(report.id, device, still ? 1 : 0, now)
    .run();
  if (!ins.meta || !ins.meta.changes) return report.exp; // already voted
  return applyVote(env, report, still, now);
}

async function postReport(request, env) {
  const device = deviceOf(request);
  const body = await readJson(request);
  const { kind, lat, lon } = body;
  if (typeof kind !== "string" || !Object.prototype.hasOwnProperty.call(LIFETIME, kind))
    throw new HttpError(400, "unknown kind");
  if (typeof lat !== "number" || !Number.isFinite(lat) || lat < -90 || lat > 90)
    throw new HttpError(400, "invalid lat");
  if (typeof lon !== "number" || !Number.isFinite(lon) || lon < -180 || lon > 180)
    throw new HttpError(400, "invalid lon");
  const now = Date.now();

  if (!(await underLimit(env, `d:${device}`, LIMIT_REPORTS_PER_DEVICE, now)))
    throw new HttpError(429, "too many reports from this device");
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  if (!(await underLimit(env, `ip:${ip}`, LIMIT_REPORTS_PER_IP, now)))
    throw new HttpError(429, "too many reports from this address");

  const la = round5(lat);
  const lo = round5(lon);

  // duplicate merge: same kind within 300 m of an active report counts as a "still there" vote on it
  const dLat = MERGE_RADIUS_M / 111320;
  const dLon = dLat / Math.max(0.01, Math.cos((la * Math.PI) / 180));
  const { results } = await env.DB.prepare(
    "SELECT id, kind, lat, lon, t, exp, up, down, device FROM reports " +
      "WHERE kind = ?1 AND exp > ?2 AND lat BETWEEN ?3 AND ?4 AND lon BETWEEN ?5 AND ?6"
  )
    .bind(kind, now, la - dLat, la + dLat, lo - dLon, lo + dLon)
    .all();
  let best = null;
  let bestDist = Infinity;
  for (const r of results || []) {
    const d = haversineMeters(la, lo, r.lat, r.lon);
    if (d <= MERGE_RADIUS_M && d < bestDist) {
      best = r;
      bestDist = d;
    }
  }
  if (best) {
    await vote(env, best, device, true, now);
    return json(200, { id: best.id, merged: true });
  }

  const row = await env.DB.prepare(
    "INSERT INTO reports (kind, lat, lon, t, exp, up, down, device) VALUES (?1, ?2, ?3, ?4, ?5, 0, 0, ?6) RETURNING id"
  )
    .bind(kind, la, lo, now, now + LIFETIME[kind], device)
    .first();
  return json(200, { id: row.id, merged: false });
}

// the reporter takes back their own report (their device only); gone for everyone at once
async function deleteReport(request, env, id) {
  const device = deviceOf(request);
  const res = await env.DB.prepare("DELETE FROM reports WHERE id = ?1 AND device = ?2").bind(id, device).run();
  const changed = res?.meta?.changes ?? res?.changes ?? 0;
  if (!changed) throw new HttpError(404, "not found");
  return json(200, { ok: true });
}

async function getReports(request, env, url) {
  const device = deviceOf(request);
  const p = url.searchParams;
  const nums = ["s", "w", "n", "e"].map((k) => {
    const v = p.get(k);
    const x = v === null || v.trim() === "" ? NaN : Number(v);
    if (!Number.isFinite(x)) throw new HttpError(400, `invalid ${k}`);
    return x;
  });
  const [s, w, n, e] = nums;
  if (s < -90 || n > 90 || w < -180 || e > 180 || s > n || w > e) throw new HttpError(400, "invalid bbox");
  if (n - s > MAX_BBOX_DEG || e - w > MAX_BBOX_DEG) throw new HttpError(400, "bbox too large");
  const now = Date.now();
  const { results } = await env.DB.prepare(
    "SELECT id, kind, lat, lon, t, exp, up, down, device FROM reports " +
      "WHERE exp > ?1 AND lat BETWEEN ?2 AND ?3 AND lon BETWEEN ?4 AND ?5 ORDER BY t DESC LIMIT ?6"
  )
    .bind(now, s, n, w, e, MAX_RESULTS)
    .all();
  const reports = (results || []).map((r) => ({
    id: r.id,
    kind: r.kind,
    lat: r.lat,
    lon: r.lon,
    t: r.t,
    exp: r.exp,
    up: r.up,
    down: r.down,
    mine: r.device === device,
  }));
  return json(200, { now, reports });
}

async function postVote(request, env, id) {
  const device = deviceOf(request);
  const body = await readJson(request);
  if (typeof body.still !== "boolean") throw new HttpError(400, "still must be true or false");
  const now = Date.now();
  if (!(await underLimit(env, `v:${device}`, LIMIT_VOTES_PER_DEVICE, now)))
    throw new HttpError(429, "too many votes from this device");
  const report = await env.DB.prepare(
    "SELECT id, kind, t, exp, up, down, device FROM reports WHERE id = ?1 AND exp > ?2"
  )
    .bind(id, now)
    .first();
  if (!report) throw new HttpError(404, "unknown or expired report");
  const exp = await vote(env, report, device, body.still, now);
  return json(200, { ok: true, exp });
}

const angleDiff = (a, b) => Math.abs((((a - b) % 360) + 540) % 360 - 180);

function parseBbox(url, maxDeg) {
  const p = url.searchParams;
  const [s, w, n, e] = ["s", "w", "n", "e"].map((k) => {
    const v = p.get(k);
    const x = v === null || v.trim() === "" ? NaN : Number(v);
    if (!Number.isFinite(x)) throw new HttpError(400, `invalid ${k}`);
    return x;
  });
  if (s < -90 || n > 90 || w < -180 || e > 180 || s > n || w > e) throw new HttpError(400, "invalid bbox");
  if (n - s > maxDeg || e - w > maxDeg) throw new HttpError(400, "bbox too large");
  return [s, w, n, e];
}

async function postLimit(request, env) {
  const device = deviceOf(request);
  const body = await readJson(request);
  const { lat, lon, heading, kmh } = body;
  if (typeof lat !== "number" || !Number.isFinite(lat) || lat < -90 || lat > 90) throw new HttpError(400, "invalid lat");
  if (typeof lon !== "number" || !Number.isFinite(lon) || lon < -180 || lon > 180) throw new HttpError(400, "invalid lon");
  if (!Number.isInteger(kmh) || !(kmh === 0 || (kmh >= 5 && kmh <= 160))) throw new HttpError(400, "invalid kmh");
  const h = Number.isFinite(heading) && heading >= 0 ? Math.round(heading) % 360 : -1;
  const now = Date.now();
  if (!(await underLimit(env, `sd:${device}`, LIMIT_SPEED_PER_DEVICE, now)))
    throw new HttpError(429, "too many speed reports from this device");
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  if (!(await underLimit(env, `sip:${ip}`, LIMIT_SPEED_PER_IP, now)))
    throw new HttpError(429, "too many speed reports from this address");
  const la = round5(lat);
  const lo = round5(lon);
  // this device's own earlier reports at the same spot (same direction) are replaced, so one device always counts once
  const dLat = SPEED_REPLACE_M / 111320;
  const dLon = dLat / Math.max(0.01, Math.cos((la * Math.PI) / 180));
  const { results } = await env.DB.prepare(
    "SELECT id, lat, lon, h FROM speed_reports WHERE device = ?1 AND lat BETWEEN ?2 AND ?3 AND lon BETWEEN ?4 AND ?5"
  )
    .bind(device, la - dLat, la + dLat, lo - dLon, lo + dLon)
    .all();
  const old = (results || []).filter(
    (r) => haversineMeters(la, lo, r.lat, r.lon) <= SPEED_REPLACE_M && (r.h < 0 || h < 0 || angleDiff(r.h, h) < 60)
  );
  const stmts = old.map((r) => env.DB.prepare("DELETE FROM speed_reports WHERE id = ?1").bind(r.id));
  stmts.push(
    env.DB.prepare("INSERT INTO speed_reports (lat, lon, h, kmh, t, device) VALUES (?1, ?2, ?3, ?4, ?5, ?6)").bind(la, lo, h, kmh, now, device)
  );
  await env.DB.batch(stmts);
  return json(200, { ok: true, replaced: old.length });
}

async function getLimits(request, env, url) {
  const device = deviceOf(request);
  const [s, w, n, e] = parseBbox(url, SPEED_MAX_BBOX_DEG);
  const now = Date.now();
  const { results } = await env.DB.prepare(
    "SELECT lat, lon, h, kmh, t, device FROM speed_reports " +
      "WHERE t > ?1 AND lat BETWEEN ?2 AND ?3 AND lon BETWEEN ?4 AND ?5 ORDER BY t DESC LIMIT ?6"
  )
    .bind(now - YEAR, s, n, w, e, SPEED_MAX_RESULTS)
    .all();
  const limits = (results || []).map((r) => ({ lat: r.lat, lon: r.lon, h: r.h, kmh: r.kmh, t: r.t, mine: r.device === device }));
  return json(200, { now, limits });
}


// ---- shared trips ----

function randomId(n) {
  const abc = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  const b = new Uint8Array(n);
  crypto.getRandomValues(b);
  let out = "";
  for (const x of b) out += abc[x % abc.length];
  return out;
}

function tripFields(body) {
  const { lat, lon, heading, eta, rem } = body;
  if (typeof lat !== "number" || !Number.isFinite(lat) || lat < -90 || lat > 90) throw new HttpError(400, "invalid lat");
  if (typeof lon !== "number" || !Number.isFinite(lon) || lon < -180 || lon > 180) throw new HttpError(400, "invalid lon");
  if (!Number.isFinite(eta) || eta < 0) throw new HttpError(400, "invalid eta");
  if (!Number.isFinite(rem) || rem < 0 || rem > 5e7) throw new HttpError(400, "invalid rem");
  const h = Number.isFinite(heading) && heading >= 0 ? Math.round(heading) % 360 : -1;
  return { lat: round5(lat), lon: round5(lon), h, eta: Math.round(eta), rem: Math.round(rem) };
}

async function postTrip(request, env, url) {
  const device = deviceOf(request);
  const body = await readJson(request);
  const f = tripFields(body);
  const name = typeof body.name === "string" ? body.name.trim().slice(0, 40) : "";
  const now = Date.now();
  if (!(await underLimit(env, `td:${device}`, LIMIT_TRIPS_PER_DEVICE, now))) throw new HttpError(429, "too many shared trips from this device");
  const id = randomId(16);
  const key = randomId(32);
  await env.DB.prepare(
    "INSERT INTO trips (id, k, device, name, lat, lon, h, eta, rem, t, ended) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, 0)"
  )
    .bind(id, key, device, name, f.lat, f.lon, f.h, f.eta, f.rem, now)
    .run();
  return json(200, { id, key, url: `${url.origin}/t/${id}` });
}

async function updateTrip(request, env, id) {
  const device = deviceOf(request);
  const key = request.headers.get("X-Trip-Key") || "";
  const body = await readJson(request);
  const now = Date.now();
  if (!(await underLimit(env, `tu:${device}`, LIMIT_TRIP_UPDATES_PER_DEVICE, now))) throw new HttpError(429, "too many trip updates");
  const row = await env.DB.prepare("SELECT k, ended FROM trips WHERE id = ?1").bind(id).first();
  if (!row || row.k !== key) throw new HttpError(404, "no such trip");
  if (body.end === true) {
    await env.DB.prepare("UPDATE trips SET ended = 1, t = ?2 WHERE id = ?1").bind(id, now).run();
    return json(200, { ok: true, ended: true });
  }
  if (row.ended) return json(409, { error: "trip is over" });
  const f = tripFields(body);
  await env.DB.prepare("UPDATE trips SET lat = ?2, lon = ?3, h = ?4, eta = ?5, rem = ?6, t = ?7 WHERE id = ?1")
    .bind(id, f.lat, f.lon, f.h, f.eta, f.rem, now)
    .run();
  return json(200, { ok: true });
}

async function getTrip(env, id) {
  const row = await env.DB.prepare("SELECT name, lat, lon, h, eta, rem, t, ended FROM trips WHERE id = ?1").bind(id).first();
  if (!row) throw new HttpError(404, "no such trip");
  const now = Date.now();
  const state = row.ended ? "ended" : now - row.t > TRIP_STALE ? "stale" : "live";
  // once it's over, where the driver is stays private
  if (state !== "live") return json(200, { state, name: row.name, now, t: row.t });
  return json(200, { state, name: row.name, lat: row.lat, lon: row.lon, h: row.h, eta: row.eta, rem: row.rem, t: row.t, now });
}

function tripPage(id) {
  const html = `<!doctype html>
<html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex"><title>Unterwegs · Horizon Nav</title>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
<style>
html,body{margin:0;height:100%;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#0f1216;color:#fff}
#map{position:absolute;inset:0}
#card{position:absolute;left:12px;right:12px;bottom:16px;z-index:1000;max-width:480px;margin:0 auto;background:#1b2027;border-radius:20px;padding:16px 18px;box-shadow:0 8px 30px rgba(0,0,0,.45)}
#who{font-size:14px;color:#9aa4b2}#eta{font-size:30px;font-weight:700;margin:2px 0}#rest{font-size:16px;color:#d6dbe2}#age{font-size:12px;color:#7d8794;margin-top:6px}
.car{width:26px;height:26px;border-radius:50%;background:#2e6fd8;border:4px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.5)}
</style></head><body>
<div id="map"></div>
<div id="card"><div id="who">Unterwegs</div><div id="eta">…</div><div id="rest"></div><div id="age"></div></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
const ID=${JSON.stringify(id)};
const map=L.map('map',{zoomControl:false}).setView([51.2,10.4],6);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);
let car=null,first=true,last=null,timer=null;
const $=i=>document.getElementById(i);
function km(m){return m<1000?Math.round(m/10)*10+' m':(m/1000).toFixed(m<10000?1:0).replace('.',',')+' km'}
function hhmm(ms){return new Date(ms).toLocaleTimeString('de-DE',{hour:'2-digit',minute:'2-digit'})}
function age(){if(!last)return;const s=Math.max(0,Math.round((Date.now()-last)/1000));$('age').textContent='aktualisiert vor '+(s<60?s+' s':Math.round(s/60)+' Min.')}
async function load(){
 try{
  const r=await fetch('/v1/trips/'+ID,{cache:'no-store'});
  if(r.status===404){over('Dieser Link ist ungültig oder abgelaufen.');return}
  const d=await r.json();
  $('who').textContent=d.name?d.name+' ist unterwegs':'Unterwegs';
  if(d.state==='ended'){over('Angekommen oder Teilen beendet.');return}
  if(d.state==='stale'){over('Seit über 30 Minuten keine Aktualisierung – der Link ist abgelaufen.');return}
  const off=Date.now()-d.now; last=d.t+off;
  $('eta').textContent='Ankunft '+hhmm(d.eta+off);
  const min=Math.max(0,Math.round((d.eta-d.now)/60000));
  $('rest').textContent='noch '+km(d.rem)+' · etwa '+(min<60?min+' Min.':Math.floor(min/60)+' Std. '+(min%60)+' Min.');
  age();
  const p=[d.lat,d.lon];
  if(!car)car=L.marker(p,{icon:L.divIcon({className:'',html:'<div class="car"></div>',iconSize:[26,26],iconAnchor:[13,13]})}).addTo(map);
  else car.setLatLng(p);
  if(first){map.setView(p,14);first=false}else if(!map.getBounds().pad(-0.15).contains(p))map.panTo(p);
 }catch(e){$('age').textContent='Keine Verbindung – neuer Versuch …'}
}
function over(t){$('eta').textContent='Fahrt beendet';$('rest').textContent=t;$('age').textContent='';if(car){map.removeLayer(car);car=null}clearInterval(timer);timer=null}
load();timer=setInterval(load,15000);setInterval(age,1000);
</script></body></html>`;
  return new Response(html, {
    status: 200,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
      "referrer-policy": "no-referrer",
      "x-robots-tag": "noindex",
    },
  });
}

async function handle(request, env) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  const method = request.method;

  if (path === "/v1/health") {
    if (method !== "GET" && method !== "HEAD") throw new HttpError(405, "method not allowed");
    return json(200, { ok: true });
  }
  if (path === "/v1/reports") {
    if (method === "POST") return postReport(request, env);
    if (method === "GET") return getReports(request, env, url);
    throw new HttpError(405, "method not allowed");
  }
  if (path === "/v1/limits") {
    if (method === "POST") return postLimit(request, env);
    if (method === "GET") return getLimits(request, env, url);
    throw new HttpError(405, "method not allowed");
  }
  if (path === "/v1/trips") {
    if (method !== "POST") throw new HttpError(405, "method not allowed");
    return postTrip(request, env, url);
  }
  const tm = path.match(/^\/v1\/trips\/([A-Za-z0-9]+)$/);
  if (tm) {
    if (!TRIP_ID_RE.test(tm[1])) throw new HttpError(404, "no such trip");
    if (method === "POST") return updateTrip(request, env, tm[1]);
    if (method === "GET") return getTrip(env, tm[1]);
    throw new HttpError(405, "method not allowed");
  }
  const pm = path.match(/^\/t\/([A-Za-z0-9]+)$/);
  if (pm) {
    if (method !== "GET" && method !== "HEAD") throw new HttpError(405, "method not allowed");
    if (!TRIP_ID_RE.test(pm[1])) throw new HttpError(404, "not found");
    return tripPage(pm[1]);
  }
  const del = path.match(/^\/v1\/reports\/(\d{1,15})$/);
  if (del) {
    if (method !== "DELETE") throw new HttpError(405, "method not allowed");
    return deleteReport(request, env, Number(del[1]));
  }
  const m = path.match(/^\/v1\/reports\/(\d{1,15})\/vote$/);
  if (m) {
    if (method !== "POST") throw new HttpError(405, "method not allowed");
    return postVote(request, env, Number(m[1]));
  }
  throw new HttpError(404, "not found");
}

// hourly clean-up: reports expired more than an hour ago, their votes, and old rate counters
async function cleanup(env, now) {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM reports WHERE exp < ?1").bind(now - HOUR),
    env.DB.prepare("DELETE FROM votes WHERE report_id NOT IN (SELECT id FROM reports)"),
    env.DB.prepare("DELETE FROM rate WHERE hour < ?1").bind(Math.floor(now / HOUR) - 1),
    env.DB.prepare("DELETE FROM speed_reports WHERE t < ?1").bind(now - YEAR),
    env.DB.prepare("DELETE FROM trips WHERE t < ?1").bind(now - 24 * HOUR),
  ]);
}

export default {
  async fetch(request, env) {
    try {
      return await handle(request, env);
    } catch (err) {
      if (err instanceof HttpError) return json(err.status, { error: err.message });
      console.error(err && err.stack ? err.stack : String(err));
      return json(500, { error: "internal error" });
    }
  },

  async scheduled(event, env, ctx) {
    ctx.waitUntil(cleanup(env, Date.now()));
  },
};
