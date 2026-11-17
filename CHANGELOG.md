## Unreleased

### Behoben
- Pfeil läuft in die Kreuzung: Beim Anhalten an einer Kreuzung wird der Pfeil nicht mehr auf die Straße gesetzt, in die du abbiegst. Er bleibt, bis das GPS sagt, wo du wirklich bist. (Dank an jvricardo1)
  EN: Arrow running into the junction: stopping at a junction no longer leaves your arrow out in the middle of the road you are turning into. It stays until the GPS says where you really are. (Thanks to jvricardo1)
- Falsche Fahrtrichtung auf der Route: Wenn du umdrehst oder die Route andersherum fährst, wird nach etwa 2 Sekunden eine neue Route gesucht (vorher 5). (Dank an jvricardo1)
  EN: Driving the route the wrong way: turning round, or setting off the other way along the route, now finds a new route after about 2 seconds (it was 5). (Thanks to jvricardo1)
- Karte lädt nicht: Lädt die Karte nach 25 Sekunden ohne Fehlermeldung nicht, fragt die App Mapbox selbst und nennt die Ursache: einen blockierten Server (privates DNS oder Werbeblocker), die Uhrzeit des Autodisplays, das Token oder die Verbindung.
  EN: Map not appearing: if the map still hasn't loaded after 25 seconds with no error, the app asks Mapbox itself and names the cause: a blocked server (private DNS or an ad blocker), the head unit's clock, the token or the connection.

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
