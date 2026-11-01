# Horizon Nav

Navigations-App für Android und Android Auto: Karten, Routenführung mit Ansagen, Verkehr, Warnungen, Ladeplanung für Elektroautos und Spritpreise. Ohne Benutzerkonto, ohne Werbung, ohne Tracking.

## Herkunft

Dieses Projekt baut auf dem originalen **Horizon Maps** von [benbishop028-arch](https://github.com/benbishop028-arch) auf:
👉 https://github.com/benbishop028-arch/Horizon-Maps

Die Entwicklung hier ist eine Weiterentwicklung dieses Originals. Vielen Dank an den ursprünglichen Autor für die Grundlage.

## Funktionen (Auswahl)

- **Navigation** mit Mapbox-Karten, Spurhinweisen, Tempolimits, Auto-Zoom, 2D/3D und Offline-Paketen
- **Android Auto** mit eigener Kartenansicht und Bedienelementen
- **Meldungen** (Polizei, Unfall, Gefahr, Panne, mobiler Blitzer, Stau), anonym mit anderen Fahrern geteilt, mit „Noch da?“-Abstimmung
- **Elektroauto**: Akkustand vom Auto oder aus dem Widget der Hersteller-App, automatische Ladestopps, Ladekurven aus Open EV Data, bevorzugte Ladeanbieter
- **Spritpreise** live für Deutschland (Tankerkönig), Österreich, Frankreich und Großbritannien
- **Deutsche Sprachbefehle**, z. B. „Bring mich nach Hause“
- **Ziele vom Handy ans Radio senden**, Parkplatz merken, Wetter und Regenradar

Alle Änderungen stehen in [CHANGELOG.md](CHANGELOG.md).

## Installation

- **GitHub-Version**: APK aus dem [neuesten Release](https://github.com/phil07/Horizon-Maps/releases/latest) installieren. Die App aktualisiert sich selbst.
- **Google-Play-Version (interner Test)**: nötig für Android Auto, da Android Auto seit Juni 2026 selbst installierte Navigations-Apps nicht mehr anzeigt. Einrichtung: [docs/play-einrichtung.md](docs/play-einrichtung.md).

## Projektaufbau

| Ordner | Inhalt |
| --- | --- |
| `app/` | Android-App (Kotlin, Jetpack Compose, Mapbox, Android for Cars) |
| `worker/` | Meldungs-Server (Cloudflare Worker mit D1-Datenbank) |
| `docs/` | Datenschutzerklärung und Anleitungen |
| `scripts/` | Hilfsskripte |
| `.github/workflows/` | Build, Release und Server-Deployment |

Mehr zur Code-Struktur: [ARCHITECTURE.md](ARCHITECTURE.md), Pläne: [ROADMAP.md](ROADMAP.md).

## Bauen

Die App wird über GitHub Actions gebaut (`release-apk.yml`). Jeder Push auf `main` erzeugt ein Release mit APK und, wenn der Upload-Schlüssel hinterlegt ist, dem Play-Bundle (`HorizonNav-play.aab`). API-Schlüssel und Signaturschlüssel liegen nur als GitHub-Secrets vor und gehören nie ins Repository.

## Datenschutz

Siehe [docs/datenschutz.md](docs/datenschutz.md).
