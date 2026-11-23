## Unreleased

### Improved
- Suche als Knopf ist jetzt standardmäßig an. Zugeklappt zeigt die Karte nur den Such-Knopf. Aufgeklappt stehen unter der Suchleiste Zuhause, Arbeit, deine Orte, Laden oder Tanken, Essen, Kaffee, Parken und Geparkt. Mit dem Pfeil in der Suchleiste klappst du alles wieder zu.
  EN: Search as a button is now on by default. Folded, the map only shows the search button. Open, the search bar has Home, Work, your places, charging or fuel, food, coffee, parking and Parked under it. The arrow in the search bar folds it all away again.
- Ist Suche als Knopf aus, ist die Suchleiste mit ihren Knöpfen immer da. Sie klappt sich beim Verschieben der Karte nicht mehr weg.
  EN: With Search as a button off, the search bar and its buttons are always there. It no longer folds away when you move the map.
- Die Karte „Dein Auto“ mit „Zu Fuß“ ist weg, Geparkt übernimmt das. Geparkt steht jetzt bündig mit den Knöpfen darüber.
  EN: The "Your car" card with "Walk" is gone; Parked does that now. Parked now lines up with the buttons above it.

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
