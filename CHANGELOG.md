# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Kreisverkehr im Head-up-Display: In Android Auto zeigt der Pfeil die richtige Seite. Die dritte Ausfahrt zeigte nach rechts wie die erste.
  EN: Roundabout on the head-up display: In Android Auto the arrow now points the right way. The third exit pointed right like the first.

### Verbessert
- Quellen-Button: Der (i)-Button von Mapbox passt sich der Karte an, hell mit dunklem „i“ bei heller Karte, dunkel mit hellem „i“ bei dunkler Karte. In Android Auto war er nur ein schwarzer Kreis.
  EN: Credits button: Mapbox's (i) button now follows the map, light with a dark "i" on a light map, dark with a light "i" on a dark map. In Android Auto it was a plain black circle.
- Tempolimit: Das kleine „Danach …“-Schild neben dem Tempolimit ist entfernt.
  EN: Speed limit: The small "Then …" sign next to the speed limit is gone.

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
