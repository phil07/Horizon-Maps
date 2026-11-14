## Unreleased

### Behoben
- Android Auto, Knöpfe: Die Leiste links ist schmal und rund (nur Symbole) und überdeckt nichts mehr. Sie hat Beenden, Melden und den Ladesäulen-Filter. Größer, Kleiner und Zentrieren übernehmen die Knöpfe von Android Auto rechts, die kompakter sind. Die Karten (Abbiegekarte, Suche, Fahrtkarte) rücken rechts neben die Leiste. Bei Autos, die Berührungen weitergeben, steht in der Leiste oben rechts nur noch ein Knopf (Einstellungen, auf der Fahrt 2D/3D), weil Android Auto mindestens einen verlangt.
  EN: Android Auto, buttons: the column on the left is narrow and round (icons only) and covers nothing. It has End, Report and the charger filter. Zoom in, zoom out and Re-centre are Android Auto's own buttons on the right, which are more compact. The cards (turn, search, trip) move to the column's right. On cars that pass touches on, the bar at the top right has just one button (Settings, while driving 2D/3D), as Android Auto requires at least one.
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
