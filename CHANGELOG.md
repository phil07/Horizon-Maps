## Unreleased

### Behoben
- Immer Tag auf Android Auto: Die Einstellung „Immer Tag“ am Auto gewinnt jetzt. Stand das Handy auf Nacht, blieb die Karte am Auto dunkel.
  EN: Always day on Android Auto: the car screen's own "Always day" now wins. With the phone set to Night, picking "Always day" on the car still left the car's map dark.
- Abschnittskontrollen als normale Blitzer: Die Kameras am Anfang und am Ende einer Abschnittskontrolle werden jetzt als Abschnittskontrolle angesagt, statt als „Blitzer voraus“, das sie verdeckt hat. Die Kontrolle wird so früh gemeldet wie ein Blitzer. Schlägt die Suche entlang der Route auf einem ausgelasteten Server fehl, werden die Abschnittskontrollen einzeln abgefragt, damit sie nicht fehlen.
  EN: Average speed checks shown as plain speed cameras: the cameras at the start and end of a check are now announced as the average speed check, instead of a "Speed camera" alert that hid it, and the check is warned about as early as a camera would be. If the search along the route fails on a busy server, the checks are now looked up on their own so they aren't missed.

### Neu
- Landmarken in Anweisungen: Biegt die Route an einer Ampel ab, zeigt die Abbiegekarte „An der Ampel“ (darunter Entfernung und Straße), und die Stimme sagt „An der Ampel, rechts abbiegen auf …“, oder „An der zweiten Ampel“, wenn davor eine Ampel kommt. Bei mehr Ampeln bleibt es bei der Entfernung. Abschaltbar unter Einstellungen › Ton & Stimme › Landmarken in Anweisungen.
  EN: Landmarks in directions: when a turn is at traffic lights, the turn card says "At the lights" (with the distance and road below) and the voice says "At the traffic lights, turn right onto…", or "At the second set of lights" when there's one set before it. With more lights on the way it says the distance as before. Turn it off in Settings › Sound & voice › Landmarks in directions.
- Eigenes 3D-Modell: Einstellungen › Pfeil › Echtes 3D-Modell › Dein Modell lädt eine eigene .glb-Datei (bis 8 MB) als Pfeil. Sie wird passend zum eingebauten Auto skaliert, egal in welcher Größe sie gebaut wurde, mit Größe und Drehen, damit sie in Fahrtrichtung zeigt. Kann sie nicht angezeigt werden, steht das eingebaute Auto da. Auch auf Android Auto.
  EN: Your own 3D model: Settings › Arrow › Real 3D model › Your model loads a .glb file of your own (up to 8 MB) as the arrow, sized to match the built-in car whatever scale it was made at, with Turn to point it the way you drive and the size slider to fine-tune it. If it can't be shown, the built-in car stands in. Works on Android Auto too.

### Verbessert
- Fahrtkarte: Stimme, Ton und Stopps sind von der Fahrtkarte weg, übrig bleibt Beenden (und die Ladesäulen im Elektroauto). Der Ton ist auf Android Auto ein großer Knopf oben links, weil der Fahrer dort links sitzt. Stimme und Stopps stehen unter Kartenoptionen › Diese Fahrt.
  EN: Trip card: the voice, sound and stops buttons are gone from the trip card, leaving just End (and chargers in an electric car). On Android Auto, sound is a big button at the top left, where the driver sits. Voice and Stops are in Map options › This trip.

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
