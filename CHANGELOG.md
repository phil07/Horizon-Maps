## Unreleased

### Neu
- Android Auto: Suchleiste und Orte auf der Karte ausblenden. Ein neuer Schalter unter Einstellungen › Android Auto (und in den Einstellungen im Auto) lässt Suchleiste, Zuhause und Arbeit, die Schnellwahl und den Fahrtvorschlag weg, wenn du nicht navigierst. Gedacht für Autos mit Drehknopf ohne Touchscreen: Die Karte bekommt den ganzen Bildschirm, und Orte (★) in den Knöpfen von Android Auto hat weiter Suche, Zuhause, Arbeit und deine gespeicherten Orte. Danke an wass0035.
  EN: Android Auto: leave the search bar and places off the map. A new switch in Settings › Android Auto (and in the car's own settings) leaves out the search bar, Home and Work, the shortcuts and the trip suggestion when you're not navigating. For cars with a rotary knob and no touchscreen: the map gets the whole screen, and Places (★) in Android Auto's own buttons still has Search, Home, Work and your saved places. Thanks to wass0035.

### Improved
- Gefahrenradar ohne Zebrastreifen: Wer die Zebrastreifen unter Einstellungen › Auf der Karte ausschaltet, sieht auch keine Fußgängerüberwege mehr im Gefahrenradar. Danke an joel941703.
  EN: Hazard radar without crossings: switching off Zebra crossings in Settings › On the map now takes pedestrian crossings off the hazard radar too. Thanks to joel941703.

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
