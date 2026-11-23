## Unreleased

### New
- Bild im Bild auf Handys: Auf einer Route läuft die Karte beim Wechsel zum Startbildschirm oder einer anderen App in einem kleinen Fenster weiter, mit der nächsten Abbiegung und der Restzeit. Antippen holt die App zurück. Einstellungen › Beim Fahren › Bild im Bild (standardmäßig an; nicht auf Head-Units und nicht mit Android Auto).
  EN: Picture in picture on phones: on a route, going to the home screen or another app keeps the map going in a small window, with the next turn and the time left. Tap it to come back. Settings › While driving › Picture in picture (on by default; not on head units or with Android Auto).
- Die Verkehrsfarben auf der Route und der Routenleiste gehen ineinander über, statt hart zu wechseln. Abschaltbar unter Einstellungen › Auf der Karte › Verkehrsfarben fließend.
  EN: The traffic colours on the route and the route bar run into each other instead of changing in one step. Switch it off under Settings › On the map › Blend traffic colours.
- Stimme über den Handylautsprecher: Einstellungen › Ton & Stimme › Stimme kommt aus › Handylautsprecher. Ansagen und Töne bleiben auf dem Handy, während Musik über Bluetooth läuft (ab Android 9, nicht mit Android Auto).
  EN: Voice through the phone speaker: Settings › Sound & voice › Voice comes out of › Phone speaker. Directions and chimes stay on the phone while music plays over Bluetooth (Android 9 and later, not with Android Auto).
- Neuer Schalter „Stimme wie die Töne abspielen“ unter Ton & Stimme: Für Head-Units, die die Musik bei den Tönen leiser machen, aber nicht bei den Ansagen, spielt die App die Stimme selbst ab. Standardmäßig aus.
  EN: New switch "Play the voice like the chimes" under Sound & voice: for head units that lower the music for the beeps but not for the spoken directions, the app plays the voice itself. Off by default.
- Größen der Abbiegekarte: neue Regler für Pfeilgröße, Breite und Höhe unter Einstellungen › Bildschirm & Größen › Erweiterte Displaygrößen, und unter Android Auto › Erweiterte Autodisplay-Größen.
  EN: Turn card sizes: new sliders for the arrow size, width and height under Settings › Screen & sizes › Advanced screen sizes, and under Android Auto › Advanced car screen sizes.
- Live-Spritkosten: Was die Fahrt bisher gekostet hat, als Zeile unter der Fahrtkarte (Telefon und Android Auto), und die Summe auf der Ankunftskarte. Einstellungen › Kraftstoff & Laden › Live-Spritkosten (standardmäßig aus).
  EN: Live fuel cost: what the trip has cost so far, as a line under the trip card (phone and Android Auto), and the total on the arrival card. Settings › Fuel & charging › Live fuel cost (off by default).
- Android Auto: Suchleiste und Orte können nach 30 Sekunden bis 5 Minuten ohne Berührung verschwinden. Eine Berührung holt sie zurück. Einstellungen › Android Auto › Ausblenden nach (Standard: nie).
  EN: Android Auto: the search bar and places can go after 30 seconds to 5 minutes without a touch; a touch brings them back. Settings › Android Auto › Hide them after (Never by default).
- Android Auto: Cool Running für Handys, die heiß werden. 30 Bilder pro Sekunde, flache Karte, keine Laternen, kein Regen oder Schnee. Aus, Automatisch oder Immer unter Einstellungen › Android Auto (standardmäßig aus).
  EN: Android Auto: Cool running for phones that get hot. 30 frames a second, a flat map, no street lamps and no rain or snow. Off, Automatic or Always under Settings › Android Auto (off by default).

### Fixed
- Suche: Ein weit entfernter Treffer kommt nicht mehr vor einem nahen, der genauso gut passt. Mit dem Ort in der Suche („Hauptstraße, Bensheim“) bleibt die Reihenfolge.
  EN: Search: a result far away no longer comes before a nearer one that matches as well. With the town typed in ("Hauptstraße, Bensheim") the order stays.
- Beim Start gilt eine alte gespeicherte GPS-Position nicht mehr als aktuell. Der Pfeil springt sofort an deinen Standort.
  EN: At start, an old remembered GPS position no longer counts as live. The arrow moves to where you are straight away.
- Das Feedback-Fenster scrollt auf kurzen, breiten Bildschirmen, damit Senden und Kopieren erreichbar sind.
  EN: The feedback window scrolls on short, wide screens, so Send and Copy can be reached.
- Android Auto: Zuhause und Arbeit festlegen speichert den gewählten Ort, statt eine Route dorthin zu starten.
  EN: Android Auto: setting Home and Work saves the place you pick, instead of starting a route to it.
- Die Parkplatz-Zeilen am Ende der Route gehen nicht mehr ständig auf und zu.
  EN: The car park lines near the end of a route no longer open and close over and over.

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
