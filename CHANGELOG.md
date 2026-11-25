## Unreleased

### New
- „Dein Auto“ auf dem Handy: Unter der Suche steht die Karte „Dein Auto“. Ist kein Auto gespeichert, gibt es den Knopf „Hier parken“, der deinen Standort speichert. Ist eins gespeichert, bringt „Zu Fuß“ dich hin und die Entfernung steht dabei.
  EN: "Your car" on the phone: under the search bar there is a "Your car" card. When no car is saved, a Park here button saves where you are now. When one is saved, Walk takes you back to it, with the distance.
- Schnellmeldung auf Android Auto: Ein Megafon auf der Fahrtkarte öffnet ein Panel über der Karte mit Polizei, Unfall, Gefahr, Panne, mobilem Blitzer und Stau. Ein Tipp meldet, wo du bist, das Panel schließt sich nach 8 Sekunden. Ausschaltbar unter Einstellungen › Android Auto › Knöpfe auf der Fahrtkarte.
  EN: Quick reports on Android Auto: a megaphone on the trip card opens a panel over the map with Police, Crash, Hazard, Broken down, Mobile camera and Traffic jam. One tap reports where you are; it closes by itself after 8 seconds. It can be switched off under Settings › Android Auto › Buttons on the trip card.
- Sprachverstärkung: Einstellungen › Ton & Stimme › Stimme und Lautstärke › Sprachverstärkung macht die Ansagen bis 18 dB lauter, wenn sie neben der Musik zu leise sind. Ein Wert über 0 schaltet „Stimme wie die Töne abspielen“ ein. Standard: aus (0 dB).
  EN: Voice boost: Settings › Sound & voice › Voice and volume › Voice boost makes the spoken directions up to 18 dB louder, for when they are too quiet next to the music. A value above 0 turns on "Play the voice like the chimes". Default: off (0 dB).

### Changed
- Der Parkplatz-Chip im Hochformat-Blatt ist in die Karte „Dein Auto“ gewandert.
  EN: The parked-car chip on the upright sheet is now the "Your car" card.

### Fixed
- „Stau sagen“: „Melde Verkehr“ oder „report traffic“ meldet jetzt einen Stau, statt eine allgemeine Gefahr.
  EN: Saying "report traffic" now reports a traffic jam, not a general hazard.

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
