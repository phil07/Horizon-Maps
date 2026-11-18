## Unreleased

### Verbessert
- Scheinwerferkegel: Etwa halb so breit, damit er die Straße vor dir ausleuchtet und nicht die Gebäude daneben. (Dank an liamstears)
  EN: Headlight beam: about half as wide, so it lights the road ahead rather than the buildings either side. (Thanks to liamstears)

### Behoben
- Scheinwerfer mit eigenem 3D-Modell: Kein dunkler Fleck mehr um das Auto. Der Kegel beginnt an der Front deines Modells, statt an einem autoförmigen Ausschnitt, der nicht zur Form deines Modells passte. (Dank an liamstears)
  EN: Headlight beam with your own 3D model: no more odd dark patch round the car. The beam now starts at the front of your model instead of a car-shaped box that didn't match your model's shape. (Thanks to liamstears)

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
