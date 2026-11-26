## Unreleased

### Fixed
- Ankunftskarte: Wenn du sie schließt, springt der Pfeil zurück zum Auto, statt auf der Zielansicht stehen zu bleiben.
  EN: Arrival card: when you dismiss it, the arrow returns to the car instead of staying on the destination view.
- Tunneln: Bei aktiver Route werden Standort-Meldungen aus dem Netz ohne oder mit schlechter als 30 m Genauigkeit auch nach zwei Minuten ohne GPS ignoriert. Die Schätzung läuft weiter wie bisher, nur für zwei Minuten.
  EN: Tunnels: while a route is active, network-location fixes with missing or worse-than-30 m accuracy are ignored even after two minutes without GPS. The estimate still runs for two minutes as before.

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
