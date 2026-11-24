# IPTVPlayer – Produktspezifikation

## Ziel und Rahmen

Die App erscheint auf Android und Android TV unter dem Namen **StreamWatch**. Der Anzeigewechsel übernimmt bestehende Installationen über die Paket-ID `com.iptvplayer.app` und den vorhandenen Update-Kanal.

Ein nutzerkonfigurierbarer IPTV-Player für Android-Telefone und -Tablets sowie Android TV, Fire TV und Nvidia Shield. Die App liefert keine Sender, Playlists oder Zugangsdaten und umgeht keine Zugangsbeschränkungen. Nutzer fügen eigene, zur Nutzung berechtigte Quellen hinzu. Kotlin, Compose, Media3 und Room bilden den technischen Rahmen. Mindest-SDK, Geräteabdeckung und konkrete Versionen werden nach Prüfung des bestehenden Projekts festgelegt.

## Quellen und Bibliothek

- Der Schnellstart kann die letzte Live-Auswahl aus einer kleinen verschlüsselten Momentaufnahme öffnen, während die Bibliothek wiederhergestellt wird. Ein fehlendes oder unpassendes Abbild verwendet den normalen Startpfad.
- Der gespeicherte Quellen- und Katalogstand bleibt nach dem Start sofort sichtbar und bedienbar. Eine aktivierte vollständige Portalaktualisierung vergleicht Kataloge im Hintergrund; EPG-Aktualisierung ist getrennt einstellbar. Neue Daten werden übernommen, vorhandene Einträge bleiben erhalten; ein fehlgeschlagener Refresh lässt den letzten lokalen Stand bestehen.
- Automatische vollständige Portalabrufe beim Start werden pro Portal auf höchstens einen Abruf je sechs Stunden begrenzt. Dazwischen prüft die App den Zugang und verwendet den gespeicherten Katalog; eine manuelle Aktualisierung umgeht die Wartezeit.
- Bei einem Katalogabgleich werden lokale und geladene Sender, Genres und Titel verglichen. Unveränderte Inhalte behalten ihren gespeicherten Stand; geänderte Inhalte werden aktualisiert und neue Inhalte ergänzt.
- Eine neu bestätigte Portalquelle wird vor dem vollständigen Katalogimport gespeichert. Jeder fertig geladene Bereich wird nachgesichert, sodass ein unterbrochener Import bereits vorhandene Portalinformationen nicht verwirft.
- Die Quellenverwaltung bietet getrennte Aktionen für die Aktualisierung aller Quellen und für eine Mehrfachauswahl einzelner Quellen. Während einer laufenden Startprüfung bleibt die Bedienoberfläche verfügbar; manuelle Aktualisierungen werden anschließend ausgeführt.
- Beim Rückweg aus dem Live-Vollbild bleibt der Guide in der Kategorie des laufenden Senders und stellt dessen Eintrag samt Fokus wieder her.
- Große Film- und Serienkataloge werden asynchron gruppiert, damit das Anzeigen der gespeicherten Startansicht nicht auf die Katalogberechnung wartet.
- Die Senderansicht bleibt während des Gruppierens aufrufbar; Portal-, Sendergruppen- und Trakt-Abgleiche zeigen einen sichtbaren Fortschritt. Der gespeicherte Senderstand wird dabei weiter angezeigt.
- Die Quellenübersicht zeigt ein lokal gespeichertes Ablaufdatum aus dem Geräteprofil oder den Kontoinformationen des Portals. Weitere Formatvarianten werden nur für angemeldete Portale geprüft.

- Die Quellenliste zeigt das Ablaufdatum eines Stalker-Portals an, sofern das Portal es im Geräteprofil bereitstellt. Das Datum wird beim Hinzufügen und Aktualisieren der Quelle eingelesen.
- Die Quellenliste zeigt für jedes gespeicherte Portal die lokal gefundenen, innerhalb des Portals entdoppelten TV-, Film- und Serienzahlen. Eine zusätzliche Gesamtzeile zählt gemeinsame Sender und Titel jeweils nur einmal.

