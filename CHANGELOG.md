## Unreleased

### Neu
- Zuhause und Arbeit lassen sich einzeln unter der Suche ausblenden: Einstellungen › Orte & Fahrten › Zuhause und Arbeit unter der Suche. Gilt für die App und für Android Auto.
  EN: Home and Work can each be hidden under the search bar: Settings › Places & trips › Home and Work under the search bar. Applies to the app and Android Auto.

### Behoben
- Position des Tachos auf dem Autodisplay: Die Auswahl lief am Rand ab, sodass „Oben rechts“ nur durch Wischen zu sehen war. Die Auswahl bricht jetzt in eine zweite Zeile um.
  EN: Speed position on the car screen: the choices ran off the side, so "Top right" could only be reached by swiping. They now wrap onto a second line.

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
