## Unreleased

### Improved
- Ein einfacher Tipp auf die Karte macht nichts mehr, auch nicht auf eine freie Stelle oder einen Ort. Langes Drücken öffnet die Karte des Ortes mit Name, „Los“ und „Als Stopp“.
  EN: A simple tap on the map does nothing now, on a free spot or on a place. A long press opens the card of the place with its name, "Los" and "Als Stopp".


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
