## Unreleased

### Verbessert
- Straßennummern als Schilder auf der Abbiegekarte: Autobahnen blau mit Weiß, A-Straßen grün mit Gelb und B-Straßen weiß mit Schwarz, wie auf den echten Schildern. Die Ecken der Abbiegekarte sind etwas weniger rund. (Dank an karthikeyansr)
  EN: UK road numbers as signs on the turn card: motorways in blue with white, A roads in green with yellow and B roads in white with black, as on the real signs. The turn card's corners are also a little less rounded. (Thanks to karthikeyansr)

### Behoben
- Eigene 3D-Modelle, die unsichtbar waren: Ein Modell, das sich selbst verkleinert (häufig bei Modellen von Sketchfab und anderen Seiten), erschien viel zu klein. Modelle werden jetzt so gemessen, wie die Karte sie zeichnet. Bereits gewählte werden einmal neu gemessen. (Dank an Matattack99)
  EN: Own 3D models that were invisible: a model whose file scales itself down, common in models from Sketchfab and other sites, came out far too small to see. Models are now measured the way the map draws them, and ones picked before are measured again once. (Thanks to Matattack99)
- Sagt, warum Satellit oder dein eigenes Modell nicht zu sehen sind: Wenn Mapbox für dein Token keine Satellitenbilder liefert oder das 3D-Modell nicht lädt, erscheint eine Meldung mit dem, was du probieren kannst. Der Fehlerbericht nennt den Grund von Mapbox. (Dank an Matattack99)
  EN: Says why satellite or your own model didn't show: when Mapbox turns down satellite pictures for your token, or can't load your 3D model, a message now says so and what to try. The feedback report includes Mapbox's reason. (Thanks to Matattack99)

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
