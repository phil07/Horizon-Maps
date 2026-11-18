## Unreleased

### Neu
- Uhr auf der Karte: Eine kleine Uhr unter dem Tacho, für Geräte, deren eigene Statusleiste verdeckt ist. Einstellung unter Beim Fahren › Uhr auf der Karte. (Dank an bijindas)
  EN: Clock on the map: a small clock under the speedometer, for head units whose own status bar is covered. Setting under While driving › Clock on the map. (Thanks to bijindas)

### Verbessert
- GPX-Tracks in der Sicherung: Sichern und Wiederherstellen umfassen jetzt auch deine GPX-Tracks mit ihren Ordnern, wie schon deine Einstellungen und Orte. (Dank an bijindas)
  EN: GPX tracks in backups: Back up and Restore now include your GPX tracks with their folders, as well as your settings and saved places. (Thanks to bijindas)
- „Navigieren“ statt „Fahren“: Der Knopf, der einen Track fährt, heißt jetzt Navigieren. (Dank an bijindas)
  EN: "Navigate" for GPX tracks: the button that drives a track says Navigate instead of "Drive". (Thanks to bijindas)

### Behoben
- Nachtkarte am Tag auf dem Handy: Mit eingeschaltetem Dunkelmodus des Telefons blieb die Karte bei Auto den ganzen Tag dunkel. Auf dem Handy folgt Auto jetzt Sonnenauf- und -untergang. Neu: „Dunkelmodus des Telefons“ richtet die Karte nach der Hell- oder Dunkeleinstellung des Telefons. (Dank an lukaszsakul)
  EN: Night map by day on phones: with the phone's dark mode on, Auto kept the map dark all day. On a phone, Auto now follows sunrise and sunset, and a new "Phone's dark mode" choice matches the phone's own light or dark setting. (Thanks to lukaszsakul)
- Durchschnittsgeschwindigkeit auf dem Autodisplay: Solange die Karte mit deinem Durchschnitt, dem Limit und dem Rest läuft, ersetzt sie die Blitzer-Zeile. Auf hellen Karten sind die Farben dunkler. (Dank an magnazan)
  EN: Average speed checks on Android Auto: while the average speed card shows, it takes the place of the speed camera line, and its colours are deeper on a light card. (Thanks to magnazan)
- Stoppschilder auf Seitenstraßen: Die Warnung „Stoppschild voraus“ kommt nur noch für Schilder vor dir auf deiner Straße, nicht mehr für die an Seitenstraßen, an denen du vorbeifährst. (Dank an dubrava4ever)
  EN: Stop signs on side roads: the "Stop sign ahead" warning only goes off for signs ahead on your own road, not for those on the side streets you pass. (Thanks to dubrava4ever)

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
