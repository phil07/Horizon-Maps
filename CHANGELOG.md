## Unreleased

### New
- Schnellere Route: Das Angebot „Schnellere Route gefunden“ steht als Streifen oben auf der Fahrtkarte, mit Wechseln und Behalten in einer Zeile. Im Hochformat steht es oben im Fahrt-Blatt.
  EN: A faster route on offer is a strip across the top of the trip card, with Switch and Keep on one line. Upright, it is at the top of the trip sheet.
- Warnungen auf der Route stehen als eine kurze Zeile unter der Abbiegekarte statt als großes Banner. Außerhalb der Route bleibt das Banner.
  EN: On a route, alerts are one short line under the turn card instead of the big banner. Off the route the banner stays.
- Android Auto: Die Hinweise unter der Abbiegekarte sind kleine Zeilen wie auf dem Telefon, statt Karten.
  EN: Android Auto: the hints under the turn card are small chips in one line, as on the phone, instead of cards.
- „Außerhalb deiner Karten“ ist antippbar und öffnet die Offline-Karten in den Einstellungen.
  EN: "Outside your maps" is tappable and opens Offline maps in Settings.

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
