## Unreleased

### Improved
- Geschwindigkeitswechsel auf der Karte: Hat die Route für den aktuellen Abschnitt keine Grenze, kommt der nächste Wechsel jetzt aus den Live-Straßendaten, statt zu fehlen.
  EN: Speed-limit changes on the map: when the route has no limit for the current stretch, the next change now comes from the live road data instead of being missing.

### Fixed
- Haltestellen-Schilder auf der Karte: Nach einer neuen Route verschwinden die Schilder der alten Route sofort, und späte Antworten zur alten Route werden nicht mehr angezeigt.
  EN: Stop signs on the map: after a new route the old route's signs disappear at once, and late replies for the old route are no longer shown.

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
