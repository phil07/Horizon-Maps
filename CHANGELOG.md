## Unreleased

### Fixed
- Problemberichte sagen, wie die App tatsächlich beendet wurde: Android's eigene Aufzeichnung (weggewischt, wegen zu wenig Speicher geschlossen, Absturz im Kartencode, keine Reaktion) steht jetzt im Bericht. Wegwischen zählt nicht mehr als „von Android geschlossen“ und auch nicht zum Herunterstellen der Zeichnung.
  EN: Problem reports say how the app really ended: Android's own record (swiped away, closed for low memory, crashed in the map's code, not responding) is now in the report. Swiping the app away no longer shows as "closed by Android" and no longer counts towards turning the drawing down.

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
