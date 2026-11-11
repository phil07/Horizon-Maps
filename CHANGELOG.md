# Changelog

Add user-facing Horizon Nav changes under **Unreleased** before publishing each build, in German (the app shows them as they are), under ### Neu, ### Verbessert or ### Behoben headings, each written as "Überschrift: was es tut" (the What's new card shows the headline in bold with the rest underneath). Under each entry, an indented line "  EN: Headline: what it does" gives its English for the app in English (GitHub and Google Play get the German only). Keep entries focused on features, improvements, and fixes in the app—not GitHub Actions or release-publishing maintenance. There is no need to delete entries after publishing: notes that an earlier release already included are left out of the next one automatically. After a build has been published, remove its entries from Unreleased again: the What's new card shows exactly what is in this section, so anything left there is shown again with the next build.

## Unreleased

### Behoben
- Fahrtkarte verdeckt das Stopp-Panel nicht mehr: Solange das Panel für einen Stopp oder alle Standorte offen ist, tritt die Fahrtkarte zur Seite und kommt danach wieder.
  EN: Trip card no longer covers the stop panel: While the add-a-stop or all-locations panel is open, the trip card steps aside and comes back when it closes.
- Ein Shop-Filter lässt Orte weg, die den Shop nur erwähnen: Ein Western-Union-Schalter in einem Sainsbury's gehört nicht mehr zur Kette.
  EN: Shop search leaves out places that only mention the shop: A Western Union counter inside a Sainsbury's no longer counts as a branch of the shop.

### Verbessert
- Android Auto auf kleinen Bildschirmen: Auf 800 × 480 sind Zuhause, Arbeit und die Schnellsuche schmaler, die Fahrtkarte ist eine Zeile und das Tempolimit-Schild kleiner.
  EN: Android Auto on small screens: At 800 × 480 Home, Work and the quick search are slimmer, the trip card is one line and the speed sign is smaller.

### Neu
- Stopp entlang der Route: Beim Hinzufügen eines Stopps zeigt „Entlang der Route suchen“ (oder die Suchtaste der Tastatur) alle Treffer entlang der restlichen Strecke auf der Karte, jeweils mit dem Zeitaufwand. Sortierbar nach geringstem Umweg, am frühesten oder am nächsten. Auf Android Auto als Liste nach Umweg.
  EN: Add a stop, seen along your route: When adding a stop, "See them along your route" (or the keyboard's search key) lists every match along the rest of the route, each with the time it adds. Sort by least detour, soonest or nearest. On Android Auto it's a list sorted by detour.

### Verbessert
- Schont den Akku: Die App rechnet weniger doppelt, und ein Tempolimit wird auf derselben Straße nicht gleich wieder abgefragt.
  EN: Saves battery: The app works out less twice, and a speed limit on the same road isn't looked up again straight away.

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
