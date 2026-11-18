## Unreleased

### Verbessert
- Navigation starten auf dem Handy: Der Knopf bekommt den größeren Teil der Zeile und bleibt in einer Zeile. „Route überfliegen“ darf auf zwei Zeilen umbrechen, so werden lange Wörter (etwa auf Niederländisch) nicht mehr abgeschnitten. (Dank an smikkelvriend)
  EN: Start navigation on phones: it gets the bigger share of the row and stays on one line, and Fly over route may use two lines, so long wordings (like Dutch) are no longer cut off. (Thanks to smikkelvriend)

### Behoben
- Kraftstoffkosten auf den Routenoptionen: Ein Preis in Euro oder Pfund pro Liter (2.15 oder 2,15) wird jetzt richtig gelesen. Vorher wurde er als 2.15 Cent gelesen. Dasselbe gilt für den Strompreis. Cent (215) funktioniert weiter. (Dank an smikkelvriend)
  EN: Fuel cost on the route options: a price typed in euros or pounds per litre (2.15 or 2,15) is understood now, as well as cents or pence (215). It used to be read as 2.15 cents. The same goes for the electricity price. (Thanks to smikkelvriend)
- Einstellungen mit Bildschirmtastatur: Die Tastatur verdeckt das Feld nicht mehr, die Einstellungen scrollen darüber. (Android 11 und neuer, mit Vollbild.) (Dank an smikkelvriend)
  EN: Settings with the on-screen keyboard: the keyboard no longer covers the box you're typing in; the settings scroll above it (Android 11 and newer, with full screen on). (Thanks to smikkelvriend)

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
