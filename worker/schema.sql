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

-- speed limit reports from drivers: one row per device and spot (a device's newer report nearby replaces its older one).
-- kmh 0 = no general limit; h = heading in degrees (-1 unknown); t = epoch milliseconds. Kept for a year.
CREATE TABLE IF NOT EXISTS speed_reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  h INTEGER NOT NULL,
  kmh INTEGER NOT NULL,
  t INTEGER NOT NULL,
  device TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS speed_reports_lat_lon ON speed_reports (lat, lon);
CREATE INDEX IF NOT EXISTS speed_reports_t ON speed_reports (t);

-- shared trips ("Ankunftszeit teilen"): the driver's position, arrival time and distance left, seen by whoever has the link.
-- id = the random link id; k = the write key only the app knows; t = last update (epoch ms). A trip shows as over when ended = 1
-- or after 30 minutes without an update; rows go a day later.
CREATE TABLE IF NOT EXISTS trips (
  id TEXT PRIMARY KEY,
  k TEXT NOT NULL,
  device TEXT NOT NULL,
  name TEXT NOT NULL,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  h INTEGER NOT NULL,
  eta INTEGER NOT NULL,
  rem INTEGER NOT NULL,
  t INTEGER NOT NULL,
  ended INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS trips_t ON trips (t);
