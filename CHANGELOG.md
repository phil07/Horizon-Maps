## Unreleased

### Behoben
- Verknüpfungen in den Einstellungen auf dem Handy: Die Knöpfe brechen jetzt in so viele Zeilen um, wie sie brauchen, statt dass der letzte zu einer Spalte aus einzelnen Buchstaben gequetscht wird.
  EN: Shortcuts in Settings on a phone: the buttons now wrap onto as many lines as they need, instead of the last one being squashed into a column of single letters

### Neu
- Knöpfe unter der Suchleiste sortieren: Einstellungen › Orte & Fahrten › Verknüpfungen unter der Suchleiste hat jetzt eine Liste „Deine Reihenfolge“ mit Pfeilen nach oben und unten, um jeden Knopf zu verschieben.
  EN: Reorder your shortcuts: Settings › Places & trips › Shortcuts under the search bar now has a "Your order" list with up and down arrows to move each one

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
