## Unreleased

### Improved
- Android Auto während der Fahrt: Der Beenden-Knopf (✕) steht jetzt in der linken Knopfspalte über Melden, in einer Linie mit den anderen Knöpfen. Oben in der Android-Auto-Leiste sitzt stattdessen der Ton-Knopf.
  EN: Android Auto while driving: the end button (✕) now sits in the left button column above Report, in line with the other buttons. Android Auto's bar at the top shows the sound button instead.

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
