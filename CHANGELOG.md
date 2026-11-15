## Unreleased

### Neu
- Kompakter Tacho auf Android Auto: Unter Einstellungen › Android Auto › Tachostil auf dem Autodisplay gibt es „Kompakt“: eine kleine dunkle Pille mit Limit-Schild und Tempo nebeneinander. Sie passt auch oben rechts auf einen kurzen Bildschirm. Standard bleibt „Normal“.
  EN: Compact speedometer on Android Auto: under Settings › Android Auto › Speedometer style on the car screen, "Compact" shows a small dark pill with the limit sign and your speed side by side. It also fits top right on a short screen. "Normal" stays the default.

### Verbessert
- Android Auto übernimmt Einstellungen sofort: Kartendetails, Pfeil- bzw. Autogröße und -art, Straßennamen-Größe, 3D-Extras (Fahnen, Bäume, Lampen), Satellit/3D und das Licht ändern sich auf dem Autodisplay, sobald du sie änderst, ohne neu zu verbinden.
  EN: Android Auto follows your settings straight away: map detail, arrow or car kind and size, road name size, the 3D extras (flags, trees, lamps), satellite/3D and the light now change on the car screen as you change them, without reconnecting.
- Tachoposition auf Android Auto: Unten links (Standard), Unter der Abbiege-Karte oder Oben rechts. Unter der Abbiege-Karte fällt auf Unten links zurück, solange keine Abbiege-Karte zu sehen ist.
  EN: Speed position on Android Auto: Bottom left (default), Under the turn card or Top right. "Under the turn card" falls back to bottom left while no turn card is showing.
- Android Auto im geteilten Bildschirm: Während der Navigation ist die Fahrtkarte ein Balken unten in der Mitte der Kachel.
  EN: Android Auto split screen: while navigating, the trip card is a bar along the bottom edge of the tile, centred.

### Behoben
- Was ist neu: Versionsüberschriften wie „### 1.0.567“ erscheinen nicht mehr als Text zwischen den Hinweisen.
  EN: What's new: version headings such as "### 1.0.567" no longer show as stray text between the notes.
- Handy, Route wählen: Mit „Route überfliegen“ und „Demo“ stehen diese Knöpfe in einer eigenen Reihe, sodass „Navigation starten“ wieder die volle Breite hat.
  EN: Phone, route choice: with "Fly over route" and "Demo" on, they share a row of their own, so "Start navigation" has the full width again.
- Scheinwerferlicht: Der Lichtkegel beginnt vor dem Pfeil statt darunter.
  EN: Headlight beam: the beam starts in front of your arrow instead of underneath it.
- Tachoposition auf Android Auto hatte bei einem kurzen Bildschirm oder mit der Fahrtkarte rechts keine Wirkung; jetzt wird die gewählte Position benutzt.
  EN: Speed position on Android Auto had no effect on a short screen or with the trip card on the right; the chosen position is now used.

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
