## Unreleased

### Neu
- Horizon Nav schließen: Ein Knopf unten in den Kartenoptionen beendet die Route, schaltet das GPS ab und schließt die App, damit nichts im Hintergrund weiterläuft. (Dank an pc4096)
  EN: Close Horizon Nav: a button at the bottom of Map options ends the route, stops the GPS and closes the app, so nothing keeps running in the background. (Thanks to pc4096)
- Markierte Orte: Unter Einstellungen › Orte & Fahrten › Eigene Orte bekommt jeder Ort einen Stern. Markierte stehen in der Reihe unter der Suche, neben Zuhause und Arbeit. Ein Schalter blendet sie alle aus. Jeder Ort bleibt in der Suche und unter „Meine Orte“. (Dank an bijindas)
  EN: Starred places: under Settings › Places & trips › Your places each place has a star. Starred ones show in the row under the search bar, next to Home and Work, and a switch hides them all. Every place stays in search and in "My places". (Thanks to bijindas)
- Ordner für Orte und Tracks: „Meine Orte“ und „Meine Tracks“ zeigen zuerst deine Ordner, dann, was in keinem Ordner ist. Ein Tipp öffnet einen Ordner, ‹ geht zurück. Auch auf dem Autodisplay. (Dank an bijindas)
  EN: Folders in My places and My tracks: the lists show your folders first, then what is in no folder. Tap a folder to open it, ‹ goes back. Also on Android Auto. (Thanks to bijindas)

### Verbessert
- Update-Karte: Sie sieht aus wie die Neuerungen-Karte und zeigt deine Version und die neue, dazu, wie viele Änderungen neu, verbessert und behoben sind. „Was drin ist“ klappt die Überschriften aller Änderungen seit deiner Version auf. Sie passt auch auf einen kurzen Querformat-Bildschirm.
  EN: Update card: styled like the What's new card, showing your version and the new one, and how many changes are new, improved and fixed. "What's in it" folds open to the headlines of every change since your version. It fits a short landscape screen too.
- Kraftstoffkosten auf jeder Route: Jede Routenkarte zeigt, was die Route ungefähr an Sprit (oder Strom) kostet. Vorher stand das am Ende der Stopp-Reihe, außer Sicht. (Dank an pc4096)
  EN: Fuel cost on each route: every route card shows roughly what the route costs in fuel (or charging). It used to sit at the far end of the Stops row, out of sight. (Thanks to pc4096)

- Einstellungen aufgeräumt: „Beim Fahren“ hat eine eigene Seite mit Straßenname, Tacho, Tunnel, Medien und Sperren. „Bildschirm & Größen“ ist dadurch kürzer. Das Wetter steht bei Licht und 3D. Alles zur Suchleiste ist in einer Gruppe „Unter der Suchleiste“. Die Einstellungssuche findet alles am neuen Ort.
  EN: Tidier Settings: While driving has its own page with road name, speedometer, tunnels, media controls and locking, so Screen & sizes is shorter. Weather sits with the light and 3D. Everything about the search bar is in one group, "Under the search bar". Settings search finds everything in its new place.

### Behoben
- Ankunft auf großen Parkplätzen: Wer innerhalb von 250 m vom Ziel eine Minute steht, ist angekommen. Die Route endet, wenn du parkst, nicht erst am Eingang des Ladens. (Dank an pc4096)
  EN: Arriving in a big car park: stopping for a minute within 250 m of the destination counts as arriving, so the route ends when you park, not only at the shop's front door. (Thanks to pc4096)
- Routen, die im Hintergrund weiterlaufen: Eine Route, die nach einer halben Stunde Stillstand mit weggelegter App noch läuft, endet von selbst, und das GPS stoppt. (Dank an pc4096)
  EN: Routes left running in the background: a route still going after half an hour parked, with the app put away, now ends by itself, and the GPS stops. (Thanks to pc4096)

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
