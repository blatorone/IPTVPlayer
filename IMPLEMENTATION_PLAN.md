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

Der Programmführer und die Vollbild-Senderliste verwenden dieselben EPG-Daten. Der Anbieter-Cache ist persistent und XMLTV liest den lokalen Stand vor einer Netzaktualisierung. Bildschirmdarstellung und Vollbild-Overlay haben getrennte Einstellungsseiten.

XMLTV-Import, ID- und manuelle Zuordnung, Zeiten, Aktualisierung und EPG-Oberfläche implementieren. Fehlende EPG-Daten unabhängig von Live behandeln.
**Fertig:** Programme und lokale Uhrzeit stimmen bei Zeitzonenwechsel; EPG-Navigation lässt Live laufen; unzugeordnete Sender funktionieren weiterhin.

## 5. VOD

Filme, Serien, Details und Episoden aus verfügbaren Quellen anzeigen; expliziten Wechsel von Live zu VOD, Wiedergabesteuerung und lokalen Fortschritt ergänzen.
**Fertig:** Start, Pause, unterstütztes Suchen, Neustart und Fortsetzen funktionieren; fehlende Inhalte und Netzwerkfehler werden aufgefangen.

## 6. Temporäre QR-Verwaltung

Transport und Schutzmodell sind festgelegt (siehe `PROJECT_SPEC.md`, Fernverwaltung): Cloudflare-Vermittler per WebSocket, Seite über Cloudflare Pages, Ende-zu-Ende-Verschlüsselung mit Geheimnis im QR-Fragment. Schritte:

1. Verschlüsselung und Rahmenformat mit Tests (`RemoteSessionCrypto`) – umgesetzt.
2. Nachrichtenformat für Einstellungen (Lesen des Zustands, Änderungen, Bestätigung) mit Tests.
3. Sitzung am Gerät: Start in den Einstellungen, QR-Code, Verbindungsstatus, „Zugriff beenden“, Ablauf, Ende beim Verlassen der App.
4. Verwaltungsseite (HTML/JS im Repo, WebCrypto) für alle Einstellungen.
5. Vermittler (Cloudflare Worker im Repo) und Veröffentlichung – Einrichtung im Cloudflare-Konto nur auf ausdrücklichen Auftrag.
6. Optional: direkte Heimnetz-Verbindung als Notlösung.
**Fertig:** Ein zweites Gerät kann eine bestätigte Änderung vornehmen; abgelaufene oder ungültige Sitzung scheitert; Dienst endet wie spezifiziert. Geheimnisse werden geschützt und nicht geloggt.

## 7. Optionale BYO-Cloud

Speicheranbieter und Konfliktregeln auswählen; Opt-in, Auswahl der Daten, lokale Verschlüsselung und Schlüsselableitung, Upload/Download, Konfliktbehandlung und Wiederherstellung implementieren. Sicherheitsverfahren vor Einsatz überprüfen.
**Fertig:** Sync bleibt ohne Einrichtung aus; zwei Testgeräte synchronisieren ausgewählte Daten; falsche Passphrase, Manipulation, Offline-Konflikt und Verlustszenario sind geprüft.

## 8. CI, Releases und Updates

