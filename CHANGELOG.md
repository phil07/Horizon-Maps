## Unreleased

### New
- Sprachkanal unter Einstellungen › Ton: neu Benachrichtigung, Wecker und Klingelton, für Head-Units, die den Navi-Kanal mit der Musiklautstärke abspielen.
  EN: Voice channel under Settings › Sound: new Notification, Alarm and Ringtone choices, for head units that play the navigation channel at the music volume.

### Improved
- Routen zu einer Hausadresse enden auf der Straße, an der das Haus liegt, nicht in der Mitte des Grundstücks. Mapbox' Punkt auf der Straße wird genutzt; fehlt er, der nächste Punkt auf der benannten Straße.
  EN: Routes to a house address end on the street the house is on, not in the middle of the plot. Mapbox's point on the street is used; if it gives none, the nearest point on the named street.
- Die Vorschau in den Einstellungen zentriert beim Öffnen auf dich und zeigt einen Schritt weiter heraus, damit mehr von deiner Umgebung sichtbar ist. Der Zoom der Karte kommt beim Schließen zurück.
  EN: The settings map preview centres on you when it opens and shows a step further out, so more of your area is visible. The map's zoom comes back when it closes.
- Die Was-ist-neu-Karte zeigt nur Änderungen, die du noch nicht gesehen hast. Jede Änderung wird beim Schließen der Karte gemerkt.
  EN: The what's-new card only shows changes you haven't seen yet. Each change is remembered when the card is closed.

### Fixed
- Straßenlampen, die die App entlang von Straßen setzt, stehen nicht mehr auf der Fahrbahn: Eine Lampe wird nur gesetzt, wenn sie mindestens 4 m von jeder Straßenmitte entfernt ist.
  EN: Street lamps the app places along streets no longer stand in the road: a lamp is only placed at least 4 m away from every street's middle line.

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
