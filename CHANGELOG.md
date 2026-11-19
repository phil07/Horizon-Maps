## Unreleased

### Behoben
- Kosten für Sprit und Laden in Rupien, Kronen und mehr: Die Kosten auf den Routenoptionen erscheinen jetzt in der Währung des Landes, in dem du fährt. Außerhalb von Pfund-, Euro- und Dollarländern nimmt das Preisfeld den Preis so, wie er geschrieben ist (₹107.40 je Liter). Bisher wurde er als Paise gelesen, und die Kosten waren 100-mal zu klein. (Dank an bijindas)
  EN: Fuel and charging costs in rupees, kroner and more: the cost on the route options is now shown in the currency of the country you're driving in, and outside pound, euro and dollar countries the price box takes the price as normally written (₹107.40 a litre), where it used to read it as paise and show the cost 100 times too small. (thanks to bijindas)

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
