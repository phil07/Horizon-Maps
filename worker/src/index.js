// Horizon Nav shared reports: a small Cloudflare Worker with a D1 database.
// Drivers share police/accident/hazard/... reports; others confirm ("still there") or dismiss them.
// API (JSON): POST /v1/reports, GET /v1/reports?s=&w=&n=&e=, POST /v1/reports/<id>/vote, GET /v1/health.
// Speed limits reported by drivers: POST /v1/limits, GET /v1/limits?s=&w=&n=&e=.

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
