## Unreleased

### Neu
- Schnellere Route auf Android Auto: Wird eine schnellere Route gefunden, kommt ein Pop-up mit Wechseln und Beibehalten. Es zeigt die gesparte Zeit und die Kilometer-Differenz, z. B. „Schnellere Route: spart 4 Min. · 2,3 km kürzer“.
  EN: Faster route on Android Auto: when a faster route is found, a pop-up offers Switch and Keep. It shows the time saved and the distance difference, e.g. "Faster route: saves 4 min · 2.3 km shorter".
- Knöpfe unten links auf Android Auto: Hineinzoomen, Herauszoomen, Einstellungen und Zentrieren auf deinen Standort. Sie sind so groß und haben dieselben Icons wie die Knöpfe oben links.
  EN: Buttons bottom left on Android Auto: zoom in, zoom out, settings and re-centre on your position. They are the same size and use the same icons as the buttons top left.

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
