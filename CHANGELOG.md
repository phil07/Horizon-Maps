## Unreleased

### Behoben
- Satellitenbilder leer bei manchen: Seit Ende Oktober weist Mapbox die Satellitenbilder für viele ab ("zu viele Anfragen"), auch weit unter den Grenzen. Mit einem TomTom-Schlüssel springen jetzt TomToms Satellitenbilder automatisch ein, mit Mapbox-Straßen und -Namen darüber. Ohne TomTom-Schlüssel erklärt die App, was los ist. (Dank an atnr, Matattack99)
  EN: Satellite view blank for some people: since late October Mapbox has been refusing its satellite pictures for many people ("too many requests"), even well inside their limits. With a TomTom key, TomTom's satellite pictures now take their place automatically, with Mapbox's roads and names on top. Without a TomTom key, the app explains what is happening. (thanks to atnr, Matattack99)

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
