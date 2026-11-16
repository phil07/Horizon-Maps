## Unreleased

### Verbessert
- Schnelleres Umrouten nach einer verpassten Abbiegung: Die neue Route wird angefragt, sobald du von der alten Route abzweigst, nicht erst, wenn es sicher ist. Früher dauerte es 6 bis 8 Sekunden.
  EN: Faster rerouting after a missed turn: the new route is asked for as soon as you start to leave the old one, not once it is certain. It used to take 6 to 8 seconds.

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
