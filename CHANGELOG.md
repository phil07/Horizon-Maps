## Unreleased

### Improved
- Kompass: Die rote Nordmarke wandert am Rand mit der Karte und zeigt immer nach echtem Norden, auch wenn du die Karte von Hand drehst oder umschaust.
  EN: Compass: the red north mark moves round the edge with the map and always points to real north, even when you turn the map by hand or look around.
- Suche: Der kleine Ausklapp-Pfeil auf der Suchleiste zeigt sich nur noch im Querformat. Im Hochformat stehen die letzten Orte im Panel darunter.
  EN: Search: the little fold-down arrow on the search bar now only shows in landscape; upright, the recent places are in the pull-up panel.

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
