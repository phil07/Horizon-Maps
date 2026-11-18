## Unreleased

### Neu
- Abbiegepfeile: fetter, mit einer weichen Kurve und einer eingekerbten Spitze. Die Straße, die du verlässt, ist dahinter blass zu sehen. Im Kreisverkehr sind die anderen Ausfahrten blass zu sehen, die Ausfahrt-Nummer steht in der Mitte auf einer weichen Scheibe. Die großen Spurpfeile bleiben gleich.
  EN: Turn arrows: bolder, with a smooth curve and a notched head, and the road you're leaving shown faintly behind them. Roundabouts show the other exits faintly too, with the exit number in the middle on a soft disc. The big lane arrows are unchanged.

### Verbessert
- Die Ecken der Abbiegekarte sind wieder etwas runder.
  EN: The turn card's corners are slightly rounder again.

### Behoben
- Zahlen nicht mittig: Die Ausfahrt-Nummer im Kreisverkehr und die Zahl auf dem runden Tempolimit-Schild saßen etwas zu hoch. Beide stehen jetzt genau in der Mitte. Dank an karthikeyansr-lgtm.
  EN: Numbers off-centre: the exit number in the roundabout icon and the number on the round speed limit sign sat slightly high; both are now exactly in the middle. Thanks to karthikeyansr-lgtm.

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
