## Unreleased

### Fixed
- Das Löschen der letzten Ziele löscht auch die Orte, die die App als deine üblichen Ziele gelernt hat (sie wurden sonst weiter als „Meist um …“ vorgeschlagen).
  EN: Clearing recent destinations also clears the places the app has learned you usually go to, which were still suggested as "Usually around …".
- Dienste ohne Route: Die Suche sieht 30 km um dich herum statt 6 km.
  EN: Services with no route: the search looks 30 km around you instead of 6 km.

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
