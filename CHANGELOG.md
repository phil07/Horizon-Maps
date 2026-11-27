## Unreleased

### Improved
- Zebrastreifen ohne Route: In Orten werden die Übergänge rund um dich auch ohne Route auf die Straße gezeichnet (mit Zebra-Leuchten), unter etwa 40 mph und etwa alle 500 m abgefragt.
  EN: Zebra crossings without a route: in towns the crossings round you are painted on the road too (with Belisha beacons), looked up under about 40 mph and about every 500 m.
- Gespeicherte Orte auf der Karte: Die Symbole sind doppelt so groß (Einstellung bis 300 %).
  EN: Saved places on the map: their icons are twice the size (setting up to 300 %).
- „Hier parken“ wird nur angeboten, wenn du gefahren bist und stehst, und nicht auf dem Handy, solange Android Auto verbunden ist.
  EN: "Park here" is only offered once you've driven and stopped, and not on the phone while Android Auto is connected.
- Auto-Display: Tempolimit-Schilder neben dem Tacho und das kommende Limit folgen der Einstellung „Limit-Schild-Größe“.
  EN: Car display: the speed limit signs beside the speed and the upcoming limit follow the "Speed-limit sign size" setting.
- Verkehr ohne Route: Die Verkehrslinien sind breiter und deckend, damit langsamer und stehender Verkehr auffällt.
  EN: Traffic when driving without a route: the lines are wider and solid, so slow and stopped traffic stands out.
- TomTom-Karte mit abgelehntem Schlüssel: Die Meldung erklärt, dass die Karte ein eigenes TomTom-Produkt ist, das für den Schlüssel eingeschaltet sein muss; Routen und Suche können trotzdem gehen.
  EN: TomTom map with a key it turns down: the message says the map is a separate TomTom product to switch on for your key; routes and search can still work.

### Fixed
- Android Auto: Tempolimit-Wechsel, Tempowarnung und Blitzer werden nicht mehr angesagt, solange eine andere App (z. B. Waze) den Autobildschirm hat und keine Horizon-Route läuft (mit „Tempowarnung im Hintergrund“ weiterhin).
  EN: Android Auto: speed limit changes, speeding and cameras are no longer called out while another app such as Waze has the car screen and no Horizon route is running (still with "Speed alert in the background").

### Added
- Autobahnen vermeiden: Nie, Immer oder nur auf kurzen Fahrten (Länge einstellbar, 15 bis 80 km oder 10 bis 50 Meilen). Offline-Routen meiden Autobahnen jetzt auch.
  EN: Avoid motorways: Never, Always or Short trips only (the length is set, 15 to 80 km or 10 to 50 miles). Offline routes now keep off motorways too.
- Zoom an Kreuzungen (Einstellungen › Spuren und Kreuzungen): Aus, Weiter, Näher oder Am nächsten.
  EN: Junction zoom (Settings › Lanes and junctions): Off, Further, Closer or Closest.
- Gefahrenschilder auf der Karte (Baustellen, Sperrungen, Gefahren) haben eine eigene Größeneinstellung und sind etwas größer.
  EN: Roadworks, closure and hazard signs on the map have their own size setting and are a little bigger.

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
