## Unreleased

### Improved
- Feedback: Ist eine neuere Version da, sagt der Bericht-Screen das und bietet „Jetzt aktualisieren“ an, mit dem Hinweis, vorher zu prüfen, ob das Problem noch da ist. „Trotzdem senden“ bleibt. Auf Google Play erinnert er daran, nach einer neueren Version zu suchen.
  EN: Feedback: if a newer version is out, the report screen says so and offers "Update now", with a reminder to check the problem is still there first. "Send anyway" stays. On Google Play it reminds you to check for a newer version.

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
