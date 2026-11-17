## Unreleased

### Neu
- GPX-Ordner und Suche: Tracks lassen sich unter Einstellungen › Orte & Fahrten › GPX-Tracks in Ordnern zusammenfassen, die sich auf- und zuklappen (Ordner-Knopf an jedem Track). In der Suchleiste findest du einen Track über seinen Namen. (Dank an bijindas)
  EN: GPX folders and search: file your tracks in folders that fold open (the folder button on each track, under Settings › Places & trips › GPX tracks), and type a track's name in the search bar to find it. (Thanks to bijindas)
- Himmelsrichtung getrennt: „Richtung E“ hinter dem Straßennamen ohne Route hat jetzt einen eigenen Schalter, damit nur die Straße bleibt (Einstellungen › Bildschirm & Größen › Beim Fahren). (Dank an liamstears)
  EN: Road name and heading switch separately: "Heading E" after the road name when driving without a route now has its own switch, so you can keep just the road (Settings › Screen & sizes › While driving). (Thanks to liamstears)

### Verbessert
- Orte in einer Reihe: Eigene Orte, Favoriten und gespeicherte Routen (GPX-Tracks) stehen alle in der scrollbaren Reihe unter der Suche, die immer mit „Ort hinzufügen“ endet. Die Liste darunter zeigt nur die letzten Orte.
  EN: Saved places all in one row: your own places, favourites and saved routes are now all in the scrolling row under the search bar, which always ends with an "Add a place" tile. The list below it is just recent places.
- Vorbereitung für Google Play: Der Build kann jetzt auch eine Google-Play-Version namens „Horizon Maps“ erstellen. Sie wird neben dieser installiert und von Play aktualisiert, ohne eigenen Updater. In dieser Version ändert sich nichts.
  EN: Getting ready for Google Play: the build can now also make a Google Play edition of the app, called Horizon Maps, installed beside this one and updated by Play, without the self-updater. Nothing changes in this version.

### Behoben
- Google Play Protect blockiert die Installation nicht mehr: Titel und Cover der Medien-Leiste brauchten Benachrichtigungszugriff, den Play Protect bei Apps aus einer Datei sperrt. Die GitHub-Version behält Zurück, Pause und Weiter ohne Titel. (Dank an desko1973)
  EN: Google Play Protect no longer blocks installing: the media bar's song and artwork needed notification access, which Play Protect blocks in apps installed from a file. The GitHub edition's media bar keeps back, play / pause and next without the song. (Thanks to desko1973)
- Fernlicht von unter dem Auto: Der Lichtkegel ist jetzt ein einziger Kegel in etwa halber Helligkeit (er sah aus wie zwei übereinander) und startet unter dem Auto, damit er von den Scheinwerfern zu kommen scheint und nicht von der Windschutzscheibe. (Dank an liamstears)
  EN: One headlight beam, from under the car: the beam is now a single cone at about half the brightness (it looked like two beams on top of each other), starting under the car so it seems to come from the headlights rather than the windscreen. (Thanks to liamstears)

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
