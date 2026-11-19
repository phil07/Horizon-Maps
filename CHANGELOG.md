## Unreleased

### Neu
- Ankunftszeit senden: Ein neuer Knopf auf der Fahrtkarte öffnet das Teilen-Menü deines Handys mit einer fertigen Nachricht („Auf dem Weg nach …, Ankunft etwa 10:42“) und einem Kartenlink, zum Senden per WhatsApp, SMS oder wie du willst. Er erscheint nur, wenn du stehst, damit niemand beim Fahren einen Kontakt wählt. (Dank an Dotancom)
  EN: Send your arrival time: a new button on the trip card opens your phone's share menu with a message ready ("On my way to …, arriving about 10:42") and a map link, to send by WhatsApp, text or anything else. It shows only while you're stopped, so nobody picks a contact while driving. (thanks to Dotancom)
- Spritpreise in Spanien: Die Live-Preise jeder Tankstelle aus dem spanischen Ministerium für ökologischen Wandel (kostenlos, ohne Schlüssel) erscheinen in der Kraftstoffliste, bei „Günstigste“ in „Stopp hinzufügen“ und bei den Tankvorschlägen. Sie werden auf dem Gerät gespeichert und etwa alle vier Stunden aktualisiert, sodass Preise auch bei einem Ausfall des Dienstes angezeigt werden. (Dank an franciscoserranoadt)
  EN: Fuel prices in Spain: every petrol station's live prices from the Spanish Ministry for the Ecological Transition (free, no key), in the fuel list, Cheapest in Add a stop and the fuel stop suggestions. Kept on the device and refreshed every few hours, so prices still show if the service is down for a while. (thanks to franciscoserranoadt)

### Verbessert
- Die Zurück-Taste in den Einstellungen bleibt stehen: Auf dem Handy im Hochformat sitzen der Zurück-Pfeil und der Seitenname jetzt in einer Leiste oben auf jeder Einstellungsseite statt im Scrollen. Die Einstellungen werden oben und unten sanft ausgeblendet. (Dank an yurividal)
  EN: Settings back button stays put: on phones held upright, the back arrow and the page name now sit in a bar at the top of each settings page instead of scrolling away, and the settings fade softly at the top and bottom edges. (thanks to yurividal)

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
