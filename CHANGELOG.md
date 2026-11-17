## Unreleased

### Neu
- Stopp hinzufügen von der Fahrtkarte: Neben „Route beenden“ gibt es einen Plus-Knopf. Er öffnet deine Stopps, wo du unterwegs einen Stopp hinzufügst. (Dank an karthikeyansr-lgtm)
  EN: Add a stop from the trip card: a plus button next to "End route" opens your stops, where you can add one on the way. (Thanks to karthikeyansr-lgtm)

### Verbessert
- Abbiegekarte: Der Fortschrittsbalken sitzt ein Stück innen, sodass seine runden Ecken die letzten Sekunden nicht mehr verdecken. Im Kreisverkehr steht die Ausfahrtnummer groß im größeren Ring, auf der Abbiegekarte und in der „Dann“-Zeile. Die „Dann“-Zeile nennt dann nur noch die Straße.
  EN: Turn card: the progress bar sits a little way in, so its rounded corners no longer hide the last seconds. At a roundabout, the exit number is written large inside a bigger ring, on the turn card and in the "then" line. The "then" line now only names the road. (Thanks to karthikeyansr-lgtm)

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
