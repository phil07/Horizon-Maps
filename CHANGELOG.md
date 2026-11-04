# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert
- Elektro-Routenvorschau: Die Ladeplanung aller Routen läuft gleichzeitig statt nacheinander und wird für die gewählte Route nicht noch einmal berechnet. Das verkürzt die Wartezeit deutlich.

## Unreleased

### Verbessert
- Elektro-Routenvorschau: Zeit und Ankunft aller Routen erscheinen erst, wenn die Ladeplanung fertig ist, und enthalten dann gleich die Ladepausen. Bis dahin steht „…“ und der Start-Countdown wartet.
- Elektro-Routenvorschau: Die geplanten Ladestopps der gewählten Route werden schon in der Vorschau auf der Karte gezeigt.
- Elektro-Routenvorschau: Die Ladeplanung aller Routen läuft gleichzeitig statt nacheinander und wird für die gewählte Route nicht noch einmal berechnet. Das verkürzt die Wartezeit deutlich.
- Der Pfeil im Fortschrittsbalken sitzt am Anfang der Route ganz links (und am Ziel ganz rechts).


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