- Der Portalstatus (erreichbar, nicht erreichbar oder ungeprüft) wird lokal gespeichert. Portale können manuell deaktiviert werden; standardmäßig deaktiviert die App eine beim Handshake nicht erreichbare Quelle automatisch. Automatisch deaktivierte Quellen werden bei später erfolgreicher Prüfung wieder aktiviert; manuell deaktivierte Quellen bleiben aus. Deaktivierte Quellen bleiben gespeichert, werden aber aus Suche, Zusammenführung, Metadatenabgleich und Wiedergabe ausgeschlossen.
- Nach Portalaktualisierungen zeigt eine kurze Meldung die erfolgreichen und nicht erreichbaren Quellen an. Die Quellenverwaltung zeigt ihren Status und bietet Aktivieren/Deaktivieren sowie eine Einstellung für die automatische Deaktivierung.
- Diagnoseberichte lassen sich als einzelne Datei exportieren, ohne zuvor einen Ordner auswählen zu müssen.

- Mehrere M3U-/M3U8-Playlisten per URL oder vom Nutzer gewählter Datei hinzufügen, ändern, aktualisieren und entfernen. Relative URLs anhand der Playlist-Quelle auflösen; Metadaten wie `tvg-id`, Name, Logo und Gruppe berücksichtigen. Fehlerhafte Einträge isolieren und Importergebnis anzeigen.
- Xtream-kompatible Quellen mit Serveradresse und Zugangsdaten unterstützen; Live, Filme, Serien, Kategorien und verfügbare Metadaten übernehmen. Zugangsdaten geschützt ablegen und aus Anzeigen und Logs heraushalten.
- Stalker-Portale als eigenen Quelltyp unterstützen. Nutzer geben die Portaladresse und die vom Anbieter autorisierten Anmeldedaten oder Gerätekennungen ein. Live-Sender, Kategorien, EPG-Informationen, Filme und Serien übernehmen, soweit das jeweilige Portal sie bereitstellt. Zugangsdaten und Sitzungstokens geschützt behandeln.
- Beim Stalker-Portal gespeicherte Quellen separat auswählen oder eine manuelle HTTP-, HTTPS- beziehungsweise vollständige Adresse eingeben. Für MAC-Adressen bekannte Präfixe anbieten und ein benutzerdefiniertes Hex-Präfix sowie die sechs Gerätezeichen per Eingabe oder Einzelzeichen-Auswahl erlauben.
- XMLTV aus konfigurierbaren Quellen importieren und EPG-Zuordnungen anhand stabiler IDs ermöglichen. Zeitzonen und Programmzeiten korrekt behandeln; fehlende Daten dürfen Wiedergabe nicht verhindern.
- Live-Sender, Filme und Serien mehrerer Quellen standardmäßig gemeinsam anzeigen. Die Quellverwaltung bietet eine Einzelansicht und eine gemeinsame Aktualisierung gespeicherter Quellen. Live-TV ordnet Sender eigenen Kategorien zu. Stimmen Sendername und Land nach dem Entfernen eindeutiger Portal- und Qualitätszusätze überein, können die Streams als Varianten eines Senders erscheinen. Zusätzlich sind verlässliche EPG-IDs und manuelle Zuordnungen vorgesehen. Varianten derselben Quelle werden vor Varianten weiterer Quellen versucht.
- Kategorien und Länderfilter anbieten; unbekannte oder nicht eindeutig ableitbare Länder als „Unbekannt“ behandeln. Filter kombinieren, zurücksetzen und leere Ergebnisse verständlich anzeigen. Favoriten und benutzerdefinierte Zuordnungen über Aktualisierungen erhalten, soweit stabile Identitäten verfügbar sind.
- Live-TV, Filme und Serien öffnen zunächst ihre Kategorien. Ein Dialog ermöglicht das Ein- und Ausblenden mehrerer Kategorien. Ausgeblendete Kategorien sind dort gekennzeichnet. Die Länderauswahl und ihre Reihenfolge gelten für alle drei Bereiche und bleiben lokal gespeichert. Nach einem Portalimport wird die Auswahl standardmäßig angeboten.

## Live, EPG und Navigation

- Filme, Folgen, Staffeln und Serien lassen sich in der Detailansicht als gesehen oder ungesehen markieren. Bei verbundenem Trakt-Konto überträgt die Aktion die Änderung auch an den Trakt-Verlauf. Staffel- und Serienaktionen betreffen die geladenen Folgen. Bei erneut gestarteter und unterbrochener Wiedergabe zeigen Balken und Fortsetzung die aktuelle Position.

