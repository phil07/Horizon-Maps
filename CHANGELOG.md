## Unreleased

### Behoben
- Scheinwerferkegel: Er flackert nicht mehr, verschwindet nicht mehr kurz und bremst die Karte nicht mehr. Er ist jetzt ein einzelnes Bild, das nur nachgeführt wird, wenn sich der Pfeil wirklich bewegt oder dreht (höchstens etwa 10 Mal pro Sekunde), statt jedes Mal neu gezeichnet zu werden. Bei kurzem Tempoverlust bleibt er stehen.
  EN: Headlight beam: no longer flickers or vanishes for a moment, and no longer slows the map. It is now a single picture that is only moved when the arrow really moves or turns (at most about 10 times a second), instead of being redrawn every time. It stays through a short dip in speed.
- Blitzer entlang der Route: Die Blitzer einer geplanten Route erscheinen jetzt auf der ganzen Strecke, während du die Route wählst oder eine Demo startest, nicht nur im Umkreis von 10 km um dich. Ein Straßenstück, dessen Blitzer nicht geladen werden konnten (ein ausgelasteter Server), wird jetzt erneut versucht, statt für den Rest der Fahrt übersprungen zu werden.
  EN: Speed cameras along the route: the cameras on a planned route now show along all of it while you choose the route or start a demo, not only within 10 km of where you are. A stretch of road whose cameras couldn't be looked up (a busy server) is now tried again, instead of being skipped for the rest of the trip.
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
