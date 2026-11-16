## Unreleased

### Neu
- Abschnittskontrollen auch ohne Ziel: Fährst du ohne Route durch eine Abschnittskontrolle, sagt die App sie an und zeigt deine Durchschnittsgeschwindigkeit, gemessen an der gefahrenen Strecke ab dem Anfang.
  EN: Average speed checks without a destination: driving through one without a route, the app announces it and shows your average speed, measured by the distance driven from its start.

### Verbessert
- Abschnittskontrollen werden gemerkt: Gefundene Abschnittskontrollen bleiben auf dem Gerät gespeichert. Auf einer Strecke, die du schon gefahren bist, ist die Kontrolle sofort da, auch ohne Netz.
  EN: Average speed checks are remembered: checks found are kept on the device. On a road driven before, the check is there straight away, also without signal.

### Behoben
- Abschnittskontrolle startete nicht: Die Kameras am Anfang und Ende wurden als Blitzer angezeigt, die Kontrolle selbst aber nie. Die Kontrollen werden jetzt in einer eigenen, kleinen Abfrage gesucht, mit mehr Geduld für langsame Server und einem neuen Versuch nach 20 statt 60 Sekunden.
  EN: Average speed check never started: the cameras at its ends showed as speed cameras, but the check itself never began. The checks are now looked up in a small request of their own, with more patience for slow servers and a retry after 20 seconds instead of 60.
- Kameras einer Abschnittskontrolle werden nicht mehr als einzelne Blitzer angesagt.
  EN: The cameras of an average speed check are no longer announced as spot speed cameras.
- Tempolimits ohne Einheit: Solange das Land noch nicht bekannt war, entschied die Spracheinstellung des Geräts über km/h oder mph. Jetzt zählt nur noch die Position.
  EN: Speed limits without a unit: until the country was known, the device's language setting decided between km/h and mph. Now only the position counts.

## 1.0.246 — 2026-10-28

### Improved in-app updates
- Show the installed and available app versions in the update prompt.
- Display release notes in the update prompt when the release provides them.
- Show download progress as a percentage and downloaded/total megabytes.
- Improve update failure messages, including checksum verification failures, timeouts, and cancellations.
- Only open the APK in a browser when the device has no download service, avoiding a browser fallback that could bypass checksum verification after other download failures.
- Keep update detection working if `version.json` is missing or malformed by falling back to the version number in the release title or notes.

## Historical versions

Earlier builds were published by replacing assets on the single `app` release. Their original release notes and assets may not be recoverable from GitHub Releases. The new versioned-release history starts with the first successful publish using this workflow.
