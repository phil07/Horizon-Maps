## Unreleased

### Neu
- GPX-Tracks: Eine .gpx-Datei (von Komoot, Strava, Garmin oder einer eigenen Aufnahme) lässt sich unter Einstellungen › Orte & Fahrten › GPX-Tracks importieren. „Zeigen“ legt sie auf die Karte, „Fahren“ führt dich bis zu ihrem Ende mit den üblichen Sprachansagen, ab der Stelle, an der du dich einfädelst. Eine Neuberechnung hält dich auf der Spur. (Dank an bijindas)
  EN: GPX tracks: import a .gpx file (from Komoot, Strava, Garmin or your own recording) under Settings › Places & trips › GPX tracks. Show puts it on the map, Drive takes you to its end with the usual spoken directions, carrying on from where you join it. A reroute keeps you on the track. (Thanks to bijindas)
- Suche als Knopf: Ohne Route klappt die Suchleiste mit ihren Knöpfen in einen runden Suchknopf in der Ecke zusammen. Die Karte hat den ganzen Bildschirm, dein Pfeil steht in der Mitte. Einstellungen › Orte & Fahrten › Suchleiste. (Dank an vishnauth-bit)
  EN: Search as a button: with no route, the search bar and its buttons fold into one round search button in the corner. The map has the whole screen and your arrow sits in the middle. Settings › Places & trips › Search bar. (Thanks to vishnauth-bit)
- Automatischer Zoom aus: Die Karte bleibt auf einem gleichen Zoom, statt sich nach Tempo und Kreuzungen zu richten. + und − wirken weiterhin. Einstellungen › Karte › Standard-Zoom. (Dank an vishnauth-bit)
  EN: Auto zoom can be turned off: the map keeps one steady zoom instead of following your speed and the junctions, and + and − still change it. Settings › Map style, under Default zoom. (Thanks to vishnauth-bit)
- Medien-Steuerung: Zurück, Pause und Weiter unten auf dem Fahrbildschirm, solange auf dem Autodisplay Musik oder Radio läuft, aus jeder App. Mit Benachrichtigungszugriff (Einstellungen › Bildschirm & Größen › Beim Fahren › Song und Cover) werden auch Titel, Interpret und Cover angezeigt. Ausschaltbar am selben Ort. (Dank an mrEnmi)
  EN: Media controls: back, play / pause and next at the bottom of the driving screen while music or radio plays on the head unit, from any app. With notification access (Settings › Screen & sizes › While driving › Song and artwork) the song, artist and artwork show too. It can be turned off in the same place. (Thanks to mrEnmi)

### Verbessert
- Schnellere Neuberechnung: Ein falscher Abbieger wird schon beim ersten GPS-Wert bemerkt, wenn du klar auf einer anderen Straße bist. Die neue Route wird zweimal gleichzeitig angefragt (eine frühe Anfrage und eine frische von deiner Position), die schnellere Antwort gilt. Eine frühe Anfrage von weiter zurück wird nicht mehr genutzt, sie würde hinter dir starten. (Dank an karthikeyansr-lgtm)
  EN: Quicker rerouting: a wrong turn is now noticed on the first GPS reading when you're clearly on another road, and the new route is asked for twice at once (the early request and a fresh one from where you are), using whichever answers first. An early request made from too far back is no longer used, as it gave a route starting behind you. (Thanks to karthikeyansr-lgtm)

### Behoben
- Fernlicht hinter dem Auto: Nachts begann der Lichtkegel weit hinter dem Pfeil und leuchtete nach hinten über ihn. Er kommt jetzt von der Stelle des Autos aus, leuchtet die Straße vor dir aus und liegt bei jeder Neigung flach auf der Straße. (Dank an liamstears)
  EN: Headlight beam behind the car: at night the beam started well behind the arrow and shone back over it. It now comes from the car's own spot, lights the road ahead and lies flat on the road at any tilt. (Thanks to liamstears)

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
