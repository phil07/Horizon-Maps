## Unreleased

### New
- Android Auto folgt dem Auto wieder von selbst: Wenn du die Karte im Auto von Hand verschiebst und das Auto dann weiterfährt, folgt die Karte ein paar Sekunden später wieder dem Auto. So verschwindet der Pfeil nicht vom Bildschirm.
  EN: Android Auto follows the car again by itself: if you move the map by hand and the car drives on, the map follows the car again a few seconds later, so the arrow doesn't leave the screen.

### Fixed
- Kosten für Sprit auf den Routenkarten: „≈ £2,10“ statt „Sprit etwa £2,10“. Das Zapfsäulen-Symbol zeigt schon, worum es geht, und so wird der Betrag bei zwei oder drei Routen nebeneinander nicht abgeschnitten.
  EN: Fuel cost on the route cards: "≈ £2.10" instead of "Fuel about £2.10", as the pump icon already says what it is, so the amount isn't cut off with two or three routes side by side.

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
