## Unreleased

### New
- Bildschirm auch ohne Route anlassen: Einstellungen › Bildschirm. Der Bildschirm bleibt an, wenn du nur die Karte und den Verkehr ansiehst. Aus, weil es mehr Akku braucht.
  EN: Keep the screen on without a route: Settings › Screen. The screen stays on while you just watch the map and traffic. Off, as it uses more battery.
- Farbe der Abbiegekarte: Einstellungen › Kartenansicht › Abbiegekarte › Farbe. Grün, Schwarz, Dunkelgrau, Navy oder Lila, mit weißer Schrift.
  EN: Turn card colour: Settings › Map look › Turn card › Colour. Green, black, dark grey, navy or purple, with white writing.

### Improved
- Kompass: Die rote Nordmarke wandert am Rand mit der Karte und zeigt immer nach echtem Norden.
  EN: Compass: the red north mark moves round the edge with the map and always points to real north.
- Suche: Der Ausklapp-Pfeil auf der Suchleiste zeigt sich nur noch im Querformat.
  EN: Search: the fold-down arrow on the search bar now only shows in landscape.

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