Der Workflow und der Update-Client stehen im Projekt. Der Workflow baut und testet auf Pull Requests und auf `master`. Ein Tag `v<versionName>` veröffentlicht die mit dem bisherigen Schlüssel signierte APK mit Prüfsumme und Hinweisen aus `CHANGELOG.md` im öffentlichen Repository `blatorone/IPTVPlayer-Releases`. Dafür braucht der Workflow die Signing-Secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`, `RELEASE_KEY_ALIAS` und `UPDATE_RELEASE_TOKEN` mit Schreibzugriff auf Inhalte des Download-Repositorys.

**Abnahme:** CI auf GitHub ausführen, Signing-Secrets und Download-Kanal einrichten, ein signiertes Test-Release veröffentlichen und das Upgrade auf der Shield mit derselben Signatur prüfen. Nach der einmaligen Installation einer Version mit Update-Client kann die App folgende Releases anbieten.

## 9. Gesamtprüfung

Abnahme aus `PROJECT_SPEC.md` mit Touch und TV-Fernbedienung durchführen; unterschiedliche Bildschirmgrößen, schlechte Verbindung, Neustart, größere Bibliotheken, Migration, Datenschutz und Ressourcenverbrauch prüfen. Nicht verfügbare Fire-TV-/Shield-Geräte ausdrücklich als Testlücke benennen. Dokumentation und bekannte Grenzen aktualisieren.

## Ergänzung: Trakt-Konto und Live-Wiederherstellung

- Trakt-Gerätecode-Verknüpfung mit fester App-Client-ID, lokalem QR-Code, verschlüsselter Sitzung und Token-Erneuerung ist integriert. Anmeldung und manuelle Aktualisierung synchronisieren Merkliste, Filmverlauf, einzelne gesehene Folgen und pausierte Wiedergabestände; Filme/Folgen melden Wiedergabe per Scrobble an Trakt. Nutzer müssen keine Entwickler-ID eingeben.
- Live-Kategorie wird zusammen mit dem zuletzt verwendeten Live-Sender lokal wiederhergestellt. Der Programmführer startet in der Kategorie dieses Senders.
- Build-Prüfung: `assembleDebug` erfolgreich. OAuth mit einem echten Trakt-Konto und Bedienung auf Shield/Fire TV müssen noch auf Geräten geprüft werden.

## Diagnose, Lastbegrenzung und Metadatenanbieter

- Diagnoseereignisse und letzter Absturzbericht lassen sich lokal sichern und auf Wunsch in einen SAF-Ordner exportieren. Export ohne URLs, Zugangsdaten und Exception-Nachrichten.
- Trakt-Import und Portalstaffelabfragen begrenzen Zustandsaktualisierungen und gleichzeitige Anfragen. Lokale Titelzuordnungen beim Trakt-Import laufen auf einem Hintergrund-Dispatcher; die Navigation bleibt waehrend des Abgleichs bedienbar. Das Verhalten auf einem realen Shield/Fire-TV-Geraet muss mit einem Diagnosebericht und realer Bibliothek geprueft werden.
- Umgesetzt: Episodenstatus, Titelbeschreibung/Bewertung und bekannte nächste Ausstrahlung verwenden Trakt, falls verbunden, sonst TMDB bei eingerichtetem Zugang, andernfalls TVmaze. Das Datum wird in Seriendetails angezeigt. Länderspezifische Ausstrahlungszeiten bleiben gesondert zu prüfen; Trakt liefert die bekannte Ausstrahlung der Serie, nicht zwingend die vom Nutzer gewählte Portalregion.

## StreamWatch 1.0.35

- Film- und Serienübersicht auf horizontale Posterreihen mit hervorgehobenem Titel umgestellt; der Live-Programmführer bleibt separat.
- Portal-Erreichbarkeit und Aktivierung werden im verschlüsselten lokalen Portalbestand gespeichert. Automatische Deaktivierung nicht erreichbarer Quellen ist standardmäßig aktiv und kann umgestellt werden.
- D-Pad-Steuerung im Player: Play/Pause erhält den anfänglichen Fokus, Live zeigt nur verwendbare Aktionen und VOD nutzt Links/Rechts zum Suchen.
- Debug- und Release-Build sowie Veröffentlichung über das bestehende Release-Workflow-Tag vorsehen. Shield/Fire-TV-Abnahme bleibt mangels aktuellem Gerätezugriff offen.

## StreamWatch 1.0.36

- Hintergrundberechnung großer VOD-Gruppen und geschützter Portalstatus-Callback, damit Quellenfehler nicht mehr die UI beenden.
- Diagnoseexport als einzelne Datei zusätzlich zur Ordnerauswahl; interne Protokolle bleiben frei von Portaladressen und Inhalten.
- Shield-Start per ADB gemessen: Start war erfolgreich, die erste sichtbare Ansicht erschien aber nach etwa 82 Sekunden. Nach dem Hintergrundumbau muss der Release auf der Shield erneut auf Startzeit und Portalabruf geprüft werden.

## Noch zu entscheiden

Aus der Sichtung der TV-Referenz folgen weitere Funktionspakete für Aufnahmen, Anbieterarchive, Erinnerungen, Kindersicherung (Grundfunktion seit 1.0.85 umgesetzt, siehe ARCHITECTURE.md), Datensicherung, erweiterte Fernbedienungsbelegung und gerätespezifische Wiedergabe. Für Aufnahmen und Archiv sind getrennte Abnahmekriterien zu Speicherziel, Gerät und unterstützter Quelle erforderlich.

Konkrete SDK-/Gerätematrix und Versionen; belastbare Länder-Metadaten; Hintergrundverhalten außerhalb der App; Cloud-Anbieter, Datenumfang, KDF-Parameter und Konflikt-UX; automatische nächste Episode. Keine dieser Optionen als fertige Implementierung ausgeben.
