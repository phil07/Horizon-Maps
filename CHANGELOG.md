## Unreleased

### New
- Größere 3D-Modelle für deinen Pfeil: Eine .glb über 8 MB (wie sie KI-Tools oft liefern) bekommt beim Auswählen kleinere Bilder, damit sie passt. Die Form bleibt gleich. Bis 48 MB wählbar.
  EN: Bigger 3D models for your arrow: a .glb over 8 MB (as AI tools often make them) has its pictures made smaller as it's picked, so it fits; the shape isn't changed. Up to 48 MB can be picked.
- „Langsamer fahren“: Einstellungen › Warnungen › „Langsamer fahren“ sagen spricht es nach dem Ton beim Überschreiten. „Eigene Höchstgeschwindigkeit“ (z. B. 120 km/h) piept und sagt es einmal, wenn du sie erreichst, egal welches Limit gilt. Beides standardmäßig aus.
  EN: "Slow down" in words: Settings › Alerts › Say "Slow down" speaks it after the beep when you go over the limit, and Top speed alert (say 120 km/h) beeps and says it once as you reach it, whatever the limit is. Both off by default.

### Fixed
- Verkehr auf der Routenleiste ist zu sehen: Ein kurzer Stau auf einer langen Fahrt verschwand im Blau. Jetzt zeigt er seine eigene Farbe auf einer etwas dickeren Leiste, nur an den Rändern weich.
  EN: Traffic on the route bar can be seen: a short patch on a long trip was blended away into the blue. It now shows in its own colour on a slightly thicker bar, soft only at its edges.
- Parkplätze am Routenende kommen wieder: Fand die erste Suche nichts, kam keine zweite. Jetzt wird bis zu dreimal neu gesucht.
  EN: Car parks near the end of a route come back: when the first look found nothing it wasn't tried again; now it's tried up to three times.
- Deutschland: Straßen mit „kein Limit“ außer Autobahnen, und als außerorts markierte, zeigen 100 km/h statt nichts oder der alten 70.
  EN: Germany: roads marked "no limit" that aren't motorways, and those marked rural, show 100 km/h instead of nothing or the old 70.

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
