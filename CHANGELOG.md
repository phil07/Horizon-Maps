## Unreleased

### Fixed
- Schwebende Fenster: In einem freien oder geteilten Fenster (Taskbar und ähnliche) blendet die App die Leisten nicht mehr aus. Das hatte auch die Titelleiste des Fensters versteckt, sodass es sich nicht verschieben, in der Größe ändern oder auf Vollbild schalten ließ. Hat die App den ganzen Bildschirm, bleibt sie im Vollbild. Danke an Grouchy_Promotion.
  EN: Floating windows: in a freeform or split-screen window (Taskbar and similar), the app no longer hides the bars, which also hid the window's own title bar, so the window couldn't be dragged, resized or made full screen. Full screen still applies when the app has the whole screen. Thanks to Grouchy_Promotion.

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