- Die Hauptnavigation sitzt in Live-TV, Filmen, Serien, Suche und Einstellungen oben. Einstellungen zeigen ihre Bereiche direkt darunter. Film- und Serienseiten verwenden denselben dunklen Streaming-Stil; der Live-Bereich behält das EPG-Raster.
- Rechts vom fokussierten Sender im Programmführer führt die Fernbedienung in die EPG-Zeitleiste; von dort lassen sich Sendungen und spätere Zeitfenster auswählen. Beim Öffnen des Wiedergabemenüs zeigt Live-TV Sender, laufende Sendung und EPG-Beschreibung.
- Die Live-TV-Gruppierung beschränkt die teure Zusammenführung auf die ausgewählten Länder. Die Ländererkennung selbst verwendet lokale Sendernamen und Providerkategorien; Stalker-Portale bieten dafür nicht verlässlich einen serverseitigen Filter.
- Film- und Seriendetails fokussieren bei Fernbedienungsnavigation die primäre Aktion (Fortsetzen, Film abspielen oder erste Folge starten). Die Rubrikenliste der Einstellungen beginnt beim Öffnen mit Fokus auf der ersten Rubrik. Eine sichtbare Zurück-Schaltfläche ist für Touch-Geräte einstellbar und auf TV-Geräten standardmäßig ausgeblendet; System-Zurück navigiert immer eine Ebene zurück.

- Die App verwendet in allen Bereichen eine gemeinsame Streaming-Oberfläche mit tiefschwarzen Flächen, weißer Schrift und rotem Fokusakzent. Kategorien, Senderlisten, Poster, Einstellungen und Dialoge teilen dieselben Schaltflächen- und Fokuszustände. Live-TV behält seine IPTV-Programmführung mit Zeitleiste; laufende Sendungen können zusätzlich einen warmen Akzent nutzen.
- Der primäre Akzent ist in den Darstellungseinstellungen zwischen Rot, Blau, Grün, Violett und Orange wählbar. Titel und Kurzbeschreibung im Player-Overlay bleiben hell, Staffel- und Folgenangaben verwenden den gewählten Akzent.
- Der sichtbare Trakt-Verlauf zeigt höchstens die zehn neuesten Filme beziehungsweise Serien, die einer gespeicherten Portalquelle zugeordnet werden können. Bei der Zuordnung wird dieselbe portalbasierte Suche und Identitätsprüfung wie bei der Suche verwendet.
- Serien erhalten eine eigene Ansicht „Weiter schauen“ für gesehene, noch nicht abgeschlossene Serien. Trakt-Episodenstände werden paginiert importiert; der Trakt-Serienfortschritt unterscheidet abgeschlossene von noch laufenden Serien. Abgeschlossene Titel zeigen keinen Fortsetzen-Button.

- Im Vollbild startet die Fernbedienungsbedienung auf Play/Pause. Im Live-TV sind nur Play/Pause und Stopp sichtbar; Stopp ist per D-Pad erreichbar. Bei Filmen und Serien bewegen Links/Rechts die Wiedergabeposition in Zehn-Sekunden-Schritten, auch wenn die Steuerleiste ausgeblendet ist.
- In der VOD-Steuerleiste sind Suchlauf, Untertitel- und Player-Einstellungen mit der Fernbedienung erreichbar. Ein eigener Untertitelknopf öffnet die erkannten Textspuren und bietet „Aus“. Beim Springen mit Links/Rechts wird die Zeitleiste eingeblendet und fokussiert; Akzentfarbe gilt einheitlich für Suchpunkt, Fortschritt und fokussierte Player-Schaltflächen.
- Beim Starten oder Fortsetzen eines Films beziehungsweise einer Folge bleibt zunächst die Detailansicht sichtbar. Erst wenn die Wiedergabe tatsächlich läuft, wechselt der Player ins Vollbild; schlägt der Start fehl, bleibt die Detailansicht mit dem Fehler sichtbar. Automatische Folgenwechsel bleiben im Vollbild.

- Die Programmbeschreibung bleibt an Sender und Zeit der markierten Sendung gebunden. Das Kontextmenü einer Kategorie erlaubt das Verschieben oder Ein- und Ausblenden. Die Reihenfolge wird getrennt für Live-TV, Filme und Serien gespeichert. Das Hauptmenü kann Symbole, Text oder beides anzeigen.

