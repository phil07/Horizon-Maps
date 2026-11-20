## Unreleased

### New
- Helligkeit der Karte in den Kartenoptionen: ein Regler unter Tag und Nacht, für den Modus, der gerade gezeigt wird. Die Nachthelligkeit ist die aus den Einstellungen (Karte heller/dunkler), die Tageshelligkeit ist neu.
  EN: Map brightness in Map options: a slider under Day and Night, for whichever is showing. The night brightness is the one in Settings (Karte heller/dunkler), the day brightness is new.

### Improved
- Fortschrittsbalken der Abbiegekarte: Er erscheint jetzt erst etwa 800 m vor der Abbiegung und füllt sich bis dahin, statt kilometerweit leer zu stehen.
  EN: Turn card progress bar: it now appears about 800 m before the turn and fills up to it, instead of sitting empty for kilometres.

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
