# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Neu
- Pfeil seitlich lassen: Neuer Schalter (Einstellungen > Bildschirm & Größen > Beim Fahren, und unter Android Auto). Auf breitem Bildschirm bleibt der Pfeil seitlich, wenn sich die Karten einklappen, statt in die Mitte zu gleiten.
  EN: Keep the arrow to the side: a new switch (Settings > Screen & sizes > While driving, and under Android Auto) keeps the arrow to the side on a wide screen when the cards fold away, instead of gliding back to the middle.

### Verbessert
- Gong-Lautstärke: Gong bei Tempo, Tempolimit und Blitzern läuft auf dem gleichen Kanal wie die Sprachansagen, damit die Lautstärke des Geräts während einer Ansage auch den Gong leiser stellt. Einstellung unter Ton & Stimme > Gong-Lautstärke, mit Probe-Gong.
  EN: Chime volume: the speeding, speed limit and camera chimes now play on the same channel as the spoken directions, so the head unit's volume turned during a direction turns them down too. Settings > Sound & voice > Chime volume sets them against the voice, with a sample beep.

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
