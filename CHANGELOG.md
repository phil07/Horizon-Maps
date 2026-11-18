## Unreleased

### Neu
- Spritpreise in Italien: Tankstellenliste und Tankvorschläge zeigen Preise aus der täglichen Preisliste des Ministeriums (MIMIT), ohne Schlüssel. Die Liste wird höchstens einmal am Tag geladen und auf dem Gerät gespeichert.
  EN: Fuel prices in Italy: the fuel list and fuel stop suggestions show prices from the government's own daily price list (MIMIT), with no key needed. The list is fetched at most once a day and kept on the device.
- Autozüge vermeiden: Ein neuer Schalter neben „Fähren vermeiden“ hält die Route von Zügen fern, die Autos mitnehmen (Eurotunnel, die Alpen-Tunnelzüge, Sylt-Shuttle). Ist ein Autozug der einzige Weg, wird er trotzdem genommen. Mit TomTom-Schlüssel plant TomTom die Route, ohne Schlüssel kommen die anderen Mapbox-Vorschläge zuerst.
  EN: Avoid car trains: a new switch next to Avoid ferries keeps your route off trains that carry cars (the Channel Tunnel shuttle, the Alpine tunnel trains, the Sylt shuttle). If a car train is the only way there, it is still used. With a TomTom key TomTom plans the route; without one, Mapbox's other suggested routes are tried first.

### Verbessert
- Stopp hinzufügen quer: jetzt eine Leiste an der Seite, die Route bleibt auf der Karte sichtbar. Tanken, Laden, Kaffee, Essen, Raststätten und Toiletten listen diese Orte entlang der Route in derselben Leiste, jeder mit seinem Umweg und auf der Karte, statt einen ungesehen hinzuzufügen. Der Pfeil geht zurück. Die Suche steht oben, frei von der Tastatur. Die Karte zoomt nur heraus, solange Orte gelistet sind.
  EN: Add a stop sideways: now a panel down the side, with your route still on the map. Fuel, Charger, Coffee, Food, Services and Toilets list those places along your route in the same panel, each with the time it adds and shown on the map, rather than adding one you hadn't seen. The arrow goes back. Search sits at the top, clear of the keyboard, and the map only zooms out while places along the route are listed.
- Android Auto: Die Abbiegekarte im Auto hat dieselben Pfeile wie das Handy, auch den Kreisverkehr mit der echten Ausfahrt und Nummer.
  EN: Android Auto: the car's turn card has the same arrows as the phone, including the roundabout with its real exit and number.
- Kreisverkehr: Die anderen Ausfahrten erscheinen blass im Symbol, wenn OpenStreetMap den Kreisverkehr kennt.
  EN: Roundabouts: the other exits show faintly in the icon where OpenStreetMap knows the roundabout.
- Eigenes 3D-Modell: Der Schein ist so groß wie beim eingebauten Auto (bei manchen Modellen bedeckte er fast den ganzen Bildschirm). Scheinwerfer und Straßenname darunter passen zur gewählten Größe.
  EN: Your own 3D model: its glow is the same size round it as round the built-in car (with some models it covered nearly the whole screen), and the headlight beam and the road name under it fit the size you chose.

### Behoben
- TomTom-Karte abgelehnt: Lehnt TomTom den Schlüssel für die Karte ab, erscheint jetzt die Mapbox-Karte mit einem Hinweis. Vorher kam eine falsche Meldung über das Mapbox-Token. Suche und Routen behalten den TomTom-Schlüssel.
  EN: TomTom map refused: when TomTom turns the key down for its map, Mapbox's map now shows with a note saying so (it showed a wrong message about the Mapbox token). Search and routes keep the TomTom key.
- Ankunft: Eine Route, die am Ziel vorbeiführt und zurückkommt, um auf deiner Straßenseite anzukommen, wird jetzt erkannt. Dann wird der direkte Weg genommen, außer er ist langsamer.
  EN: Arriving: a route that went right past your destination and round to come back (to arrive on your side of the road) is now spotted, and the plain way is used unless it's slower.

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
