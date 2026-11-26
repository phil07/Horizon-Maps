## Unreleased

### Changed
- Ein Tempolimit bleibt im Stau gemerkt, solange du stehst. Es fällt erst weg, wenn du dich mehr als 3 km entfernst.
  EN: A speed limit stays remembered while you stand in a jam. It only goes once you are more than 3 km away.
- Auf dem Auto-Display sind die Suchleiste und die Schnellzugriffe deckender.
  EN: On the car display, the search bar and the shortcuts are more solid.
- Ein Tipp auf einen benannten Ort in der Karte öffnet die Ortskarte mit dem Namen, auch bei der OpenStreetMap-Karte.
  EN: A tap on a named place on the map opens its place card with the name, also on the OpenStreetMap map.

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
