# Änderungen

## 1.0.35

- Film- und Serienkataloge erhalten eine Streaming-Startseite mit Titelbereichen und horizontalen Posterreihen; der Live-Programmführer behält sein eigenes EPG-Raster.
- Vollbildsteuerung startet mit Fokus auf Play/Pause. Live-TV zeigt nur verfügbare Aktionen und macht Stopp per Fernbedienung erreichbar; VOD spult mit Links/Rechts in 10-Sekunden-Schritten.
- Portalstatus wird gespeichert. Nichterreichbare Quellen lassen sich deaktivieren; optional werden sie automatisch deaktiviert und aus Suche, Zusammenführung und Wiedergabe ausgeschlossen.
- Portalaktualisierungen melden geladene und nicht erreichbare Quellen. Die Quelleinstellung bietet Aktivieren/Deaktivieren und die automatische Behandlung.
- MAC-Adressen lassen sich über Präfixvorgaben und einzelne Hex-Zeichen eingeben; Portaladressen können mit HTTP/HTTPS-Vorgabe oder vollständig manuell bearbeitet werden.
- Trakt-Episoden werden auch dann den lokalen Episoden zugeordnet, wenn das Portal keine externe Serien-ID liefert, sofern Titel und Jahr eindeutig passen.

## 1.0.34

- Die Wiederherstellung und Zusammenführung großer Live-Senderlisten läuft im Hintergrund, damit Start und Fernbedienungsbedienung währenddessen reaktionsfähig bleiben.
- Der Schnellstart nutzt gespeicherte Senderdaten unabhängig von der Quellenwiederherstellung. Direkte Stream-URLs benötigen dabei keinen zusätzlichen Portal-Handshake.

## 1.0.33

- Die Wiederherstellung des Serienfortschritts wählt bei mehreren alten Speicherständen nur dann eine Folge aus, wenn deren Zeitstempel eindeutig ist. Ohne eindeutigen Stand wird kein falscher Fortsetzungsplatz geraten.

## 1.0.32

- Bei verbundener Trakt-Sitzung kommen Episodenstand und bekannte nächste Ausstrahlung aus Trakt; ohne Trakt gilt TMDB, andernfalls TVmaze. Das nächste bekannte Datum erscheint in den Seriendetails.
- Trakt-Titelbeschreibungen und Bewertungen werden auf Detailseiten verwendet; ohne eindeutigen Trakt-Treffer bleibt die Portalbeschreibung erhalten.
- Serien-Fortsetzen berücksichtigt Trakt-Fortschritt: Nach einer abgeschlossenen Folge wird die nächste verfügbare, noch nicht abgeschlossene Folge angeboten; angefangene Folgen behalten ihren Wiedergabestand.
- Zurück aus Serien-Details öffnet wieder den vorherigen Verlauf oder die Merkliste. Ein Folgenwechsel hält den Vollbildplayer geöffnet; nach der letzten Folge oder bei Stopp kehrt die App zur Serienansicht zurück.
- Der automatische Trakt-Abgleich beginnt erst nach dem Anzeigen des lokalen Startzustands. Der globale Ladehinweis blockiert die Navigation nicht.
- Die Diagnoseordner-Auswahl startet ohne wiederverwendete Provider-URI und fängt nicht verfügbare SAF-Dateiauswahl ab, statt die App zu beenden.

## 1.0.31

- Die Referenzansichten für Live-TV und Programmführer werden mit einer gemeinsamen dunklen Petrolpalette, einheitlichen Fokus-/Hover-Rahmen und konsistenten abgerundeten Schaltflächen angenähert. Der Fokus bleibt türkis; laufendes Programm und Live-Zeitmarke verwenden einen warmen Akzent.
- Trakt-Verläufe für Filme und Serien werden auf die zehn zuletzt gesehenen Titel begrenzt. Nicht zu Portalquellen passende Einträge erscheinen nicht im Verlauf; fehlende Treffer werden beim Synchronisieren mit derselben Portal-Suche wie in der Suche aufgelöst.

## 1.0.30

- Beim Trakt-Abgleich werden umfangreiche lokale Titelzuordnungen auf einem Hintergrund-Dispatcher berechnet. Dadurch bleibt die Oberfläche während des Imports reaktionsfähig und der Fortschrittsstatus kann weiter aktualisiert werden.
- Die Trakt-Synchronisierung blockiert die Navigation nicht mehr; Ladefortschritt und aktueller Arbeitsschritt bleiben sichtbar.
- Die Metadateneinstellungen zeigen die aktive Quelle für Folgeninformationen an. TVmaze wird automatisch verwendet, wenn kein TMDB-Zugang eingerichtet ist.

## 1.0.29

