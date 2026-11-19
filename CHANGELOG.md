## Unreleased

### Verbessert
- Ortskarte unten statt mitten auf der Karte: Tippst du auf ein Suchergebnis, schließt sich die Tastatur, die Karte fährt zum Ort, und die Karte öffnet sich unten am Bildschirm (auf dem Head Unit oder quer gehaltenem Handy an der Fahrerseite). Bei Tag- und Nachtfarben.
  EN: Place card docked like Add a stop: tapping a search result closes the keyboard, moves the map to the place and opens its card along the bottom of the screen (or down the driver's side on a head unit or a phone held sideways). Works in day and night colours.
- Ankunftskarte: Restzeit, Ankunft und verbleibende Strecke stehen jetzt zusammen links auf der Karte statt verteilt über die ganze Karte.
  EN: Arrival card figures grouped: time left, arrival and distance to go now sit together on the left of the card instead of spread across it.

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
