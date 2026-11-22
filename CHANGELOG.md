# Änderungen

## Unveröffentlicht

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
