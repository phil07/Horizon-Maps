## Unreleased

### Neu
- Stopps auf dem Autodisplay: Auf der Fahrtkarte die Zahlen antippen, dann siehst du die Stopps auf der Strecke mit ihrer Uhrzeit. Stopps lassen sich früher oder später setzen, entfernen und hinzufügen (hinzufügen per Sprache), danach „Route aktualisieren“. (Dank an yurividal)
  EN: Stops on Android Auto: tap the trip card's figures while navigating for the stops on the way, each with when you get there. Move them earlier or later, take one off or add one (by voice), then Update route. (Thanks to yurividal)

### Verbessert
- Abbiegekarte: kleiner, mit Ecken wie bei Verkehrsschildern. Der Fortschrittsbalken unten reicht jetzt bis ganz ans Ende. (Dank an karthikeyansr-lgtm)
  EN: Turn card: smaller, with road-sign corners, so the progress bar along the bottom reaches the very end. (Thanks to karthikeyansr-lgtm)

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
