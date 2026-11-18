## Unreleased

### Behoben
- Stopp hinzufügen hochkant: Die Liste der Orte entlang der Route steht unten, die Orte sind oben auf der Karte zu sehen. Vorher hat die Liste sie verdeckt. Die Liste hat dieselbe feste Fläche wie die anderen Karten, sodass die Karte nicht durch den Text scheint.
  EN: Add a stop on a phone held upright: the list of places along the route now sits along the bottom, with the places shown on the map above it (the list covered them). The list has the same solid panel as the other cards, so the map doesn't show through the text.

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
