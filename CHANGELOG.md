## Unreleased

### Neu
- Straßennamen in fremder Schrift werden angesagt: Hebräisch, Arabisch, Kyrillisch, Griechisch, Thai, Chinesisch, Japanisch und Koreanisch werden mit der Stimme für diese Schrift gesprochen, wenn das Telefon sie hat. (Dank an Dotancom)
  EN: Street names in other alphabets are spoken: Hebrew, Arabic, Cyrillic, Greek, Thai, Chinese, Japanese and Korean are read in the voice for that script, if the phone has it. (thanks to Dotancom)

### Verbessert
- Klarere Karte: Ziehst du die Karte oder zoomst du ohne Route, verschwinden die Suchleiste und ihre Knöpfe. Ein Tipp auf den runden Suchknopf oder auf Zentrieren holt sie zurück. (Dank an TouristCreepy1884)
  EN: Clearer map: when you drag or zoom the map with no route, the search bar and everything under it tuck away. Tap the round search button or re-centre to bring them back. (thanks to TouristCreepy1884)
- Straßenlaternen auf Android Auto folgen jetzt der Einstellung Straßenlaternen, wie Bäume und die 3D-Details. (Dank an ChrisSantosGarage)
  EN: Street lamps on Android Auto now follow the Street lamps setting, like the trees and the 3D details. (thanks to ChrisSantosGarage)

### Behoben
- Android Auto folgt dir wieder: Ohne Route und mit gesperrtem Telefon hielt Android die Standortangaben zurück, der Pfeil blieb stehen. Der GPS-Empfang läuft jetzt, solange der Autobildschirm verbunden ist. (Dank an timblr1989)
  EN: Android Auto not following you: with no route and the phone locked, Android held back location updates and the arrow stood still. GPS now keeps running while the car screen is connected. (thanks to timblr1989)

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
