## Unreleased

### Verbessert
- Hochkant auf einer Route: Die Abbiegekarte nimmt die volle Breite, und die Kartenknöpfe sitzen darunter, statt neben ihr gequetscht zu sein.
  EN: Phone held upright, on a route: the turn card takes the full width and the map buttons sit underneath it, instead of being squeezed in beside it.
- Quer auf dem Handy: Auf kurzen Bildschirmen ist das große Spurbild niedriger. Die Abbiegekarte reicht nicht mehr hinter die Fahrtkarte und verdeckt die „Dann“-Zeile nicht mehr. (Dank an fahadfadi)
  EN: Phone held sideways: the big lane picture is lower on short screens, so the turn card no longer reaches down behind the trip card and hides the "Then" line. (Thanks to fahadfadi)

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
