## Unreleased

### New
- Karten-Knöpfe: Einstellungen › Karten-Knöpfe wählt, welche Knöpfe am rechten Kartenrand erscheinen: Kompass, Klare Ansicht, Ton, Andere Routen und Übersicht. Neu ist der Übersicht-Knopf, standardmäßig aus: Er zeigt die ganze Route ein paar Sekunden, dann folgt die Karte wieder dir.
  EN: Map buttons: Settings › Map buttons chooses which buttons show on the right edge of the map: compass, clean view, sound, other routes and overview. The new Overview button is off by default: it shows the whole route for a few seconds, then the map follows you again.
- Klare Ansicht auf dem Handy: Ein Auge-Knopf auf der Kartenleiste zeigt beim Navigieren nacheinander alles, eine minimale Ansicht (Abbiegekarte, Tempo und Fahrtkarte) und nur die Karte. Die Wahl bleibt gespeichert.
  EN: Clean view on the phone: an eye button on the map bar while you navigate goes through everything, a minimal view (turn card, speed and trip card) and just the map. The choice is kept.
- Ladeleistung und Nur diese Anbieter: Einstellungen › Dein Elektroauto. Die Ladeleistung filtert auf 22, 50 oder 150 kW und mehr (oder beliebig). „Nur diese Anbieter“ zeigt nur Ladesäulen der gewählten Anbieter, z. B. Ionity oder Tesla.
  EN: Charger speed and Only these providers: Settings › Your electric car. Charger speed filters to 22, 50 or 150 kW and more (or any). Only these providers shows only the chargers of the networks you pick, e.g. Ionity or Tesla.
- Ladeumweg: Einstellungen › Dein Elektroauto › Ladeumweg (5, 10 oder 15 Minuten). Standard: 10 Minuten.
  EN: Charger detour: Settings › Your electric car › Charger detour (5, 10 or 15 minutes). Default: 10 minutes.
- Leistenhöhe auf Android Auto: Einstellungen › Größen auf dem Autodisplay › Leistenhöhe macht die Suchleiste schlanker.
  EN: Bar height on Android Auto: Settings › Sizes on the car screen › Bar height makes the search bar slimmer.
- Bildschirm auch ohne Route anlassen: Einstellungen › Bildschirm. Aus, weil es mehr Akku braucht.
  EN: Keep the screen on without a route: Settings › Screen. Off, as it uses more battery.
- Farbe der Abbiegekarte: Grün, Schwarz, Dunkelgrau, Navy oder Lila, mit weißer Schrift.
  EN: Turn card colour: green, black, dark grey, navy or purple, with white writing.

### Improved
- Die Ladeliste folgt dem Filter: Handy, Autodisplay und Android Auto zeigen beim Laden und bei Ladesäulen an der Route nur Säulen mit der gewählten Leistung und den gewählten Anbietern. Säulen mit unbekannter Leistung bleiben sichtbar.
  EN: The chargers list follows the filter: the phone, the car screen and Android Auto show only chargers of the chosen power and networks. Chargers of unknown power stay visible.
- Kompass: Die rote Nordmarke wandert am Rand mit der Karte und zeigt immer nach echtem Norden.
  EN: Compass: the red north mark moves round the edge with the map and always points to real north.
- Suche: Der Ausklapp-Pfeil auf der Suchleiste zeigt sich nur noch im Querformat.
  EN: Search: the fold-down arrow on the search bar now only shows in landscape.

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
