# IPTVPlayer – Produktspezifikation

## Ziel und Rahmen

Ein nutzerkonfigurierbarer IPTV-Player für Android-Telefone und -Tablets sowie Android TV, Fire TV und Nvidia Shield. Die App liefert keine Sender, Playlists oder Zugangsdaten und umgeht keine Zugangsbeschränkungen. Nutzer fügen eigene, zur Nutzung berechtigte Quellen hinzu. Kotlin, Compose, Media3 und Room bilden den technischen Rahmen. Mindest-SDK, Geräteabdeckung und konkrete Versionen werden nach Prüfung des bestehenden Projekts festgelegt.

## Quellen und Bibliothek

- Mehrere M3U-/M3U8-Playlisten per URL oder vom Nutzer gewählter Datei hinzufügen, ändern, aktualisieren und entfernen. Relative URLs anhand der Playlist-Quelle auflösen; Metadaten wie `tvg-id`, Name, Logo und Gruppe berücksichtigen. Fehlerhafte Einträge isolieren und Importergebnis anzeigen.
- Xtream-kompatible Quellen mit Serveradresse und Zugangsdaten unterstützen; Live, Filme, Serien, Kategorien und verfügbare Metadaten übernehmen. Zugangsdaten geschützt ablegen und aus Anzeigen und Logs heraushalten.
- Stalker-Portale als eigenen Quelltyp unterstützen. Nutzer geben die Portaladresse und die vom Anbieter autorisierten Anmeldedaten oder Gerätekennungen ein. Live-Sender, Kategorien, EPG-Informationen, Filme und Serien übernehmen, soweit das jeweilige Portal sie bereitstellt. Zugangsdaten und Sitzungstokens geschützt behandeln.
- XMLTV aus konfigurierbaren Quellen importieren und EPG-Zuordnungen anhand stabiler IDs ermöglichen. Zeitzonen und Programmzeiten korrekt behandeln; fehlende Daten dürfen Wiedergabe nicht verhindern.
- Live-Sender aus mehreren Quellen in einer Bibliothek zusammenführen. Identische Sender nur bei verlässlicher ID oder ausdrücklicher Nutzerzuordnung vereinigen; keine Zusammenführung allein aufgrund ähnlicher Namen. Ein zusammengeführter Sender besitzt priorisierte, einzeln identifizierbare Wiedergabevarianten. Quellherkunft und manuelle Korrektur bleiben zugänglich.
- Kategorien und Länderfilter anbieten; unbekannte oder nicht eindeutig ableitbare Länder als „Unbekannt“ behandeln. Filter kombinieren, zurücksetzen und leere Ergebnisse verständlich anzeigen. Favoriten und benutzerdefinierte Zuordnungen über Aktualisierungen erhalten, soweit stabile Identitäten verfügbar sind.
- Live-TV, Filme und Serien öffnen zunächst ihre Kategorien. Nutzer können Kategorien individuell per langem Druck ausblenden und über die Sichtbarkeitseinstellungen wieder einblenden. Die Länderauswahl gilt für die Kategorien aller drei Bereiche und bleibt lokal gespeichert.

## Live, EPG und Navigation

- Senderliste, aktuelle und folgende Sendung sowie zeitbasierte EPG-Ansicht anbieten, soweit Daten vorhanden sind. EPG-Zuordnung und Aktualisierungsstatus sichtbar machen.
- Beim Navigieren durch Listen und EPG läuft der aktuell gestartete Live-Sender weiter. Fokuswechsel oder bloßes Markieren startet keinen anderen Stream; erst ausdrückliche Auswahl wechselt den Sender. Zurück und Oberflächenwechsel dürfen die laufende Wiedergabe nicht unbeabsichtigt beenden. Explizites Stoppen und Verlassen gemäß Lebenszyklus bleiben möglich.
- Bei abspielbarem Variantenfehler begrenzt und ohne Endlosschleife zur nächsten verfügbaren Variante desselben zusammengeführten Senders wechseln. Fehler, Versuch und aktuelle Quelle nachvollziehbar anzeigen, ohne vertrauliche URLs offenzulegen. Keine anderen Sender automatisch auswählen. Nutzer können erneut versuchen oder Quelle wählen.
- Touch und Fernbedienung gleichwertig nutzbar: sichtbarer Fokus, logische D-Pad-Reihenfolge, erreichbare Aktionen und vorhersehbare Zurück-Navigation.

