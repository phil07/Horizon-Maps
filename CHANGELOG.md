## Unreleased

### Fixed
- TomTom-Meldungen: Fast jede Abfrage nach Unfällen, Baustellen und Sperrungen bei TomTom wurde abgelehnt, weil die App ein Feld unter einem Namen abfragte, den TomTom nicht kennt. Jetzt kommen sie durch, erscheinen auf der Karte und werden auf der Route wieder angesagt. Danke an bijindas, der es in TomToms Nutzungsstatistik entdeckt hat.
  EN: TomTom incidents: almost every check for accidents, roadworks and closures from TomTom was being turned down, because the app asked for a detail under a name TomTom doesn't use. They now come through, show on the map and are announced on your route again. Thanks to bijindas, who spotted it in TomTom's usage stats.

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
