## Unreleased

### Neu
- Ziel bei laufender Fahrt: Wählst du einen Ort aus der Suche (getippt oder per Sprache) oder bei Android Auto, fragt die App jetzt, ob er als Zwischenziel hinzukommt oder das aktuelle Ziel ersetzt. Auf dem Handy erscheint die Frage als kleine Karte und verschwindet nach 15 Sekunden ohne Antwort, das alte Ziel bleibt dann. Auf Android Auto bleibt sie stehen, bis du wählst.
  EN: Destination while driving: when you pick a place from the search (typed or by voice) or on Android Auto, the app now asks whether it is added as a stop or replaces the current destination. On the phone the question is a small card that goes away after 15 seconds without an answer, and the old destination stays. On Android Auto it stays until you choose.
- Sprachbefehl für Zwischenziele: „Zwischenziel …“ oder „Fahre zusätzlich nach …“ fügt den Ort direkt als Zwischenziel hinzu.
  EN: Voice command for stops: "Zwischenziel …" or "Fahre zusätzlich nach …" adds the place straight away as a stop.

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
