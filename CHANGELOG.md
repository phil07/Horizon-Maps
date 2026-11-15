## Unreleased

### Neu
- Tacho-Seite auf dem Handy: Unter Einstellungen › Beim Fahren › Tacho-Seite wählst du Rechts (Standard), Links oder Automatisch (links bei Linkslenker, wie bisher). Liegt die Fahrtkarte auf derselben Seite, steht der Tacho darüber.
  EN: Speed side on the phone: under Settings › While driving › Speed side you pick Right (default), Left or Automatic (left for left-hand drive, as before). When the trip card is on the same side, the speed sits above it.

### Verbessert
- Routenauswahl auf dem quer gehaltenen Handy: Die Routenauswahl ist jetzt eine Spalte links, die Routen sind daneben auf der Karte zu sehen. Vorher verdeckte sie fast die ganze Karte.
  EN: Route choice on a phone held sideways: the route panel is now a column down the left with the routes shown beside it, instead of covering nearly the whole map.
- Route überfliegen: Die Routenauswahl macht während des Flugs Platz, mit einem kleinen Stopp-Knopf zum Beenden.
  EN: Fly over route: the route panel steps aside while the flight plays, with a small Stop button to end it.

### Behoben
- Schwarze Karte nach den Einstellungen: Ab und zu war nach dem Schließen der Einstellungen nur der obere Teil der Karte zu sehen, der Rest blieb schwarz. Die Karte wird nach dem Wechsel jetzt auf die volle Größe gebracht.
  EN: Black map after Settings: now and then only the top part of the map showed after closing the settings and the rest stayed black. The map is now brought back to full size after the switch.
- Tempolimit-Schild am Rand: Das Schild steht jetzt immer auf der Seite des Tachos, die zur Bildschirmmitte zeigt, und kann nicht mehr über den Rand hinausragen.
  EN: Speed limit sign off the edge: the sign now always sits on the side of the speed facing the middle of the screen and can no longer run off the edge.

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
