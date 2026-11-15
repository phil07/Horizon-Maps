## Unreleased

### Neu
- Zwischenziel erreicht: Wird ein Zwischenziel erreicht, kommt einmal ein Pop-up „Zwischenziel erreicht“ auf dem Handy und auf Android Auto. Die Fahrt pausiert dort: keine Sprachansagen, bis du „Fahrt weitermachen“ drückst. Danach geht es direkt zum Ziel. Die Pause gilt nur für die laufende Fahrt.
  EN: Stop reached: when a stop on the way is reached, a pop-up "Stop reached" comes up once on the phone and on Android Auto. The trip pauses there: no voice until you press "Continue trip". After that it goes straight on to the destination. The pause only lasts for the trip under way.

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
