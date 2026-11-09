# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Verbessert
- Kreisverkehr bei TomTom-Routen: Abbiegebox und Head-up-Display zeigen die Ausfahrt im genauen Winkel (z. B. halblinks) statt nur links, geradeaus oder rechts.
  EN: Roundabouts on TomTom routes: The turn card and head-up display show the exit at its real angle (e.g. half left) instead of just left, straight or right.

### Behoben
- Kreisverkehr seitenrichtig: Abbiegebox, Kartenbild und Head-up-Display zeigen die Ausfahrt jetzt auf der richtigen Seite. Die dritte Ausfahrt zeigte nach rechts wie die erste.
  EN: Roundabouts the right way round: The turn card, the map and the head-up display now show the exit on the correct side. The third exit pointed right like the first.
- Gewählte Route bleibt: Eine Alternative statt der schnellsten Route wird jetzt auch nach dem Start beibehalten. Bei einer Neuberechnung (z. B. nach kurzer GPS-Abweichung) und wenn Android Auto die Fahrt übernimmt, wechselt die App nicht mehr auf die schnellste. Schnellere Wege werden nur noch angeboten.
  EN: Chosen route kept: An alternative picked instead of the fastest route is now kept after starting. A reroute (e.g. after brief GPS drift) or Android Auto taking over the trip no longer switches to the fastest. Faster ways are only offered.

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