- Die Vollbild-Senderliste zeigt laufende Sendung und Fortschritt pro Sender. Auf breiten Geräten kann daneben der Tagesplan mit der Beschreibung der ausgewählten Sendung erscheinen. Listenbreite, Deckkraft, Schließzeit und Verhalten beim Senderwechsel sind einstellbar.
- Unter **Einstellungen → Darstellung** lassen sich Zeilenhöhe, Schrift, Videovorschau, Sendernummern und Logos im Programmführer einstellen. Die übrigen Einstellungen sind in eigene Bereiche gegliedert.
- Bereits geladene EPG-Daten erscheinen nach einem Neustart aus dem lokalen Cache. Fehlende oder fällige Daten werden im Hintergrund geladen. Der Start des zuletzt gesehenen Senders wartet nicht auf eine EPG-Abfrage und ist unter **Allgemein** abschaltbar.
- Bei getrennten Quellen verwendet Live-TV die Gruppen des gewählten Portals. Standardmäßig bleiben Sendername, Varianten und Reihenfolge des Portals erhalten. Das Zusammenfassen von Sendervarianten lässt sich je Einzelportal einschalten. Suche und VOD-Details zeigen im Einzelportalmodus die Portalmetadaten.

- Android TV zeigt die App über einen eigenen Launcher-Eintrag an. Ein Touchscreen ist für den Start und die Bedienung per Fernbedienung keine Voraussetzung.
- Ohne gespeicherte Quelle bietet der erste Start direkt die Portal-Einrichtung an. Nach dem erfolgreichen ersten Import öffnet sich der Programmführer. Die automatische EPG-Aktualisierung setzt eine eingerichtete Quelle voraus.

- Als EPG-Quelle ist standardmäßig der IPTV-Anbieter ausgewählt. Alternativ können mehrere XMLTV-URLs eingerichtet werden. Aktualisierung beim Start, Intervall, manuelles Aktualisieren und Vorschauzeitraum sind einstellbar. Im Live-Vollbild schließen Senderliste und Programmbeschreibung nach 15 Sekunden oder bei Berührung des Videobildes.

- App-Kopfzeilen und Menürahmen bleiben beim Scrollen stehen. Sender, Suchergebnisse, Filme und Folgen scrollen in ihren jeweiligen Listen. Nur im Live-Vollbild öffnen horizontale Gesten oder Richtungstasten Senderliste und Programmdetails, während der Sender weiterläuft. Im normalen Programmführer bleibt Rechts bei der Zeitleisten- und Listenbedienung.

- Nach dem App-Start ist zunächst der Programmführer geöffnet. Sobald der zuletzt erfolgreich gestartete verfügbare Live-Sender wieder läuft, öffnet die App das Vollbild. Im Programmführer startet eine Senderauswahl in der Vorschau. Ein Klick auf die Vorschau öffnet das Vollbild. Ein Senderwechsel in der Vollbild-Senderliste behält das Vollbild bei. Das Hauptmenü erscheint auf Anforderung.

- Live-TV öffnet den zeitbasierten Programmführer mit Kategorien links. Programme werden für sichtbare Zeilen und benachbarte Sender nachgeladen. Beim Öffnen von Live-TV, Filmen oder Serien weicht das Hauptmenü der jeweiligen Bereichsansicht.
- Über dem Programmraster stehen links die anklickbare Live-Vorschau und daneben die Informationen zur markierten Sendung. Die Senderliste öffnet mit Links nur im Live-Vollbild. In „Alle Sender“ nimmt sie die Kategorie des laufenden oder markierten Senders. Die Auswahl einer Sendung in der Zeitleiste öffnet deren Details.
- Während die App im Vordergrund sichtbar ist, bleibt der Bildschirm auch ohne Wiedergabe aktiv. Beim Wiederherstellen gespeicherter Quellen erscheint beim Start ein Ladebalken.

- Bei der Live-Kategorisierung hat „Allgemeines“ für erkannte deutschsprachige Allgemein- und Unterhaltungssender Vorrang vor dem Portalgenre. Dazu zählen etwa Das Erste, ZDF, RTL, Sat.1, VOX und die ProSieben-Sender.

