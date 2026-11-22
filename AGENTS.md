# Arbeitsanweisungen für Codex – IPTVPlayer

## Einstieg

Vor Änderungen diese Datei sowie `PROJECT_SPEC.md`, `ARCHITECTURE.md` und `IMPLEMENTATION_PLAN.md` im Projektroot lesen. Den vorhandenen Code, Build und Projektkonventionen prüfen, bevor neue Strukturen eingeführt werden. Diese Dokumente beschreiben das Ziel, nicht den bereits implementierten Zustand. Bei Widersprüchen gilt: ausdrücklicher aktueller Nutzerwunsch, dann diese Datei, dann Spezifikation und Architektur; den Plan entsprechend aktualisieren. Offene Entscheidungen nicht als bereits getroffen ausgeben.

## Vorgehen

- In kleinen, lauffähigen Schritten arbeiten; bestehende Funktionen und Nutzerdaten erhalten. Keine vollständige Neuanlage des Projekts ohne Auftrag.
- Bestehende Gradle-, Package-, SDK- und Abhängigkeitskonfiguration zuerst ermitteln. Kompatible, stabile Versionen verwenden; keine Versionsnummern oder vorhandene Module erfinden. Neue Bibliotheken begründen und nur bei Bedarf hinzufügen.
- Kotlin, Jetpack Compose, Media3 und Room einsetzen; UI-Zustand und Seiteneffekte klar trennen, strukturierte Coroutines verwenden. Keine Netz- oder Datenbankarbeit auf dem UI-Thread.
- Android-Touch, Android TV, Fire TV und Shield berücksichtigen: D-Pad, Fokus, Zurück-Taste, Lesbarkeit und Lebenszyklus prüfen. Gerätespezifische Fähigkeiten zur Laufzeit erkennen.
- Keine Sender, Inhalte, Zugangsdaten oder geschützten Testströme mitliefern. Nur berechtigte Nutzerquellen verwenden. URLs, Tokens, Passwörter, Playlist-Inhalte und personenbezogene Daten nicht protokollieren oder in Git einchecken.
- Änderungen an Datenbankschema mit Migration und Migrationstest begleiten. Destruktive Migrationen und Datenverlust vermeiden. Fehler verständlich und ohne Geheimnisse anzeigen.
- Relevante Tests und den verfügbaren Gradle-Build ausführen; fehlende Geräte, SDKs oder externe Zugangsdaten und nicht ausgeführte Tests offen benennen. Keine erfolgreiche Prüfung behaupten, die nicht stattfand.
- Keine Veröffentlichung, kein Push, kein Signaturschlüssel-Upload und keine Änderung externer Dienste ohne ausdrücklichen Auftrag. Release-Schlüssel gehören in sichere Secrets, nie ins Repository.
- Nach jeder Aufgabe Änderungen, Prüfung und verbleibende Grenzen knapp dokumentieren. Bei Architektur- oder Produktänderungen die betroffenen Dokumente mitführen.
- Bei Release-Änderungen die Hinweise im Abschnitt der passenden Version in `CHANGELOG.md` pflegen. Der GitHub-Workflow verwendet diesen Abschnitt für die veröffentlichten Release-Hinweise.

## Definition of Done

Akzeptanzkriterien des jeweiligen Arbeitspakets erfüllt; relevante Tests und Build erfolgreich oder Einschränkung dokumentiert; TV-Bedienbarkeit und Datenschutz für die Änderung geprüft; bestehende Daten bleiben erhalten; keine ungeklärten Geheimnisse oder unbeabsichtigten Dateien im Diff.
