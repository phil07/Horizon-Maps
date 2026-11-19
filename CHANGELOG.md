## Unreleased

### Verbessert
- Spur-Bild vor einer Kreuzung deutlicher: Die Pfeile, die Spurlinien und die Straßenränder sind dicker, damit man es auf einen Blick liest. Auf Straßen mit vielen Spuren bleiben die Pfeile etwas schmaler, damit sie nicht ineinander laufen. (Dank an karthikeyansr-lgtm)
  EN: Bolder lane view: the big lane picture before a junction has thicker arrows, lane markings and road edges, so it reads at a glance. On roads with many lanes the arrows stay a little slimmer so they don't run into each other. (thanks to karthikeyansr-lgtm)
- Gefundene Orte leuchten auf: Wählst du ein Suchergebnis, zentriert die Karte den Ort im Bereich, den die Suchleiste nicht belegt. Sein Gebäude wird hervorgehoben (bei Nacht heller, und mit den 3D-Gebäuden erhöht). Orte ohne Gebäude zeigen nur die Nadel.
  EN: Searched places lit up: pick a search result and the map now centres the place in the part of the screen the search bar leaves clear, with its building highlighted (lighter at night, and raised with the 3D buildings). Places without a building just show the pin.

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
