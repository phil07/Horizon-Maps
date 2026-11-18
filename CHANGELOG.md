## Unreleased

### Neu
- Stopp hinzufügen, Tanken: Auf der Route zeigt jede Tankstelle den Preis deiner Kraftstoffsorte, die günstigste in Grün. Darunter steht der Umweg in Minuten.
  EN: Add a stop, fuel: along your route every petrol station shows the price of your fuel, with the cheapest in green and the detour time under it.
- Neuer Reiter „Günstigste“ neben „Geringster Umweg“.
  EN: A new Cheapest tab next to Least detour.
- Tankstellen mit Preis, die die Kartensuche nicht gefunden hat, werden in die Liste aufgenommen.
  EN: Priced stations that the map search missed are added to the list.
- Preise kommen wie bei der Tankstellenliste: Fuel Finder (Großbritannien), und die Preisdienste in Frankreich, Österreich und Deutschland (mit Schlüssel).
  EN: Prices come from the same place as the fuel list: Fuel Finder (UK), and the price services in France, Austria and Germany (with a key).

### Verbessert
- Stopp hinzufügen: Die Knöpfe Tanken, Laden, Kaffee, Essen, Raststätten und Toiletten sind jetzt runde farbige Symbole wie unter der Suchleiste, in einer Reihe.
  EN: Add a stop: the Fuel, Charger, Coffee, Food, Services and Toilets buttons are now the same round coloured icons as under the search bar, in one row.

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