- Diagnoseberichte koennen in einen vom Nutzer gewaehlten Ordner exportiert werden; Zugangsdaten, Portal-URLs und Fehlermeldungstexte werden ausgelassen.
- Trakt-Synchronisierung zeigt den aktuellen Arbeitsschritt, bleibt waehrenddessen bedienbar und importiert Gesehen-Status aus der kompakten Serienuebersicht statt die komplette Wiedergabehistorie seitenweise abzurufen.
- Trakt-Antworten werden einschliesslich JSON-Auswertung im Hintergrund verarbeitet; der Fortschritt wird an die Oberflaeche zurueckgemeldet.
- Trakt-Verlauf wird gebuendelt verarbeitet, Zuordnungen pro Titel zwischengespeichert und Episodenpruefungen werden gestaffelt.
- Gleichzeitige Portalabfragen beim Laden von Serien sind begrenzt.

## 1.0.28

- Trakt-Verlauf, einzelne gesehene Folgen, Merkliste und pausierte Wiedergabestände werden beim Verknüpfen und über „Trakt-Verlauf und Merkliste synchronisieren“ abgeglichen.
- Filme und Folgen übertragen beim Start, Pausieren und Stoppen ihren Wiedergabestand an Trakt; nach dem Stoppen erscheint eine Bestätigung.
- Im Live-TV-Informationsfeld sind nicht bedienbare Schaltflächen vom D-Pad-Fokus ausgeschlossen; die Fokusgruppe macht die Stopp-Aktion erreichbar.

## 1.0.27

- Trakt-Gerätecode bleibt während der normalen HTTP-400-Warteantwort sichtbar; zusätzlich werden Trakts dokumentierte Statuscodes für Warten, Drosselung, Ablauf und Ablehnung behandelt.

## 1.0.26

- Trakt-Verbindung startet ohne manuelle Client-ID-Eingabe; Gerätecode-QR berücksichtigt auch `auth.trakt.tv` und kann den Code direkt auf der Aktivierungsseite einsetzen.

## 1.0.25

- Im Live-Programmführer erscheint während der Wiedergabe eine fokussierbare Stopp-Schaltfläche.

## 1.0.24

- Absturz beim Öffnen der Einstellungen behoben, der durch eine ungültige Auswahlansicht ausgelöst wurde.

## 1.0.23

- Trakt-Konto über Bestätigungscode und QR-Code verbinden; Merkliste und Gesehen-Status mit lokal verfügbaren Filmen und Serien synchronisieren.
- Programmführer nach App-Neustart in der zuletzt verwendeten Kategorie des zuletzt gesehenen Senders öffnen.

## 1.0.22

- Der zuletzt gespielte Sender kann aus der verschlüsselten Schnellstart-Momentaufnahme starten, während die große Portalbibliothek im Hintergrund wiederhergestellt wird.
- EPG-Daten aus dem lokalen Cache stehen zuerst bereit; Anbieter- und XMLTV-Abfragen laufen begrenzt parallel.
- Beim App-Start werden zuerst der aktuelle und angeheftete Sender aktualisiert; weitere Programmzeilen laden ihr EPG beim Anzeigen nach.
- Nach dem Zurückkehren aus einem Einstellungsbereich erhält dieser Bereich wieder den Fernbedienungsfokus.

## 1.0.21

- Fernbedienungseingaben für Intro- und Folgenhinweise werden beim Verlassen des Vollbildplayers freigegeben, damit die Navigation in Menüs und Folgenlisten wieder funktioniert.
- Hauptmenüeinträge haben eine feste Hoch-/Runter-Fokusreihenfolge; Wiederholen der Links-/Rechts-Taste bewegt die Seek-Leiste weiter.
- Die Seek-Leiste markiert bei Fokus nur noch den Positionspunkt.
- Intro-Erkennung liest echte Videobilder auch aus Nicht-HLS-Streams und speichert Vergleichsbilder lokal für spätere Folgen.
- Die Oberflächenerkennung prüft außerdem den Abspann auf eine passende Sprungmarke.

## 1.0.20

- Fernbedienungsfokus im Folgenhinweis bleibt auf den beiden Aktionen; Links/Rechts wählen nur „Nächste Folge“ oder „Wiedergabe stoppen“ und OK bestätigt.
- Die Fortschrittsleiste ist im Vollbild per Fernbedienung fokussierbar; Links/Rechts spulen dort in 10-Sekunden-Schritten.

## 1.0.19

- Die Folgen-Aktionsfenster fangen Fernbedienungstasten direkt am Player ab; Links/Rechts wechseln nur zwischen den Aktionen und OK bestätigt die Auswahl.

## 1.0.18

- Der Vollbild-Ladeindikator erscheint nur bei anhaltendem Puffern während einer bereits laufenden Wiedergabe.
- Links führt aus einer geöffneten Film- oder Serienkategorie zurück zur Kategorienliste.
- Fernbedienungsaktionen für Play/Pause, vorherige/nächste Folge, Stopp und Intro-/Folgenhinweise erhalten eindeutigen Fokus und sichtbare Markierung.

