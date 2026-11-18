## Unreleased

### Verbessert
- Straßennummern im „Dann“-Satz: Die britischen Nummern erscheinen dort als dieselben blauen, grünen und weißen Schilder, an ihrem Platz im Satz. (Dank an karthikeyansr)
  EN: UK road numbers in the "Then" line: they show as the same blue, green and white signs, in their place in the sentence. (Thanks to karthikeyansr)

### Behoben
- Britische Straßenschilder: Straßen wie A1(M) zeigen ein blaues Autobahnschild statt eines grünen A1 mit „(M)“. Eine Nummer mit Leerzeichen („M 25“) bekommt jetzt auch ihr Schild. (Dank an karthikeyansr)
  EN: UK road signs: roads like A1(M) now show as one blue motorway sign instead of a green A1 with "(M)". A road number with a space ("M 25") now gets its sign too. (Thanks to karthikeyansr)

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
