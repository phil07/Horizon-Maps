## Unreleased

### Behoben
- Hochkant: Die Abbiegekarte läuft beim Abbiegen nicht mehr rechts aus dem Bildschirm, sondern wächst gleichmäßig aus der Mitte. Die Kartenknöpfe sitzen etwas tiefer, zwischen Abbiegekarte und Fahrtkarte, mit oder ohne Einstellungen-Knopf darüber.
  EN: Phone held upright: the turn card no longer goes off the right of the screen as a turn comes up; it grows evenly from the middle. The map buttons sit a little lower, between the turn card and the trip card, with or without the Settings button above them.
- Neuerungen: Ein Update ohne neue Hinweise zeigt keine Karte mehr, die „Keine Versionshinweise gefunden“ sagt. Die Karte erscheint nur, wenn es etwas Neues zu sagen gibt.
  EN: What's new: an update with no new notes no longer shows a card saying "No release notes were found". The card only appears when there is something new to tell you.

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