## 1.0.17

- Neues StreamWatch-App-Icon auch für Fire TV und aktualisiertes Android-TV-Banner.
- Wiederholte längere Pufferstopps wechseln zur nächsten Streamvariante; im Vollbild wird der Ladevorgang angezeigt.
- Folgen- und „Schaust du noch?“-Auswahl per Fernbedienung fokussierbar, ohne die Serienliste versehentlich zu öffnen.
- Folgenliste im Serien-Vollbild zeigt Folgenummern und blendet sich nach der eingestellten Zeit aus.
- Weitere vom Stalker-Portal gelieferte Ablaufdatumsformate werden erkannt.

## 1.0.16

- Portaladressen und MAC-Adressen lassen sich zwischen Geräten exportieren und importieren.
- Fire-TV-Launcher-Fallback auf das StreamWatch-Symbol umgestellt.

## 1.0.15

- Seriensteuerung im Vollbild mit vorheriger und nächster Folge sowie Stopp per Fernbedienung.
- Intro- und Outro-Aktionen unten rechts angeordnet.

- Neuer App-Name StreamWatch, adaptives Launcher-Icon und passendes Android-TV-Banner.
- Fernbedienungsfokus auf dem ersten Film, der primären Serienaktion oder dem zuletzt gesehenen Live-Sender.
- Lesbarere Programmbeschreibung mit eigener Spalte neben dem EPG auf breiten Bildschirmen.
- Vollbild-Steuerung mit Play/Pause-Fokus, automatischem Ausblenden und eigener Zurück-Aktion.
- Schnellstart des letzten Live-Senders aus einer verschlüsselten Momentaufnahme.
- Fernbedienungsfokus für Intro und nächste Folge, gespeicherte Intro-Erkennung und optionale Abfrage „Schaust du noch?“.
- Verschiebbare Kategorien, EPG-Beschreibung passend zur markierten Sendung und einstellbare Hauptmenü-Symbole.
- Weitere Formate für Portal-Ablaufdaten und eine Über-Seite mit Version und Versionsdatum.
- Live-Vollbild mit Senderwechsel per Hoch/Runter und Wischgeste. Die Belegung ist anpassbar.
- Serien-Vollbild mit direktem Zugriff auf Folgen, Staffeln und Hauptmenü über die linke Pfeiltaste.
- Intro-Erkennung auch in der kleinen Playeransicht. „Intro überspringen“ erscheint als anwählbare Schaltfläche. Automatisches Überspringen bleibt optional und standardmäßig ausgeschaltet.

## 1.0.14

- Bedienung, Wiedergabe und Katalognavigation verbessert.

## 1.0.13

- Links öffnet aus der Senderspalte oder Vollbild-Senderliste zuerst die Kategorien und danach das Hauptmenü.
- Vollbild-Senderliste mit laufenden Sendungen, Fortschritt und optionalem Tagesplan.
- Einstellbare Größe des Programmführers sowie getrennte Darstellungs- und Einstellungsseiten.
- Lokaler EPG-Cache beim Start mit Hintergrundaktualisierung.
- Originale Portalkategorien im Einzelportalmodus und optionale Gruppierung von Sendervarianten je Portal.

## 1.0.12

- Senderliste nur im Live-Vollbild öffnen. Links führt aus Film- und Seriendetails zurück in den Katalog.
- Langes Zurück holt die laufende Wiedergabe ins Vollbild. Beim Verlassen bleiben Staffel und Serienansicht erhalten, im Programmführer erhält der laufende Sender den Fokus.

## 1.0.11

- Programmführer per Fernbedienung in der Senderspalte starten und mit Rechts und Links durch die Zeitleiste navigieren.
- Film- und Seriendetails mit Zurück schließen. Die App lässt sich über zweimal Zurück im Hauptmenü beenden.

## 1.0.10

- Vereinfachte Navigation in Filme und Serien mit Merkliste und Verlauf unten in der Seitenleiste.
- Suchbuttons zeigen die Trefferzahlen automatisch, sobald der jeweilige Bereich fertig geladen ist.

## 1.0.9

- App-Updates in den Einstellungen mit optionaler Prüfung beim Start.
- Versionshinweise, APK-Prüfung und Übergabe an den Android-Paketinstaller.

## 1.0.8

- Vollbild mit einmal Zurück verlassen, Tasten und Gesten belegen und Vorschau für Filme und Serien einstellen.
- Gleichnamige Serienfassungen bei der Auswahl und Wiedergabe getrennt halten.
- Ablaufdatum bei Stalker-Quellen anzeigen, sofern das Portal es liefert.