- Senderliste, aktuelle und folgende Sendung sowie zeitbasierte EPG-Ansicht anbieten, soweit Daten vorhanden sind. EPG-Zuordnung und Aktualisierungsstatus sichtbar machen.
- Beim Navigieren durch Listen und EPG läuft der aktuell gestartete Live-Sender weiter. Fokuswechsel oder bloßes Markieren startet keinen anderen Stream; erst ausdrückliche Auswahl wechselt den Sender. Zurück und Oberflächenwechsel dürfen die laufende Wiedergabe nicht unbeabsichtigt beenden. Explizites Stoppen und Verlassen gemäß Lebenszyklus bleiben möglich.
- Bei abspielbarem Variantenfehler begrenzt und ohne Endlosschleife zur nächsten verfügbaren Variante desselben zusammengeführten Senders wechseln. Fehler, Versuch und aktuelle Quelle nachvollziehbar anzeigen, ohne vertrauliche URLs offenzulegen. Keine anderen Sender automatisch auswählen. Nutzer können erneut versuchen oder Quelle wählen.
- Touch und Fernbedienung gleichwertig nutzbar: sichtbarer Fokus, logische D-Pad-Reihenfolge, erreichbare Aktionen und vorhersehbare Zurück-Navigation.
- Die Einstellungen markieren Fokus und Mauszeiger farblich. Zweimaliges Zurück beendet die App bei geöffnetem Hauptmenü. Im Programmführer und in der Vollbild-Senderliste führt Links zuerst zu den Kategorien und danach ins Hauptmenü. Rechts öffnet die Zeitleiste am aktuellen Programm, und Links führt über frühere Programme zurück zur Senderspalte. Die erneute Auswahl des laufenden Senders oder langes Zurück öffnet eine laufende Live-Vorschau im Vollbild.

Im Programmführer erscheint während aktiver Live-Wiedergabe eine fokussierbare Stopp-Aktion, die den Stream beendet.

## VOD

- Film- und Serienkataloge verwenden eine Streaming-Startseite mit höchstens zwei beliebten Titeln im großen Bildbereich und darunter der vollständigen Posterreihe der gewählten Kategorie. Neu hinzugefügte Titel erscheinen als eigene Kategorie statt als zusätzliche Reihe unter der Kategorieansicht. Kategorien, Filter, Sortierung und Fernbedienungsfokus bleiben erreichbar. Für Anbieter-Kategorien ist die Anzeige von Markensymbol, Symbol mit Namen oder Name allein wählbar. Der Live-Programmbereich behält separat seine IPTV-Zeitleiste.
- Poster und Senderlogos werden speicherbegrenzt lokal zwischengespeichert und nach einem Neustart wiederverwendet, damit bekannte Bilder schneller angezeigt werden.

- Beim automatischen Folgenwechsel pausiert „Schaust du noch?“ nach einer einstellbaren Anzahl von Folgen ohne Eingabe. Die Abfrage ist standardmäßig nach drei Folgen aktiv. Ohne Bestätigung wird nach 30 Sekunden gestoppt.

- Beim Öffnen einer Filmkategorie erhält der erste sichtbare Titel den Fokus. Nach dem Verlassen der Details erhält der zuvor ausgewählte Titel den Fokus zurück. Seriendetails fokussieren „Fortsetzen“ bei offenem Wiedergabefortschritt oder auf die nächste verfügbare, noch nicht abgeschlossene Folge nach der zuletzt gesehenen Folge. Nur wenn keine Fortsetzung ermittelt werden kann, steht dort der Start der ersten verfügbaren Folge.

