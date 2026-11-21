## Unreleased

### New
- Zuhause, Arbeit und markierte Orte aus Deine Orte erscheinen auf der Karte: Zuhause als blaues Haus, Arbeit als gelber Koffer, markierte Orte als Stern. Ein Tipp öffnet den Ort mit „Los“. Die Größe lässt sich unter Einstellungen › Karte › Symbole gespeicherter Orte einstellen.
  EN: Home, Work and starred places from Your places now show on the map: Home as a house on blue, Work as a briefcase on amber, starred places as a star. A tap opens the place with "Go". The size is under Settings › Map › Saved places icons.

### Fixed
- Die grauen Straßen am Abbiegepfeil stehen jetzt auf 45 Grad genau, auf der Seite, auf der sie wirklich liegen, statt in einem schiefen Winkel neben dem Pfeil.
  EN: The faded roads beside the turn arrow now sit at 45 degrees, on the side they really are, instead of at an odd angle beside the arrow.
- Der Pfeil bleibt in der Mitte, wenn die Suchliste eingeklappt ist: Was oberhalb des Pfeils endet, schiebt ihn nicht mehr zur Seite.
  EN: The arrow stays in the middle when the recents panel folds away: anything that ends above the arrow no longer pushes it to the side.

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
