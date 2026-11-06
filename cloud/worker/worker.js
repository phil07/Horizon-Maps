export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    if (url.pathname === '/report' && request.method === 'POST') {
      const data = await request.json();
      await handleReport(env, data);
      return new Response(JSON.stringify({status: 'ok'}), {status: 200});
    }

    if (url.pathname === '/reports' && request.method === 'GET') {
      const reports = await handleGetReports(env);
      return new Response(JSON.stringify(reports), {status: 200});
    }

    return new Response('Not Found', {status: 404});
  }
}

async function handleReport(env, data) {
  const { kind, lat, lon, ts } = data;
  const now = Date.now();
  const ttl = 30 * 60 * 1000;

  // Cleanup old reports
  await env.DB.prepare("DELETE FROM reports WHERE ts < ?").bind(now - ttl).run();

  // Find nearby reports (approx 300m radius)
  const nearby = await env.DB.prepare(
    "SELECT id, lat, lon, count FROM reports WHERE kind = ? AND ABS(lat - ?) < 0.003 AND ABS(lon - ?) < 0.003"
  ).bind(kind, lat, lon).all();

  if (nearby.results.length > 0) {
    const existing = nearby.results[0];
    await env.DB.prepare("UPDATE reports SET ts = ?, count = count + 1 WHERE id = ?")
      .bind(ts, existing.id).run();
  } else {
    const id = crypto.randomUUID();
    await env.DB.prepare("INSERT INTO reports (id, kind, lat, lon, ts, count) VALUES (?, ?, ?, ?, ?, 1)")
      .bind(id, kind, lat, lon, ts).run();
  }
}

async function handleGetReports(env) {
  const now = Date.now();
  const ttl = 30 * 60 * 1000;
  const result = await env.DB.prepare("SELECT * FROM reports WHERE ts > ?").bind(now - ttl).all();
  return result.results;
}