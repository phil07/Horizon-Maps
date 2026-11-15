## Unreleased

### Behoben
- Kreisverkehr, geradeaus als „rechts abbiegen“: An einem Kreisverkehr, den man geradeaus durchfährt (z. B. B 44 bei Biblis), zeigte die Abbiegekarte einen Pfeil nach rechts und die Stimme sagte „rechts abbiegen“. Der Abbiegepunkt der Route liegt oft einige Meter vor dem Ring; diese Meter wurden mit in den Kreis gerechnet, der dadurch zu groß wurde. Der Kreis wird jetzt nur noch durch das Stück gelegt, das wirklich auf dem Ring liegt. Das gilt für Routen von Mapbox und von TomTom, auf dem Telefon, im Head-up-Display und in Android Auto.
  EN: Roundabout, straight on shown as "turn right": at a roundabout you drive straight through (e.g. the B 44 near Biblis), the turn card showed an arrow to the right and the voice said "turn right". A route's turn point often lies some metres before the ring; those metres were fitted into the circle, which made it too big. The circle is now laid only through the stretch that really is on the ring. This applies to routes from Mapbox and from TomTom, on the phone, the head-up display and Android Auto.

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
