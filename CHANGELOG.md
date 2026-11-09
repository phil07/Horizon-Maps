# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Eigenes Design: Unter Einstellungen > Karte > Eigenes Design lassen sich Routen-, Pfeil- und Akzentfarbe wählen und die Karte heller oder dunkler stellen, mit Live-Vorschau.
  EN: Own design: Settings > Map > Own design sets the route, arrow and accent colour and makes the map lighter or darker, with a live preview.

### Verbessert
- Routenvergleich: Jede Routenkarte zeigt jetzt auch Maut und Vignette, neben Stauminuten und Ladestopps.
  EN: Route comparison: Each route card now also shows tolls and vignettes, next to traffic minutes and charging stops.
- Englisch: Einrichtung, „Was ist neu“ und alle Einstellungstexte gibt es jetzt vollständig auf Englisch.
  EN: English: Setup, What's new and all settings texts are now fully in English.

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