## VOD

- Filme und Serien getrennt vom Live-Angebot durchsuchen; Detailansicht und Episodenauswahl bereitstellen, sofern die Quelle sie liefert. VOD startet nur nach bewusster Auswahl und ersetzt dann die laufende Live-Wiedergabe. Pause, Suche und Fortsetzen anbieten, soweit Format und Quelle dies erlauben.
- Wiedergabeposition lokal speichern und Fortsetzen oder Neustart anbieten; abgeschlossene Einträge angemessen behandeln. Fehler oder fehlende Metadaten dürfen andere Bibliotheksbereiche nicht blockieren. Automatische nächste Episode ist eine offene Produktentscheidung und zunächst nicht vorauszusetzen.

## Temporäre QR-Playlistverwaltung

Auf ausdrücklichen Start des Nutzers stellt das Gerät eine zeitlich begrenzte Verwaltungsseite im lokalen Netzwerk bereit. Ein QR-Code öffnet sie auf einem anderen Gerät. Nutzer können Quellen eingeben, ändern oder entfernen und müssen Änderungen bestätigen. Der Dienst endet nach Ablauf, beim expliziten Beenden und beim Verlassen des Verwaltungsablaufs; aktive Verbindungen schließen. Ein zufälliges, kurzlebiges Sitzungsgeheimnis schützt die Sitzung; keine Zugangsdaten im QR-Code oder in Logs. Zugriff nur im lokalen Netz, keine Portfreigabe oder Cloud-Vermittlung. Da lokales HTTP mitlesbar sein kann, sensible Daten nur nach verständlichem Hinweis und möglichst über eine abgesicherte Verbindung übertragen; konkrete sichere Transportlösung vor Implementierung entscheiden. Netzwerkisolierung und nicht erreichbare Geräte verständlich behandeln.

## Optionale Cloud-Synchronisierung

Standardmäßig aus; kein Herstellerkonto erforderlich. Nutzer stellen ihren eigenen unterstützten Speicher bereit. Nur nach Einrichtung und Einwilligung werden ausgewählte Einstellungen, Favoriten, Zuordnungen und optional Quellen/Zugangsdaten geräteübergreifend synchronisiert; keine Wiedergabedaten oder Geheimnisse stillschweigend hochladen. Vor Übertragung auf dem Gerät authentifiziert verschlüsseln; Schlüssel aus einer vom Nutzer gewählten Passphrase mit geeigneter passwortbasierter Schlüsselableitung und zufälligem Salt ableiten. Schlüssel und Passphrase nicht hochladen oder loggen. Nutzer über Verlust der Passphrase und mögliche Metadaten beim Speicheranbieter informieren. Löschung und Deaktivierung klar unterscheiden; Konflikte und Offline-Änderungen nicht stillschweigend überschreiben. Anbieter, Schlüsselparameter und Konfliktoberfläche sind vor Umsetzung festzulegen.

## Updates und Qualität

GitHub Actions baut und prüft das Projekt; versionierte GitHub-Releases können signierte APKs und SHA-256-Prüfsummen bereitstellen. Ein optionaler Update-Check zeigt Version und Release-Hinweise; Download und Installation erfolgen nur nach Nutzeraktion und Android-Systembestätigung. Signaturkontinuität und Integrität vor Installation berücksichtigen; SHA-256 allein beweist keine Herkunft. Store-Verteilung und Plattformregeln getrennt prüfen. Keine automatische Installation versprechen.

## Abnahme

Mindestens: zwei Quellen importieren, eindeutig zusammengeführten Sender mit zwei Varianten abspielen und Failover testen; EPG passend zuordnen; Kategorien/Länderfilter kombinieren; beim Navigieren Live ohne Wechsel fortsetzen; VOD starten und Position fortsetzen; QR-Sitzung starten, Änderungen bestätigen und Ablauf prüfen; Cloud optional verschlüsselt und konfliktbewusst synchronisieren; CI baut und testet, Release-Artefakt ist installierbar. Touch und D-Pad jeweils auf geeigneten Geräten oder Emulatoren prüfen. Netzwerkfehler, leere Daten und Neustart berücksichtigen.
