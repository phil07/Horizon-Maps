## Unreleased

### New
- Blitzer: Kurz bevor du an einem Blitzer vorbeifährst, gibt es einen zweiten, kurzen Piepton, etwa 4 Sekunden davor. Die Warnung bei etwa 500 m bleibt.
  EN: Speed cameras: a short second beep sounds about 4 seconds before a camera, after the warning about 500 m ahead.

### Fixed
- Der Stern auf der Karte eines Favoriten: Ein Ort, der etwas neben seiner Stelle auf der Karte gespeichert ist (oft bei importierten Orten), zeigte einen leeren Stern, und ein Tipp darauf entfernte den Favoriten. Die Karte findet ihn jetzt wie der Stern selbst.
  EN: A favourite's star on its card: a place saved a little way from where it shows on the map showed an empty star, and tapping it removed the favourite. The card now finds it the same way the star does.

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