- Langes Zurück öffnet standardmäßig die aktive Live-TV-, Film- oder Serienwiedergabe im Vollbild. Einmaliges Zurück kehrt zum laufenden Sender im Programmführer oder zur geöffneten Film- oder Serienansicht samt Staffel zurück. Langes Zurück, Live-TV-Richtungen und Gesten sowie Tipp und Wiedergabetaste im Vollbild lassen sich mit Aktionen belegen. Die Standardbelegung lässt sich wiederherstellen.
- Die mittlere Taste öffnet im Vollbild die Wiedergabesteuerung mit Fokus auf Play/Pause. Die Richtungstasten bewegen den Fokus innerhalb der Steuerung. Zurück oder sechs Sekunden ohne Eingabe schließen die Steuerung, während das Vollbild geöffnet bleibt.
- Im Live-Vollbild wechseln Hoch und Hochwischen zum nächsten Sender, Runter und Runterwischen zum vorherigen. Der Wechsel folgt der sichtbaren Kategorie. Für Filme und Serien sind die vier vertikalen Eingaben standardmäßig frei und im Mapping belegbar.
- Portaladressen und Gerätekennungen lassen sich als JSON sichern und auf einem anderen Gerät importieren. Der Export enthält Zugangsdaten und wird vom Nutzer sicher verwahrt.
- Im Serien-Vollbild öffnet Links nacheinander die Folgen der aktuellen Staffel, die Staffeln und das Hauptmenü. Die Folgen sind mit Nummer und verfügbarem Titel beschriftet. Rechts und Zurück gehen eine Ebene zurück. Die Folgenliste schließt nach der eingestellten Einblendzeit; langes Zurück schließt sie sofort. Aktionsdialoge behalten ihren Fokus und leiten Links/Rechts nicht an die Listen-Navigation weiter.
- Filme und Serien können getrennt nach dem Vollbild in einer kleinen Vorschau oder im Hintergrund weiterlaufen. Die Option „Aus“ pausiert die Wiedergabe. Die kleine Vorschau ist voreingestellt.
- Gleichnamige Serienfassungen werden nur bei übereinstimmender IMDb-/TMDB-ID oder bei passendem Titel und bekanntem, gleichem Erscheinungsjahr zusammengeführt. Widersprüchliche IDs trennen die Fassungen.

- Filme und Serien öffnen die zuletzt gewählte sichtbare Kategorie oder die erste verfügbare Kategorie. Der laufende Live-Sender erscheint standardmäßig abgedunkelt hinter dem Katalog neben der linken Kategorienliste. Die Darstellung lässt sich auf eine kleine Vorschau oben rechts oder ausgeblendetes Bild umstellen.

- Film- und Serienkategorien zeigen Poster und bieten Sortierungen nach Name, Erscheinungsjahr und Hinzufügedatum, soweit das Portal die Metadaten liefert. Eine Detailansicht bietet Aktionen für Favoriten, Merkliste und die bewusste Wiedergabe.
- Filme und Serien lassen sich nach Erscheinungsjahr filtern. Für aktuelle Kinostarts, wöchentliche Trends, Streamingdienst-Verfügbarkeit und Ausstrahlungen verwendet die App nach Einrichtung die offizielle TMDB-Schnittstelle. Die IPTV-Quellen liefern dazu passende Wiedergabevarianten.
- Die App vergleicht bei Serien auf der Merkliste und im Serienverlauf die Folgenliste mit dem zuletzt erfassten Stand. Neue Folgen werden in den Listen, Seriendetails und Staffelansichten markiert.
- Für Episodenstatus und bekannte nächste Ausstrahlung gilt Trakt, wenn ein Konto verbunden ist; sonst TMDB bei eingerichtetem Zugang, andernfalls TVmaze. Ein bekanntes Datum der nächsten Folge erscheint in den Seriendetails. Regionale Abweichungen werden nur angezeigt, wenn der gewählte Metadatenanbieter sie zuverlässig liefert.
- Die Suche berücksichtigt die Länderauswahl und zeigt Trefferzahlen auf den Schaltflächen für Live-TV, Filme und Serien. Sender erscheinen mit Logo, VOD-Titel mit Poster und verfügbaren Informationen. Die Herkunft bleibt am Treffer sichtbar.
- Die Suche startet Film- und Serienabfragen gleichzeitig. Jede Schaltfläche zeigt ihre Trefferzahl, sobald die Abfrage für den Bereich fertig ist. Bis dahin zeigt sie Auslassungspunkte.
- Filme und Serien zeigen oben keine allgemeine Überschrift oder Zurück-Schaltfläche. Merkliste und Verlauf sind unten in der Seitenleiste erreichbar. Zurück und Links am Rand der Detailansicht führen eine Ebene zurück. Bei der Rückkehr in die Kategorie bleiben Filter, Scrollposition und der zuletzt ausgewählte Titel erhalten.

