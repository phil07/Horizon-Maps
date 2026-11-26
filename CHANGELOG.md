## Unreleased

### Improved
- Das Land wird nur noch nach 15 km Fahrt neu geprüft, nicht alle zwei Minuten. Das spart Kartenabfragen und Daten auf dem Head Unit.
  EN: The country is checked again only after driving 15 km, not every two minutes. This saves map lookups and data on head units.
- Die Fahrt-Fortsetzung wird in einer kleinen Datei gespeichert, nicht mehr alle 20 Sekunden in alle Einstellungen.
  EN: The trip resume state is saved in a small file of its own, not rewritten into all the settings every 20 seconds.

### Fixed
- Die Anweisungen zu Wegpunkten („Wegpunkt erreicht“, „Wegpunkt verlassen“) aus dem Anbieter werden bei Meine Routen nicht mehr angesagt oder angezeigt. Echte Abbiegungen und echte Stopps bleiben.
  EN: The provider's waypoint lines ("waypoint reached", "waypoint left") are no longer shown or spoken for My routes. Real turns and real stops stay.
- Ankunftskarte: Wenn du sie schließt, springt der Pfeil zurück zum Auto.
  EN: Arrival card: when you dismiss it, the arrow returns to the car.

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
