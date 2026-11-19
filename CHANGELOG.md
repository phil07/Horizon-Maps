## Unreleased

### Improved
- Abbiegekarte an Autobahnausfahrten: Der blaue Streifen „AUSFAHRT 4“ ist weg. Das Ausfahrt-Schild neben dem Straßennamen zeigt die Nummer schon, und die Pfeile des Streifens zeigten immer nach rechts, auch bei einer Ausfahrt links. Danke an mjdshibly.
  EN: Turn card at motorway exits: the blue "EXIT 4" strip is gone. The exit badge beside the road name already shows the number, and the strip's arrows always pointed right, even for an exit on the left. Thanks to mjdshibly.

### Fixed
- Blasse Spurpfeile: Im großen Spurbild hatten die blassen Pfeile (Spuren, die du nicht nehmen sollst) hellere Stellen, wo ihre Linien zusammenkamen und wo zwei Pfeile einen Stiel teilen. Die Pfeile einer Spur werden jetzt als eine Form gezeichnet, die blassen sind gleichmäßig blass. Danke an mjdshibly.
  EN: Faded lane arrows: in the big lane picture the faded arrows (lanes not to use) had brighter patches where their lines joined and where two arrows share a stem. Each lane's arrows are now drawn as one shape, so the faded ones are evenly faded. Thanks to mjdshibly.

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