- Filme und Serien getrennt vom Live-Angebot durchsuchen; Detailansicht und Episodenauswahl bereitstellen, sofern die Quelle sie liefert. VOD startet nur nach bewusster Auswahl und ersetzt dann die laufende Live-Wiedergabe. Pause, Suche und Fortsetzen anbieten, soweit Format und Quelle dies erlauben.
- Wiedergabeposition lokal speichern und Fortsetzen oder Neustart anbieten; abgeschlossene Einträge angemessen behandeln. Beim Wechsel auf eine abspielbare Variante desselben Films oder derselben Folge die letzte Wiedergabeposition übernehmen. Die nächste Folge startet auf Wunsch am markierten Abspannbeginn oder am tatsächlichen Ende. Die Vorwarnzeit ist einstellbar. Eindeutig markierte Intros können manuell oder automatisch übersprungen werden.
- Der Filmverlauf zeigt die gespeicherte Position. Der Serienverlauf und die Merkliste zeigen bei angesehenen Serien auch Staffel und Folge. „Fortsetzen“ öffnet den zugehörigen Inhalt an der unter Berücksichtigung abgeschlossener Trakt-Folgen nächsten passenden Stelle.
- Die regulären Detailseiten zeigen „Fortsetzen“ ebenfalls, sobald ein Filmfortschritt oder eine fortsetzbare Folge vorliegt.
- Seriendetails zeigen die Aktionen nebeneinander und darunter eine horizontal wählbare Staffelreihe über den Folgen mit verfügbaren Portalbildern, Beschreibungen und lokalem Wiedergabestatus. Die vollständige Serienbeschreibung und die Folgen sind in einem gemeinsamen vertikalen Detailbereich erreichbar.
- Eindeutig englische Beschreibungen werden im deutschen VOD-Bereich ausgelassen. Folgenbilder und deutsche Folgenbeschreibungen können bei eingerichtetem TMDB-Zugang ergänzt werden. Ohne Folgenbild dient das Serienposter als Ersatz.
- Für Intro und Abspann lässt sich getrennt zwischen automatischer Erkennung und eigenen Zeitmarken wählen. Im manuellen Modus gelten die gespeicherten Einstellungen für alle Serien. Der Introstart und das Introende beziehen sich auf den Folgenbeginn, der Abspannbeginn auf die verbleibende Laufzeit. Der Vorlauf für den Hinweis zur nächsten Folge bleibt separat einstellbar.
- Intros werden standardmäßig anhand wiederkehrender Bilder und Titelmusik erkannt und mit einer anwählbaren Überspringen-Schaltfläche angezeigt. Nach zwei angesehenen Folgen einer Serie erscheint die Schaltfläche in weiteren Folgen bereits während des Intros. Automatisches Überspringen ist eine separate, standardmäßig ausgeschaltete Option.

## Diagnose und Absturzberichte

- StreamWatch speichert knappe Diagnoseereignisse und den letzten Absturzbericht lokal. Unter **Einstellungen > Ueber** kann ein Ordner gewaehlt und ein Bericht manuell exportiert werden.
- Diagnoseexporte duerfen keine Portaladressen, Zugangsdaten, Wiedergabelinks, Tokens oder Inhalte enthalten. Fehlertexte werden nicht exportiert; Absturzberichte enthalten nur Fehlerklassen und Stackframes.

## Trakt-Konto

- Nutzer können Trakt in den Einstellungen über den offiziellen Gerätecode-Ablauf verbinden. Die App enthält ihre öffentliche Trakt Client ID; Nutzer müssen keine Entwickler-ID eingeben. Die App zeigt Bestätigungscode und einen lokal erzeugten QR-Code, der auch `auth.trakt.tv`-Aktivierungslinks mit Gerätecode unterstützt.
- Zugriffstoken und rotierendes Erneuerungstoken werden verschlüsselt und außerhalb der Android-Backups gespeichert. Beim Verknüpfen und bei manueller Synchronisierung werden Trakt-Merkliste, Filmverlauf, gesehene Serienfolgen und pausierte Wiedergabestände geladen. Vorhandene Portalquellen werden anhand stabiler IDs und Titel zugeordnet; falls ein Titel im lokalen Katalog fehlt, wird er in den Portalen gesucht. Nur gefundene Portalinhalte sind abspielbar.
- Filme und Folgen melden Start, Pause und Stopp an Trakt. Beim Stoppen wird die bestätigte Wiedergabeaktion angezeigt. Wiedergabestände werden zwischen Geräten über Trakt abgeglichen; lokale neuere Stände haben Vorrang vor älteren Importen.
- Die App weist darauf hin, dass StreamWatch Trakt verwendet, aber nicht von Trakt unterstützt oder zertifiziert wird.

