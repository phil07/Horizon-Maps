## Unreleased

### Neu
- Vorschlagskarte: Wohin du wahrscheinlich fährst, etwa Arbeit am Morgen, mit der Fahrzeit und einem Knopf Los. Sie steht unter der Suchleiste, auf dem Telefon und auf Android Auto, und verschwindet, sobald eine Route läuft.
  EN: Suggested destination card: where you're probably heading, such as work in the morning, with its drive time and a Go button. It shows under the search bar, on the phone and on Android Auto, and goes once a route runs.

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
