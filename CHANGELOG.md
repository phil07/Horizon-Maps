## Unreleased

### New
- Favoriten auf der Karte: Deine gespeicherten Orte erscheinen als gelbe Sterne mit ihrem Namen. Ein Tipp auf einen Stern zeigt seine Karte mit „Los“. Beim Fahren ohne Namen. Ausschalten unter Einstellungen › Karte › Favoriten auf der Karte.
  EN: Favourites on the map: your saved places show as yellow stars with their names. A tap on a star shows its card with "Go". While driving, without names. Switch it off under Settings › Map › Favourites on the map.
- Beim Speichern eines Favoriten erscheint ein Feld mit dem Namen, den du ihm selbst geben kannst.
  EN: When you save a favourite, a box with its name appears, so you can call it something of your own.

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