## Live-Startkategorie

- Der Programmführer öffnet die zuletzt gewählte Live-Kategorie. Beim Start wird die Kategorie des zuletzt gespeicherten Senders bevorzugt; die Senderliste zeigt nur Sender dieser Kategorie.

## Temporäre QR-Playlistverwaltung

Auf ausdrücklichen Start des Nutzers stellt das Gerät eine zeitlich begrenzte Verwaltungsseite im lokalen Netzwerk bereit. Ein QR-Code öffnet sie auf einem anderen Gerät. Nutzer können Quellen eingeben, ändern oder entfernen und müssen Änderungen bestätigen. Der Dienst endet nach Ablauf, beim expliziten Beenden und beim Verlassen des Verwaltungsablaufs; aktive Verbindungen schließen. Ein zufälliges, kurzlebiges Sitzungsgeheimnis schützt die Sitzung; keine Zugangsdaten im QR-Code oder in Logs. Zugriff nur im lokalen Netz, keine Portfreigabe oder Cloud-Vermittlung. Da lokales HTTP mitlesbar sein kann, sensible Daten nur nach verständlichem Hinweis und möglichst über eine abgesicherte Verbindung übertragen; konkrete sichere Transportlösung vor Implementierung entscheiden. Netzwerkisolierung und nicht erreichbare Geräte verständlich behandeln.

## Optionale Cloud-Synchronisierung

Standardmäßig aus; kein Herstellerkonto erforderlich. Nutzer stellen ihren eigenen unterstützten Speicher bereit. Nur nach Einrichtung und Einwilligung werden ausgewählte Einstellungen, Favoriten, Zuordnungen und optional Quellen/Zugangsdaten geräteübergreifend synchronisiert; keine Wiedergabedaten oder Geheimnisse stillschweigend hochladen. Vor Übertragung auf dem Gerät authentifiziert verschlüsseln; Schlüssel aus einer vom Nutzer gewählten Passphrase mit geeigneter passwortbasierter Schlüsselableitung und zufälligem Salt ableiten. Schlüssel und Passphrase nicht hochladen oder loggen. Nutzer über Verlust der Passphrase und mögliche Metadaten beim Speicheranbieter informieren. Löschung und Deaktivierung klar unterscheiden; Konflikte und Offline-Änderungen nicht stillschweigend überschreiben. Anbieter, Schlüsselparameter und Konfliktoberfläche sind vor Umsetzung festzulegen.

## Updates und Qualität

Unter **Einstellungen → Über StreamWatch** stehen Version, Versionscode und Versionsdatum. Das Versionsdatum wird beim Anlegen eines Releases mitgepflegt.

Unter **Einstellungen → App-Updates** kann die App manuell oder optional beim Start nach einer neueren stabilen Version suchen. Sie zeigt Versionshinweise an und lädt die APK nach Auswahl herunter. Vor der Übergabe an den Android-Paketinstaller prüft sie SHA-256, Paketname, Versionscode und Signatur. Die Installation erfordert die Android-Systembestätigung.

GitHub Actions prüft Änderungen und veröffentlicht bei einem Versions-Tag die signierte APK, die Prüfsumme und die Hinweise aus dem Änderungsprotokoll im öffentlichen Download-Repository. Das Quellrepository kann privat bleiben.

## Abnahme

Mindestens: zwei Quellen importieren, eindeutig zusammengeführten Sender mit zwei Varianten abspielen und Failover testen; EPG passend zuordnen; Kategorien/Länderfilter kombinieren; beim Navigieren Live ohne Wechsel fortsetzen; VOD starten und Position fortsetzen; QR-Sitzung starten, Änderungen bestätigen und Ablauf prüfen; Cloud optional verschlüsselt und konfliktbewusst synchronisieren; CI baut und testet, Release-Artefakt ist installierbar. Touch und D-Pad jeweils auf geeigneten Geräten oder Emulatoren prüfen. Netzwerkfehler, leere Daten und Neustart berücksichtigen.
