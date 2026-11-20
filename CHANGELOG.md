## Unreleased

### New
- Favoriten nach Deine Orte verschieben: Jeder Favorit hat „In Deine Orte“, und „Alle verschieben“ legt alle in einen Ordner „Favoriten“ unter Deine Orte. Die Liste neben Zuhause und Arbeit bleibt so kurz.
  EN: Move favourites to Your places: each favourite has "In Deine Orte", and "Alle verschieben" puts all of them into a "Favoriten" folder under Your places, so the row next to Home and Work stays short.
- Zuhause und Arbeit: „Hier verwenden“ setzt den Ort auf deine aktuelle Position und zeigt die gefundene Adresse. So kannst du sie auch im Parkzustand speichern.
  EN: Home and Work: "Hier verwenden" sets the place to where you are now and shows the address it found, so you can save it while parked.

### Improved
- Deine Orte: Die Ordner stehen in A–Z-Reihenfolge, die Orte darin ebenfalls A–Z, bis die App deine Position kennt. Danach kommen die nächsten zuerst.
  EN: Your places: the folders are in A–Z order, and the places in them are A–Z until the app knows where you are; then the nearest come first.

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
