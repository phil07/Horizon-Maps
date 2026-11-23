## Unreleased

### New
- Neues Hochformat-Layout (an unter Einstellungen › Bildschirm): Auf dem hochkant stehenden Bildschirm liegt unten ein Blatt mit Zuhause, Arbeit, deinen Orten, Wetter, günstigstem Sprit, geparktem Auto, Abfahrtszeit „Abfahren 08:04 zur Arbeit“, Straßenname und Musik. Zieh es mit dem Griff hoch für die zuletzt genutzten Orte.
  EN: New portrait layout (on by default under Settings › Screen): on an upright screen a sheet along the bottom holds Home, Work, your places, the weather, the cheapest fuel, where you parked, a "Leave by 08:04 for Work" time, the road name and the music. Pull it up by its handle for your recent places.
- Hochformat auf der Route: Die Fahrtkarte ist ein Blatt unten mit Restzeit, Ankunft, Entfernung und Beenden. Hochgezogen zeigt sie den Rest der Fahrtkarte. Auf den letzten Metern steht im Blatt, auf welcher Seite das Ziel liegt, und der nächste Parkplatz am Ziel.
  EN: Portrait route: the trip is a sheet along the bottom with time left, arrival, distance and End; pulled up it shows the rest of the trip card. On the last stretch the sheet says which side the destination is on, and the nearest car park by it.
- Das Blatt zeigt „Außerhalb deiner Karten“, wenn du außerhalb aller heruntergeladenen Karten bist.
  EN: The sheet shows "Outside your maps" when you're outside every map you've downloaded.
- Sprachkanal unter Einstellungen › Ton: neu Benachrichtigung, Wecker und Klingelton, für Head-Units, die den Navi-Kanal mit der Musiklautstärke abspielen.
  EN: Voice channel under Settings › Sound: new Notification, Alarm and Ringtone choices, for head units that play the navigation channel at the music volume.

### Improved
- Die Hinweise unter der Abbiegekarte (Durchschnittstempo, nächster Dienst, Parkplatz, Hinweis, Mitteilung) sind kleine Chips in einer Zeile statt Kacheln darunter.
  EN: The hints under the turn card (average speed, next service, car park, advice, notice) are small chips in one line instead of cards stacked under it.
- Routen zu einer Hausadresse enden auf der Straße, an der das Haus liegt, nicht in der Mitte des Grundstücks.
  EN: Routes to a house address end on the street the house is on, not in the middle of the plot.
- Die Vorschau in den Einstellungen zentriert beim Öffnen auf dich und zeigt einen Schritt weiter heraus. Der Zoom der Karte kommt beim Schließen zurück.
  EN: The settings map preview centres on you when it opens and shows a step further out. The map's zoom comes back when it closes.
- Die Was-ist-neu-Karte zeigt nur Änderungen, die du noch nicht gesehen hast.
  EN: The what's-new card only shows changes you haven't seen yet.

### Fixed
- Straßenlampen, die die App entlang von Straßen setzt, stehen nicht mehr auf der Fahrbahn.
  EN: Street lamps the app places along streets no longer stand in the road.
- Auf der Routenleiste ist der Pfeil wieder glatt (Stufen beim Verkleinern) und die Zahlen der Fahrtkarte laufen auf Android Auto nicht mehr ineinander.
  EN: The arrow on the route bar is smooth again (scaled in steps), and the figures on the Android Auto trip card no longer run into each other.

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
