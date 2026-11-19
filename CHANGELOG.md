## Unreleased

### Verbessert
- Batterie um 1 % statt in Zehnerschritten: Das − und das + an der Batterie auf der Routenkarte gehen 1 % auf und ab und laufen weiter, solange man sie hält. (Dank an Dry-Secretary2456)
  EN: Battery by 1 %: the − and + by the battery on the route screen go up and down 1 % at a time, and keep going while held, instead of jumping in tens. (thanks to Dry-Secretary2456)

### Behoben
- Fährt noch nach dem Parken: Wird das Auto innerhalb von 400 m vor dem Ziel abgestellt, endet die Fahrt jetzt, auf Android Auto und auf dem Telefon. Startet man die App direkt am Ziel neu, wird die Fahrt nicht mehr wieder aufgenommen. (Dank an pc4096)
  EN: Still navigating after parking: switching the car off within 400 m of the destination now ends the trip, on Android Auto and on the phone. Starting the app again right by the destination no longer picks the trip back up. (thanks to pc4096)
- Musik oder Radio stoppt nach Ansagen: Kam eine zweite Ansage, während die erste noch lief, wurde die Musik nicht wieder laut gestellt und blieb auf manchen Autoradios stumm. Die Musik wird jetzt immer zurückgegeben, sobald die Stimme fertig ist. (Dank an adernev)
  EN: Music or radio stopping after directions: when a second direction came while the first was still being spoken, the music was not always handed back, and on some head units it stayed quiet. The music is now always handed back once the voice has finished. (thanks to adernev)

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
