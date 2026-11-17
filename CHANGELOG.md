## Unreleased

### Neu
- Verknüpfungen „Meine Orte“ und „Meine Tracks“: zwei Knöpfe in der Reihe unter der Suche, die eine Liste zeigen, den nächsten Ort oder Track zuerst. Einschalten unter Einstellungen › Orte & Fahrten › Verknüpfungen. (Dank an bijindas)
  EN: "My places" and "My tracks" shortcuts: two buttons in the row under the search bar, each opening a list with the nearest place or track first. Turn them on under Settings › Places & trips › Shortcuts. (Thanks to bijindas)
- Ankunftszeit je Stopp: Der Stopp-Editor auf dem Handy zeigt, wann du bei jedem Stopp bist, und die Fahrtkarte den nächsten Stopp mit seiner Uhrzeit. (Dank an yurividal)
  EN: Arrival time for each stop: the stops editor on the phone shows when you get to each stop, and the trip card shows the next stop with its time. (Thanks to yurividal)
- „Stopp hinzufügen“ per Sprache: „Füg einen Zwischenstopp bei … hinzu“ bzw. „add a stop at …“ fügt während der Fahrt einen Stopp hinzu und behält das Ziel. (Dank an yurividal)
  EN: "Add a stop" by voice: "add a stop at …" (or the German "füg einen Zwischenstopp bei … hinzu") adds a stop while navigating and keeps the destination. (Thanks to yurividal)

### Behoben
- Sprachbefehl „Stopp hinzufügen“ ersetzte das Ziel: Der Befehl warf die Fahrt weg und fuhr zum Stopp. Das Ziel bleibt jetzt. (Dank an yurividal)
  EN: The voice command "add a stop" replaced the destination: it threw the trip away and went to the stop instead. The destination now stays. (Thanks to yurividal)
- Stopp neben der Straße: Ein Stopp mehr als 80 m von der Straße entfernt (eine Nadel mitten auf dem Parkplatz) wurde nie als erreicht gezählt, und die Route fuhr beim nächsten Berechnen wieder dorthin zurück. Er zählt jetzt, sobald du an der Stelle vorbei bist, wo die Route von der Straße abbiegt. (Dank an yurividal)
  EN: Stops set back from the road: a stop more than 80 m from the road (a pin in the middle of a car park) was never counted as reached, so the route went back to it. It now counts once you have driven past the point where the route leaves the road for it. (Thanks to yurividal)

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
