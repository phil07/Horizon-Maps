## Unreleased

### Improved
- Android Auto während der Fahrt: Das Zahnrad oben links ist weg, die Einstellungen bleiben unten links bei Zoom und Zentrieren. Oben steht jetzt das rote ✕ zum Beenden, die Spalte darunter beginnt mit Melden. Die Abbiegekarte und die Fahrtleiste unten rücken direkt neben die Knöpfe, ohne große Lücke.
  EN: Android Auto while driving: the gear at the top left is gone; Settings stays at the bottom left with zoom and re-centre. The top now holds the red ✕ to end the trip, and the column below starts with Report. The turn card and the trip bar at the bottom sit right beside the buttons, without a big gap.

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
