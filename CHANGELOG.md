## Unreleased

### Fixed
- Android Auto im geteilten Bildschirm: Wenn die Karte als Kachel neben Musik oder einer anderen App steht, ordnet die App Abbiegekarte, Fahrtleiste, Tacho und Knöpfe jetzt innerhalb dieser Kachel an und passt sie an ihre Größe an. Vorher lagen sie teils unter der anderen Kachel, und die Fahrtleiste war links abgeschnitten. Pfeil und Route bleiben in der Kachel sichtbar.
  EN: Android Auto split screen: when the map is a card beside music or another app, the turn card, trip bar, speed and buttons are now laid out within that card and fit its size. Before, some of them lay under the other card and the trip bar was cut off on the left. The arrow and the route stay visible in the card.

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
