## Unreleased

### Neu
- Höchsttempo für Ankunftszeiten: Die Zeitangaben einer Route rechnen nie schneller als die Geschwindigkeit, die du unter Einstellungen › Routen › Fahrzeug einstellst (etwa für Transporter, Lkw oder Anhänger). Die Route selbst bleibt gleich.
  EN: Top speed for arrival times: a route's times never assume faster than the speed set in Settings › Routes › Vehicle (for vans, lorries or towing). The route itself stays the same.
- Sonnenzeiten auf Android Auto: Unter Einstellungen gibt es „Sonnenzeiten auf Android Auto“. Dann folgen Tag und Nacht auf dem Autodisplay Sonnenauf- und -untergang, auch für Autos, die bei eingeschaltetem Licht immer dunkel werden.
  EN: Sun times on Android Auto: under Settings there is "Sun times on Android Auto". With it on, day and night on the car screen follow sunrise and sunset, also for cars that go dark whenever the headlights are on.

### Verbessert
- Abschnittskontrollen werden eigens abgefragt, mit mehr Geduld für langsame Server und einem neuen Versuch nach 20 Sekunden. Gefundene Kontrollen bleiben auf dem Gerät gespeichert, auch für Strecken ohne Netz.
  EN: Average speed checks are looked up on their own, with more patience for busy servers and a retry after 20 seconds. Checks found stay on the device, also for roads without signal.

### Behoben
- Tag und Nacht auf dem Handy: Auto folgt jetzt einfach der Hell- oder Dunkel-Einstellung des Handys. Soll die Karte bei Sonnenauf- und -untergang wechseln, wähle Sonnenzeiten.
  EN: Day and night on a phone: Auto now simply follows the phone's own light or dark setting. For the map to change at sunrise and sunset instead, pick Sun times.
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
