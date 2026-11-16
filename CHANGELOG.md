## Unreleased

### Behoben
- 3D-Gebäude: Wer 3D-Gebäude ausschaltet, sieht jetzt auch Gebäudefassaden und 3D-Sehenswürdigkeiten nicht mehr. Auf Android Auto folgen Gebäude und Bäume den Schaltern in den Einstellungen. (Dank an VITMAD1964)
  EN: 3D buildings: turning 3D buildings off now also takes away building fronts and 3D landmarks. On Android Auto, buildings and trees follow their switches in Settings. (Thanks to VITMAD1964)

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
