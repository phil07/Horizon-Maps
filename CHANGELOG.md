## Unreleased

### Added
- Autobahnen vermeiden: Nie, Immer oder nur auf kurzen Fahrten (Länge einstellbar, 15 bis 80 km oder 10 bis 50 Meilen). Offline-Routen meiden Autobahnen jetzt auch.
  EN: Avoid motorways: Never, Always or Short trips only (the length is set, 15 to 80 km or 10 to 50 miles). Offline routes now keep off motorways too.
- Zoom an Kreuzungen (Einstellungen › Spuren und Kreuzungen): Aus, Weiter, Näher oder Am nächsten.
  EN: Junction zoom (Settings › Lanes and junctions): Off, Further, Closer or Closest.
- Gefahrenschilder auf der Karte (Baustellen, Sperrungen, Gefahren) haben eine eigene Größeneinstellung und sind etwas größer.
  EN: Roadworks, closure and hazard signs on the map have their own size setting and are a little bigger.

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
