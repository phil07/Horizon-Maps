## Unreleased

### Improved
- Ein Tipp oder langes Drücken auf die Karte macht nichts mehr, auch nicht auf Android Auto. Einen Ort auf der Karte wählst du jetzt über die Suche: „Auf der Karte wählen“ zeigt ein Fadenkreuz in der Mitte der Karte. Der Ort darunter wird mit „Los“ als Ziel oder mit „Als Stopp“ als Zwischenziel übernommen.
  EN: A tap or a long press on the map does nothing now, on Android Auto too. To pick a spot on the map, use the search: "Pick on the map" shows a crosshair in the middle of the map. The spot under it is taken as the destination with "Go" or as a stop with "Add stop".


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
