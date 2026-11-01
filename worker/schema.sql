-- D1 schema for the Horizon Nav shared reports (safe to run again on every deploy).

-- one row per shared report; t and exp are epoch milliseconds
CREATE TABLE IF NOT EXISTS reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  kind TEXT NOT NULL,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  t INTEGER NOT NULL,
  exp INTEGER NOT NULL,
  up INTEGER NOT NULL DEFAULT 0,
  down INTEGER NOT NULL DEFAULT 0,
  device TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS reports_lat_lon ON reports (lat, lon);
CREATE INDEX IF NOT EXISTS reports_exp ON reports (exp);

-- one vote per device per report
CREATE TABLE IF NOT EXISTS votes (
  report_id INTEGER NOT NULL,
  device TEXT NOT NULL,
  still INTEGER NOT NULL,
  t INTEGER NOT NULL,
  PRIMARY KEY (report_id, device)
);

-- rate-limit counters, key "<key>:<hour>" (hour = epoch ms / 3600000)
CREATE TABLE IF NOT EXISTS rate (
  k TEXT PRIMARY KEY,
  n INTEGER NOT NULL,
  hour INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS rate_hour ON rate (hour);
