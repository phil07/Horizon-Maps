## Unreleased

### Improved
- Tippen auf die Karte wählt wieder einen Ort: Ein Tipp auf eine freie Stelle oder auf einen Ort zeigt die Karte mit Name, „Los“ und „Als Stopp“. Die Auswahl per Fadenkreuz („Auf der Karte wählen“) ist wieder entfernt.
  EN: Tapping the map picks a place again: a tap on a free spot or on a place shows the card with its name, "Go" and "Add stop". The crosshair selection ("Pick on the map") is removed again.

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
