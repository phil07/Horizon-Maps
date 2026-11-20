## Unreleased

### Added
- Verkehrsmeldungen in den Niederlanden: Unfälle, Pannen, Sperrungen und Baustellen vom NDW, auf der Karte und auf der Strecke, wie in Spanien (DGT). Im Schalter „Baustellen und Ereignisse“ enthalten.
  EN: Traffic events in the Netherlands: accidents, breakdowns, closures and roadworks from NDW, on the map and on the route, as in Spain (DGT). Included in the "Roadworks and events" switch.
- Unwetterwarnungen in weiteren Ländern (Meteoalarm): auf der Strecke durch Frankreich, Österreich, Italien, Spanien, die Niederlande und weitere Länder mit Warngebieten, gezeigt wie die DWD-Warnungen. Deutschland bleibt beim DWD.
  EN: Severe weather warnings in more countries (Meteoalarm): on the route through France, Austria, Italy, Spain, the Netherlands and other countries with warning areas, shown like the DWD warnings. Germany stays with the DWD.
- Feiertagshinweis: Beim Start einer Route ein kurzer Hinweis, wenn heute oder morgen ein Feiertag ist (mehr Verkehr). Ein Schalter in den Einstellungen.
  EN: Public holiday note: when a route starts, a short note if today or tomorrow is a public holiday (more traffic). A switch in the settings.

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
