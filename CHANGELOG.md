## Unreleased

### New
- Straßennummer auf der Abbiegekarte: Eine Nummer mit Buchstabe wie „B 44“ oder „E39“ steht als kleines Badge neben dem Straßennamen (außerhalb des Vereinigten Königreichs).
  EN: Road number on the turn card: a number with a letter, such as "B 44" or "E39", shows as a small badge beside the street name (outside the United Kingdom).

### Improved
- Baustellen und Spur-Sperrungen erscheinen nur als Zeile auf der Abbiegekarte, ohne zusätzliche Warnung und Ansage.
  EN: Roadworks and lane closures show only as the line on the turn card, without an extra warning or spoken line.
- Zahlen auf der Android-Auto-Fahrtkarte teilen sich die Breite, eine lange Zahl wird etwas kleiner, statt in die nächste zu laufen.
  EN: The figures on the Android Auto trip card share the width; a long figure shrinks a little instead of running into the next.
- Der Pfeil auf der Karte ohne Route wird leicht aus der Mitte geschoben, wenn die Liste „Gespeichert & zuletzt“ eingeklappt ist (etwa ein Drittel der Verschiebung bei aufgeklappter Liste).
  EN: With no route and the "Saved & recent" list folded away, the arrow is moved slightly off centre (about a third of the shift with the list open).

### Fixed
- Die Kanten des Pfeils auf der Routenleiste sind wieder glatt: Das Bild wird in Stufen verkleinert.
  EN: The arrow on the route bar has smooth edges again: the picture is scaled down in steps.
- Die Karte nutzt die 16-KB-Seitengröße, die Google Play verlangt: Mapbox wird in der 16-KB-Variante derselben Version (11.32.1) eingebunden.
  EN: The map uses the 16 KB page size that Google Play requires: Mapbox is built in the 16 KB variant of the same version (11.32.1).

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
