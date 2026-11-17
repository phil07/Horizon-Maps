## Unreleased

### Verbessert
- Scheinwerfer unter dem 3D-Auto: Mit dem 3D-Auto oder dem flachen Pfeil liegt der Lichtkegel jetzt auf der Straße unter dem Auto, statt über dem Dach gezeichnet zu werden. (Dank an liamstears)
  EN: Headlight under the 3D car: with the 3D car or the flat arrow, the beam now lies on the road under it instead of being drawn over its roof. (Thanks to liamstears)
- Straßenname unten bleibt: Fährst du ohne Route, wechselt der Straßenname unten nicht mehr bei jedem Abbremsen mit der Blase unter dem Pfeil. Er bleibt eine Minute, nachdem du anhältst. (Dank an liamstears)
  EN: Road name at the bottom stays put: driving without a route, the road name at the bottom no longer swaps with the bubble under your arrow every time you slow down. It stays for a minute after you stop. (Thanks to liamstears)
- Verkehrshinweis beim Halt: Der Hinweis lautet jetzt „Verkehr wird angezeigt, während du stehst“ (englisch: „Showing traffic while you’re stopped“). (Dank an liamstears)
  EN: Clearer traffic note when stopped: the note now reads "Showing traffic while you’re stopped". (Thanks to liamstears)

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
