# IPTVPlayer – Umsetzungsplan

Dieser Plan ist eine Reihenfolge, kein Nachweis vorhandener Funktionen. Zuerst Ist-Zustand, offene Entscheidungen und Abweichungen erfassen. Jede Phase endet mit einem lauffähigen Stand und den passenden Prüfungen aus `AGENTS.md` und `ARCHITECTURE.md`.

## 0. Bestandsaufnahme und Entscheidungen

- Gradle, SDK, Package-ID, bestehende Architektur, Tests, Daten und Geräteunterstützung prüfen; funktionierenden Ausgangsbuild dokumentieren.
- Mindest-SDK und Zielgeräte, tatsächliches Verhalten beim App-Verlassen, Herkunft von Länderinformationen, erlaubte Quellformate und Datenschutz-/Backupstrategie festlegen. Bereits vorhandene Datenmigration planen.
- Bei unklaren Produktentscheidungen sinnvolle reversible Voreinstellung dokumentieren und vor irreversiblen Änderungen nachfragen.
**Fertig:** Ist-Zustand und konkrete nächste Schritte bekannt; keine vorhandene Funktion versehentlich ersetzt.

## 1. Grundgerüst und Bedienung

Compose-Navigation und adaptive Touch-/TV-Oberflächen an vorhandene Struktur anpassen; Fokus, Zurück-Verhalten, Zustandsverwaltung, Room-Grundlage und sichere Speicherung vorbereiten. Keine unnötigen Module hinzufügen.
**Fertig:** App startet auf verfügbaren Zielgeräten; Navigation per Touch und D-Pad funktioniert; Build und relevante Tests laufen.

## 2. Quellen und Katalog

M3U-URL/Datei, Xtream-Adapter, Stalker-Adapter, Quellverwaltung, geschützte Zugangsdaten, transaktionale Aktualisierung und Fehlerstatus implementieren. Für Stalker-Portale Anmeldung, Sitzungsablauf und die Übernahme des verfügbaren Katalogs mit synthetischen Antworten prüfen. Quellenübergreifende Identitäten, sichere Zusammenführung, manuelle Zuordnung, Prioritäten, Kategorien, Länderfilter und Favoriten ergänzen.
**Fertig:** Zwei synthetische Quellen importieren und aktualisieren; eindeutige Einträge zusammenführen, mehrdeutige getrennt halten; Filter und Nutzerzuordnungen bleiben nach Aktualisierung erhalten.

## 3. Live und Failover

Media3-Controller und Live-Oberfläche anbinden; Wiedergabe über Navigation erhalten, nur explizite Auswahl schaltet um. Endlichen Variantenwechsel bei relevanten Fehlern und verständliche Fehlerzustände umsetzen.
**Fertig:** Navigation verändert laufenden Sender nicht; Ausfall der ersten Variante führt kontrolliert zur zweiten; Abbruch, Erschöpfung und manuelle Auswahl sind geprüft.

## 4. XMLTV und EPG

XMLTV-Import, ID- und manuelle Zuordnung, Zeiten, Aktualisierung und EPG-Oberfläche implementieren. Fehlende EPG-Daten unabhängig von Live behandeln.
**Fertig:** Programme und lokale Uhrzeit stimmen bei Zeitzonenwechsel; EPG-Navigation lässt Live laufen; unzugeordnete Sender funktionieren weiterhin.

## 5. VOD

Filme, Serien, Details und Episoden aus verfügbaren Quellen anzeigen; expliziten Wechsel von Live zu VOD, Wiedergabesteuerung und lokalen Fortschritt ergänzen.
**Fertig:** Start, Pause, unterstütztes Suchen, Neustart und Fortsetzen funktionieren; fehlende Inhalte und Netzwerkfehler werden aufgefangen.

## 6. Temporäre QR-Verwaltung

Transport und Sicherheitsmodell vorab festlegen; zeitbegrenzten lokalen Dienst, QR-Sitzung, Formular, Bestätigung und zuverlässiges Beenden realisieren. Keine dauerhafte Freigabe.
**Fertig:** Ein zweites Gerät kann eine bestätigte Änderung vornehmen; abgelaufene oder ungültige Sitzung scheitert; Dienst endet wie spezifiziert. Geheimnisse werden geschützt und nicht geloggt.

## 7. Optionale BYO-Cloud

Speicheranbieter und Konfliktregeln auswählen; Opt-in, Auswahl der Daten, lokale Verschlüsselung und Schlüsselableitung, Upload/Download, Konfliktbehandlung und Wiederherstellung implementieren. Sicherheitsverfahren vor Einsatz überprüfen.
**Fertig:** Sync bleibt ohne Einrichtung aus; zwei Testgeräte synchronisieren ausgewählte Daten; falsche Passphrase, Manipulation, Offline-Konflikt und Verlustszenario sind geprüft.

## 8. CI, Releases und Updates

GitHub Actions für reproduzierbaren Build und geeignete Tests einrichten; Rechte minimieren, Secrets aus Logs halten. Signierung über sichere Repository-Secrets konfigurieren, wenn vorhanden; Release auf expliziten Tag/Workflow begrenzen. APK und SHA-256 veröffentlichen; optionalen nutzerinitiierten Update- und Installationsablauf ergänzen.
**Fertig:** CI prüft Änderungen; Test-Release erzeugt installierbare signierte APK, Upgrade mit gleichem Schlüssel funktioniert und Release-Artefakte sind prüfbar. Keine Veröffentlichung ohne Auftrag.

## 9. Gesamtprüfung

Abnahme aus `PROJECT_SPEC.md` mit Touch und TV-Fernbedienung durchführen; unterschiedliche Bildschirmgrößen, schlechte Verbindung, Neustart, größere Bibliotheken, Migration, Datenschutz und Ressourcenverbrauch prüfen. Nicht verfügbare Fire-TV-/Shield-Geräte ausdrücklich als Testlücke benennen. Dokumentation und bekannte Grenzen aktualisieren.

## Noch zu entscheiden

Konkrete SDK-/Gerätematrix und Versionen; belastbare Länder-Metadaten; Hintergrundverhalten außerhalb der App; QR-Transport und Schutzmodell; Cloud-Anbieter, Datenumfang, KDF-Parameter und Konflikt-UX; Release-Verteilung und Update-Quelle; automatische nächste Episode. Keine dieser Optionen als fertige Implementierung ausgeben.
