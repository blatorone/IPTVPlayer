# Änderungen

## 2.0.1

- Smart Home: Die Knöpfe sprangen beim Drücken in den Bereich „Programmführer“. Der Fokus bleibt jetzt beim Seitenwechsel oben auf der Seite.
- Neu: Autostart (Einstellungen → Allgemein, auch per Handy). StreamWatch startet auf Wunsch beim Einschalten des Geräts und/oder beim Aufwecken aus dem Standby. Dafür braucht die App die Berechtigung „Über anderen Apps einblenden“; auf Android TV (z. B. Shield) führt ein Knopf direkt dorthin, auf Fire TV lässt sie sich einmalig über einen Computer setzen.
- Neu: Smart Home mit Home Assistant (eigener Bereich in den Einstellungen, auch per Handy). Die App sucht Home Assistant im Heimnetz und steuert die gewählten Lampen, Schalter oder Szenen: bei Wiedergabe aus oder gedimmt, bei Pause nach einigen Sekunden an, bei Stopp oder Ende an – für Filme, Serien und/oder Live-TV, immer, zu festen Zeiten oder bei Dunkelheit (Sonnenuntergang aus Home Assistant, mit Vorlauf). Eingeschaltet wird nur, was StreamWatch vorher ausgeschaltet hat. Im Backup ist Smart Home schon zu sehen, kommt aber erst später hinein.
- Smart Home: Für Wiedergabe, Pause, Stopp/Ende und Raucherpause gibt es je eine eigene Seite. Dort stellst du jede Lampe einzeln ein: nicht ändern, aus oder an – dimmbare mit eigener Helligkeit, mit einer Palette für Farbtemperatur und (bei Farblampen) Farbe. Rollos gehen auf oder zu. Pause und Stopp werden immer gesendet, auch wenn das Licht zwischendurch von Hand geschaltet wurde; beide Verzögerungen sind einstellbar (bis 30 s). Beim Sender- oder Folgenwechsel wird die Stopp-Szene nicht ausgelöst. Zusätzlich lassen sich die Wochentage wählen.
- Raucherpause: Im Vollbild erscheint unten ein Zigaretten-Knopf, sobald die Raucherpause eingerichtet ist. Er pausiert und startet ihre Szene (z. B. Rollo auf); bei Play kommt alles genau auf den vorherigen Stand zurück. Gilt für Filme, Serien und Live-TV, unabhängig von Uhrzeit und Wochentag.
- Handy-Seite „Sperren“: aufgeteilt wie am Gerät in Kindersicherung und Einstellungsschutz, jeweils mit eigener PIN (oder derselben). Die Bereiche (Live-TV, Filme, Serien anzeigen) lassen sich auch ohne PIN einstellen. Die PIN des Einstellungsschutzes lässt sich jetzt auch per Handy festlegen, ändern und entfernen.
- Programmführer: Schlug der Abruf beim Portal einmal fehl oder kam leer zurück, blieb das bis zu 6 Stunden so – man musste von Hand aktualisieren. Jetzt wird es nach 10 Minuten erneut versucht, und alle 15 Minuten wird nachgeladen, was veraltet ist.
- Programmführer: hält jetzt einen Tag zurück und einen Tag voraus (bisher 2 Stunden zurück). Beim Nachladen bleiben die schon gelaufenen Sendungen erhalten, alles Ältere fällt heraus – der Speicher wird überschrieben und wächst nicht. „Früher“ blättert bis zu 24 Stunden zurück.
- Handy-Seite: Backups lassen sich auch dort mit Passwort erstellen und geschützte Backups einspielen. Das Passwort geht über die verschlüsselte Verbindung direkt ans Gerät.
- Handy-Seite: PIN-Abfragen sind eine eigene Seite mit großem Ziffernblock statt der Handy-Tastatur; die Leiste unten ist dabei verdeckt.
- PIN ändern per Handy: Nach der aktuellen PIN ging die neue PIN immer als „falsch“ zurück – das Handy prüfte weiter gegen die alte. Jetzt läuft die ganze Änderung über eine Verbindung, und das Handy zeigt jeden Schritt: aktuelle PIN, dann Zahlen oder Text, die Länge (4, 6, 8 oder eigene), neue PIN eingeben und wiederholen; OK speichert.
- Neue App-Sprachen: Lettisch, Litauisch, Ungarisch, Niederländisch, Norwegisch, Schwedisch, Slowakisch und Slowenisch.
- Fließender Portalwechsel: Der Abstand zwischen zwei Portalen wird jetzt pro Sender gelernt und erst verwendet, wenn zwei Messungen übereinstimmen. Das Standbild beim Wechsel dauert höchstens 3 Sekunden (vorher bis zu 9).
- Diagnose: Bricht ein Stream nach dem Wechsel ab, steht dabei, wie lange seine Aufnahme lief und warum sie endete.
- Timeshift und fließender Portalwechsel: Die Aufnahme ruft den Stream jetzt genauso ab wie der Player. Manche Portale hatten die Aufnahme abgelehnt („HTTP 406“), sodass der Sender sofort auf ein anderes Portal sprang. Die Verbindung wird beim Umschalten gleich geschlossen.
- Hakt das Bild während Timeshift (Dekodierfehler), startet die Wiedergabe einmal neu am Live-Rand der Aufnahme, statt das Portal zu wechseln. Ist die Aufnahme selbst beendet, wird sofort gewechselt statt erst neu geladen.
- Serien → „Weiter schauen“: Ist diese Woche eine neue Folge erschienen und schon im Portal, steht die Serie vorne und zeigt „Neue Folgen“. Fehlt die Folge im Portal noch, gibt es keinen Hinweis (die App schaut alle 3 Stunden nach). „Zuletzt gesehen“ bleibt nach dem letzten Anschauen sortiert.
- Serien → „Bald verfügbar“: zeigt jetzt den Start der nächsten Staffel (z. B. „S3 · Do, 04.12.“), nur bei Serien, die du schaust. Das klappt mit Trakt, mit TMDB und ohne beides über TVmaze. Nur ein Hinweis – ob das Portal sie dann gleich hat, ist noch offen.
- Backup: Beim Speichern kann die Datei mit einem Passwort geschützt werden (mind. 8 Zeichen). Dann ist sie wirklich verschlüsselt, und ohne das Passwort lässt sie sich nicht öffnen. Ohne Passwort bleibt alles wie bisher; alte Backups lassen sich weiter einspielen.
- Intro-Erkennung: Der Vorspann wird jetzt in den ersten 10 Minuten gesucht (vorher 5) – für Serien mit langem Einstieg wie Tulsa King.
- Technik: Kotlin 2.4.
- Diagnose: Die Suche nach neuen Folgen vermerkt Funde und Fehler (nur Anzahlen, keine Serientitel).
- Aufgeräumt: alle Warnungen des Compilers und von Android Lint behoben (u. a. Passwortfelder auf die neue sichere Eingabe umgestellt, Fokussteuerung der Einstellungen auf die neue Schnittstelle, ungenutzte Startsymbole entfernt, slowenische Mehrzahl korrigiert).
- Aufgeräumt: über 40 nicht mehr verwendete Texte in allen Sprachen entfernt, dazu ungenutzte Farben und Symbole.
- Sicherheit: Der Hinweis zum Backup sagt jetzt klar, dass die Datei Zugangsdaten enthält und sicher aufbewahrt werden muss (sie lässt sich ohne Passwort auf jedem Gerät öffnen).
- Technik: Player (Media3 1.11), Oberfläche (Compose), Netzwerk (OkHttp 5) und weitere Bibliotheken auf den aktuellen Stand gebracht – mit den Sicherheits- und Fehlerkorrekturen der neuen Versionen.

## 2.0.0

Neue Oberfläche: StreamWatch wurde seit dem Anfang komplett überarbeitet.

Bedienung und Aussehen:
- Einheitliche Knöpfe in der ganzen App: Angewählt wird die Fläche heller und bekommt einen Rahmen in der Akzentfarbe; Ausgewähltes zeigt das Symbol in der Akzentfarbe. Dialogknöpfe sind kleine Karten.
- Neue Wiedergabeleiste wie bei Netflix und Prime: vorherige Folge – zurückspulen – Play/Pause – Stopp – vorspulen – nächste Folge, alle rund im gleichen Stil. Bei Filmen und Timeshift springen die äußeren Knöpfe 10 Minuten. Was nicht geht, wird ausgeblendet; Untertitel nur, wenn der Stream welche hat.
- Zurück aus dem Vollbild führt dorthin, wo der Titel geöffnet wurde – nie in die Einstellungen. Zurück von Film- und Seriendetails landet auf dem zuletzt gewählten Titel.
- Programmführer: durchgehende Zeitlinie mit wählbarer Farbe; die abgelaufene Zeit liegt unter einem Schleier, dessen Art und Stärke einstellbar sind.
- Neue Seite „Neuigkeiten“ unter „Über StreamWatch“.
- Neue App-Sprachen: Tschechisch und Dänisch. Die Handy-Seite hat den App-Stil und alle neuen Einstellungen.

Live-TV:
- Timeshift (optional): Live-TV pausieren und zurückspulen, 15, 30 oder 60 Minuten; „● Live“ springt zurück.
- Puffer (optional) in vier Stufen.
- Zuverlässigerer Portalwechsel: Springt oder hängt ein Stream, wechselt die App sofort; genau diese Variante wird 6 Stunden lang zuletzt versucht. Der Ersatz kommt bevorzugt von einem anderen Portal, ähnlich gute Streams werden gemischt, und bevor aufgegeben wird, gibt es eine zweite Runde.
- Fließender Portalwechsel (experimentell, eigener Schalter): Bei Störungen wird das Ersatzportal schon im Hintergrund aufgenommen, und es geht an derselben Stelle weiter.
- Die Senderinfo erscheint bei jedem Umschalten im Vollbild; die Senderliste bleibt nach Updates erhalten.

Filme und Serien:
- Kategorien wie „Beliebt“ oder „Im Kino“ laden deutlich schneller.
- „Forced“-Untertitel werden zuverlässig gewählt. Ohne Ton oder nur mit fremdsprachigem Ton wird zu einem anderen Portal gewechselt oder ein Hinweis gezeigt.

Updates und Diagnose:
- „Nach Updates suchen“ behält den Fokus; bei einem GitHub-Anfragelimit nennt die App die Wartezeit.
- Der Diagnosebericht nennt den Grund jedes automatischen Portalwechsels.

## 1.0.98

- Behoben: Nach dem Start im Vollbild ließ sich nicht umschalten – die Tasten nutzten noch die beim Start leere Senderliste. Jetzt gilt immer die aktuelle Liste.
- Serbisch (lateinisch): falsche Großschreibung von „Nj“, „Lj“ und „Dž“ mitten im Wort korrigiert.

## 1.0.97

- Filme und Serien: „Mein Bereich“ ersetzt Weiter schauen, Bald verfügbar, Merkliste und Verlauf. Er steht vorne in der Kategorie-Leiste und zeigt Reihen mit Postern: Weiter schauen, Merkliste, (bei Serien) Bald verfügbar und Zuletzt gesehen.
- „Mein Bereich“ hat ein eigenes Symbol; neue Folgen gefolgter Serien stehen als Zahl im Kreis daneben (wie bei „Einstellungen“).
- Handy-Seite: folgt der App-Sprache (17 Sprachen), mit Backup, Entsperren per PIN oder Passwort und den neuen Einstellungen.
- Startseite: Unter Allgemein lässt sich wählen, ob die App mit Live-TV, Filmen oder Serien startet. Filme und Serien öffnen die erste Kategorie; „Letzten Sender beim Start abspielen“ gibt es dann nicht, es wird nichts abgespielt.
- Neu: „Letzten Sender im Vollbild starten“. Die App startet dann sofort im Vollbild (ohne kurz den Guide zu zeigen) und blendet oben links einmal die Senderinfo ein. Ausgeschaltet läuft der Sender im Guide-Vorschaubild.
- Behoben: Nach dem Start im Vollbild ließ sich nicht umschalten (und die Senderinfo zeigte keine Programmdaten), bis man das Vollbild einmal verlassen hatte.
- Sperren: Statt einer PIN aus Ziffern lässt sich auch ein Passwort festlegen („Passwort statt Ziffern“). Ist eine PIN oder ein Passwort gesetzt, muss es nach dem Scannen am Handy eingegeben werden, bevor dort etwas einstellbar ist. Bei gesperrten Einstellungen bietet die PIN-Abfrage „Per Handy einstellen“ – am Fernseher ist dann nur der QR-Code zu sehen, nicht was am Handy geändert wird.
- Neu unter Allgemein: „Eingaben nur per Handy“ (standardmäßig aus). Dann werden PINs, Passwörter, Portale und die Trakt-API nur am Handy per QR-Code eingegeben; die Suche bleibt bei der Tastatur.
- Zurück-Taste: In Filmen und Serien bleibt immer eine Kategorie offen (vorher wurde der untere Bereich schwarz). Zurück führt zur Startseite und von dort aus der App.
- Obere Leiste: Die aktuelle Seite zeigt das Symbol in der Akzentfarbe (ohne Symbole die Schrift) statt des Strichs.
- Live-TV: Die Senderliste steht beim Start deutlich schneller bereit. Die App merkt sich, wie sie die Sendernamen (Land, Qualität, Kategorie) gelesen hat, statt bei jedem Start alle neu zu zerlegen – bei rund 60 000 Sendern dauerte das auf dem Fire TV Stick etwa 45 Sekunden. Nach einem App-Update wird einmal neu gerechnet.
- Diagnose: „Diagnosebericht in ‚Download‘ speichern“ legt den Bericht ohne Ordnerauswahl im Download-Ordner ab – auch auf Fire TV, das keine Ordnerauswahl hat.
- Trakt: Ist schon eine Trakt-API gespeichert (beim Kinderkonto die des Admins), erscheint direkt der QR-Code statt der Eingabefelder; „Trakt-API ändern“ zeigt sie bei Bedarf.
- Einstellungen: Nach rechts in die Zeilen geht es beim ersten Mal zur obersten Zeile, danach zur zuletzt benutzten des Bereichs (vorher sprang der Fokus z. B. bei „Sperren“ nach unten). Nach dem Schließen einer Seite steht der Fokus wieder auf der Zeile, die sie geöffnet hat; neu geöffnete Einstellungen beginnen wieder oben.
- Behoben: Absturz beim Öffnen der Nutzungsbedingungen in den Einstellungen.
- Suche: Bei leerem Suchfeld stehen die letzten Suchen (Standard 10). OK sucht erneut, langes OK entfernt einen Eintrag, „Suchverlauf löschen“ leert die Liste. Unter Allgemein: Suchverlauf speichern (aus löscht ihn), Anzahl der Einträge (5–30) und Suchverlauf löschen. Das Kinderkonto hat einen eigenen Verlauf.
- Backup (Daten & Diagnose, ersetzt „Portale sichern“): „Backup erstellen“ und „Backup importieren“, jeweils mit Auswahl – Alles oder einzeln Portale, Einstellungen, Kategorien und Länder, Sperren/PINs/Kinderkonto, Trakt und TMDB, EPG-Quellen sowie Fortschritt/Verlauf/Merkliste/Favoriten. Die Datei ist verschlüsselt und wird im Download-Ordner gespeichert (oder an einem gewählten Ort, wo das Gerät eine Auswahl hat). Nach dem Import startet die App neu; Portale kommen zu den vorhandenen hinzu. Alte Portal-Dateien lassen sich weiterhin importieren.

## 1.0.96

- App-Updates: Die Markierung für ein neues Update steht wieder als kleine „1“ oben rechts neben „Einstellungen“ (in 1.0.95 ersetzte sie den Text).

## 1.0.95

- Kinderkonto: „Trakt für <Name>“ nutzt die Trakt-Kennung des Admins, auch wenn dessen Konto noch mit der früher eingebauten Kennung verbunden ist; eine eigene API muss dann nicht eingegeben werden.
- App-Updates: Mit „Beim Start automatisch nach Updates suchen“ wird jetzt bei jedem Start und danach alle 6 Stunden gesucht. Eine neue Version zeigt die Markierung an „Einstellungen“; beim Öffnen geht es direkt zum Update.

## 1.0.94

- Kinderkonto: „Trakt für <Name>“ öffnet sich wieder (lag hinter der Kinderschutz-Seite) und erscheint nur, wenn beim Admin ein Trakt-Konto verbunden ist. Das Kinderkonto nutzt dieselbe Trakt-API; ist keine gespeichert, wird sie einmal eingegeben.

## 1.0.93

- Live-TV: Beim Wechsel der Quelle oder des Senders bleibt das letzte Bild stehen, statt schwarz zu werden. Der nächste Stream wird schon vorbereitet, sobald der laufende hakt.
- Live-TV: Hänger werden besser erkannt – ein stehendes Bild (auch wenn nur der Ton weiterläuft) und Streams, die immer wieder ein paar Sekunden zurückspringen, führen zum Wechsel. Gut laufende Streams werden seltener grundlos gewechselt: langes Puffern zählt nur innerhalb von drei Minuten, kurze Verbindungsfehler laden erst denselben Stream neu.
- Live-TV: Die App merkt sich je Sender, welches Portal am besten lief, und startet beim nächsten Mal damit.
- Portale: Hinweis vor dem Ablauf und nach dem Ablauf (je einmal, mit OK oder Zurück bestätigen). In den Quellen sind bald ablaufende Portale gelb, abgelaufene rot markiert. Ab wann der Hinweis kommt, ist unter Quellen einstellbar (1–14 Tage, 1–3 Wochen oder 1 Monat; voreingestellt 1 Woche), auch am Handy.
- Benutzerkonten: Neben dem Admin-Konto kann ein Kinderkonto angelegt werden (Sperren → Kinderschutz). Es sieht nur freigegebene Bereiche und Inhalte passend zur Altersgrenze und kommt nicht in die Einstellungen. „Abmelden“ oben im Menü wechselt das Konto, zum Admin nur mit PIN. Beim Start: Benutzer auswählen, Admin (mit PIN) oder Kinderkonto. Jedes Konto hat seine eigene Farbe; das Kinderkonto kann die Daten des Admins mitnutzen oder eigenen Fortschritt, eigene Merkliste und ein eigenes Trakt-Konto haben.
- Sperren (früher Kindersicherung): Einstellungsschutz mit eigener oder gleicher PIN wie der Kinderschutz – Einstellungen, Portale und Kategorien lassen sich einzeln sperren. Live-TV, Filme und Serien lassen sich ausblenden; ein Bereich bleibt immer.
- Trakt: Zum Verbinden trägt jeder seine eigene Trakt-API (Client ID und Client Secret) ein. Bereits verbundene Konten laufen unverändert weiter.
- Einstellungen: Konten und Synchronisierung enthält jetzt auch die Filmdatenbank (TMDB). Neuer Bereich „Daten & Diagnose“ mit Portal-Sicherung und Diagnosebericht; „Über“ zeigt nur noch App-Infos und die Nutzungsbedingungen. Links aus den Einstellungen wirkt wie Zurück und bleibt im zuletzt offenen Bereich.
- Farben: 40 Akzentfarben zur Auswahl; neben der Beispielansicht wie bisher, die neuen darunter über die ganze Breite.
- Farbverläufe: Zweite Palette mit 40 Farbverläufen (pro Konto wählbar). Fokus-Rahmen, Auswahlbalken und Schalter zeigen den Verlauf.
- App-Sprache: Unter Allgemein → App-Sprache lässt sich die Sprache der Oberfläche wählen (Standard: Systemsprache, sonst Englisch). Verfügbar: Deutsch, Englisch, Türkisch, Polnisch, Russisch, Arabisch, Rumänisch, Kroatisch, Bosnisch, Serbisch (kyrillisch und lateinisch), Spanisch, Französisch, Italienisch, Portugiesisch (Brasilien), Albanisch und Ukrainisch; weitere folgen. Bei Arabisch wird gefragt, ob die Oberfläche gespiegelt werden soll. Neue Installationen fragen beim ersten Start zuerst nach der Sprache, danach kommen die Nutzungsbedingungen in dieser Sprache. Auch Status- und Fehlermeldungen (z. B. beim Trakt-Abgleich) sind übersetzt; die Handy-Seite ist noch deutsch.
- Kinderschutz: Erotik- und Kinderkategorien werden auch in Portal-Listen anderer Sprachen erkannt (z. B. Türkisch, Polnisch, Russisch, Arabisch).
- PIN-Eingabe: Die Zifferntasten der Fernbedienung tippen die PIN direkt ein. Neu: „Mit dem Handy eingeben“ zeigt einen QR-Code – die PIN wird dann verdeckt am Handy getippt und verschlüsselt übertragen, am Fernseher sieht sie niemand.
- App-Updates: Die Änderungen im Update-Hinweis lassen sich mit der Fernbedienung durchscrollen (vom Knopf nach oben). Ist ein neues Update da, öffnen sich die Einstellungen einmal direkt mit dem Update-Fenster.
- Download: Die APK heißt jetzt StreamWatch-<Nummer>.apk. Übergangsweise liegt sie zusätzlich unter dem alten Namen bei, damit ältere Versionen das Update finden.
- Neue Installationen zeigen beim ersten Start die Nutzungsbedingungen, die akzeptiert werden müssen.

## 1.0.92

- Quellen: Neben jedem Portal zeigt „Kategorien“ die Kategorien so, wie der Anbieter sie liefert – für Live-TV, Filme und Serien, mit erkanntem Land und Anzahl, oben zusammengefasst nach Ländern. So sieht man vor der Gruppierung, welche Länder ein Portal enthält. Auch am Handy bei den Portalen.

## 1.0.91

- Portale: Der Schalter zum Aktivieren/Deaktivieren sitzt jetzt im Rahmen des Portals; „Bevorzugen“ hat rechts daneben einen eigenen Rahmen.
- Filme und Serien: Beim automatischen Wechsel auf ein anderes Portal werden nur Versionen aus den gewählten Ländern genommen. Hat ein Stream nur Ton in einer Sprache, die zu keinem gewählten Land passt (z. B. Türkisch bei Deutschland), wird das nächste Portal versucht.
- App-Updates: Neuer Schalter „Beta-Versionen erhalten“ (standardmäßig aus, auch am Handy). Betas sind Vorabversionen zum Ausprobieren; ohne den Schalter werden wie bisher nur fertige Versionen angeboten.

## 1.0.90

- Bevorzugtes Portal: In der Portalliste (und am Handy) lässt sich ein Portal mit „Bevorzugen“ markieren. Es wird für Live-TV, Filme und Serien zuerst verwendet; klappt es nicht, werden die anderen Portale automatisch versucht.
- Filme und Serien: Neben „Gesehen-Status“ gibt es den Knopf „Portal“, wenn der Titel auf mehreren Portalen liegt. Ein Film startet direkt über das gewählte Portal; bei einer Serie gilt die Wahl für die Folgen. Dort lässt sich das Portal auch bevorzugen.
- Vollbild: Das Zahnrad öffnet ein Menü mit Geschwindigkeit, Tonspur und Portal. So lässt sich laufende Wiedergabe auf ein anderes Portal umschalten; Filme und Folgen machen an derselben Stelle weiter.
- Kein Ton: Hat ein Stream keinen abspielbaren Ton, startet die App ihn automatisch über das nächste Portal. Gibt es keines mehr, erscheint ein Hinweis.
- Untertitel: Forced-Untertitel werden jetzt auch bei Filmen und Live-TV automatisch eingeschaltet (vorher nur bei Serien). Erkannt werden auch Spuren, die nur „Forced“ bzw. „erzwungen“ heißen. Die Untertitel-Taste gibt es jetzt auch bei Live-TV.
- Zurück-Schaltflächen: Alle Zurück-Knöpfe auf dem Bildschirm (auch in der Info im Vollbild und im Programmführer) folgen der Einstellung „Zurück-Schaltfläche auf dem Bildschirm anzeigen“.
- Live-TV: „Wiedergabe stoppen“ im Programmführer entfällt.
- Behoben: Zurück aus „Quellen“ oder „Kategorien“ (und in den Einstellungen) konnte die App beenden, weil die App sich fälschlich noch im Programmführer wähnte.
- Einstellungen: Zurück geht immer einen Schritt zurück – aus einer Unterseite zu den Zeilen der Rubrik, aus den Zeilen zur Rubrikenleiste und von dort zum zuletzt genutzten Bereich (Live-TV, Filme, Serien).
- App beenden: Neue Einstellung unter Allgemein (auch am Handy). Standard ist eine Abfrage, die man bestätigen muss; wahlweise zweimal Zurück drücken oder ohne Bestätigung.
- Farben: 20 Akzentfarben statt 5. Unter Darstellung → Akzentfarbe gibt es eine Farbpalette mit einer Beispielansicht daneben; sie zeigt die Farbe, auf der der Fokus steht, OK übernimmt sie. Auf hellen Farben (z. B. Gelb) ist die Schrift dunkel.
- Handy-Seite: zeigt das StreamWatch-Logo und übernimmt die in der App eingestellte Akzentfarbe; die Farben lassen sich dort als Farbfelder wählen.
- Per Handy einstellen: Jetzt mit allen Einstellungen des Geräts – EPG-Quelle und eigene XMLTV-Quellen, EPG jetzt aktualisieren, Programmführer und Senderliste gestalten, Sendervarianten innerhalb des Portals zusammenführen, alle Portale aktualisieren, App-Updates, TMDB-Token, Trakt verbinden/synchronisieren/trennen und bevorzugtes Portal. Intro- und Abspannzeiten erscheinen nur bei „Feste Zeit“, die Nachfrage-Anzahl nur bei eingeschalteter Nachfrage. Nur am Gerät bleiben Diagnose sowie Portal-Export und -Import, weil sie Dateien auf dem Gerät brauchen.

## 1.0.89

- Neu: Per Handy einstellen (Einstellungen → Allgemein). Das Gerät zeigt einen QR-Code; damit lassen sich im Handy-Browser Portale hinzufügen, aktivieren und entfernen, Länder und Kategorien für Live-TV, Filme und Serien ein- und ausblenden, Kategorien sperren, PIN und Kindersicherung sowie die übrigen Einstellungen ändern – im Heimnetz und unterwegs. Die Verbindung ist Ende-zu-Ende verschlüsselt und endet mit „Zugriff beenden“, beim Verlassen der Seite oder App und nach 15 Minuten ohne Änderung.
- Portale: Der Schalter direkt an der Portalzeile aktiviert bzw. deaktiviert das Portal; die eigene Zeile dafür entfällt.
- Ohne Portal: Beim Einrichten (Startdialog und Portal-Eingabe) gibt es „Per Handy einrichten“. Nach dem Laden fragt das Handy nach den Ländern; mit „Übernehmen“ wird die Auswahl ans Gerät gesendet. Länder werden am Handy erst gesammelt und dann gemeinsam übernommen.
- Am Handy lassen sich vorhandene Portale bearbeiten (Adresse, MAC) und „Quellen zusammenführen“ direkt bei den Portalen ein- und ausschalten.
- Während der Zugriff per Handy läuft, zeigt das Gerät nur QR-Code und Verbindungsstatus; was am Handy eingestellt wird und Fragen wie die Länderauswahl erscheinen nur am Handy. Mit „Fertig – Zugriff beenden“ am Handy endet die Sitzung und das Gerät wechselt zu Live-TV. Die Sitzung endet sonst nur durch Beenden am Gerät, beim Verlassen der App oder nach 15 Minuten ohne Aktivität.

## 1.0.88

- Behoben: Einstellungs-Unterseiten (z. B. „Tasten und Gesten belegen“, EPG, Folgen, TMDB, Programmführer gestalten) schlossen sich beim Öffnen sofort wieder und die Einstellungen sprangen zu „Allgemein“. Die Unterseite öffnet sich jetzt zuverlässig und bekommt den Fokus.

## 1.0.87

- Einstellungen: Quellen, Kategorien, App-Updates, Trakt und Info im neuen Stil (Zeilen mit Schaltern/Werten). Quellen und Kategorien erscheinen rechts neben der Rubrikenleiste. Das Änderungs-Popup bei Updates bleibt.
- Zurück aus den Einstellungen führt immer zum zuletzt genutzten Bereich (Live-TV, Filme, Serien); schnelles mehrfaches Zurück beendet die App dabei nicht mehr.
- PIN-Tastatur: Ziffern mittig in den Tasten. Beim Festlegen wird die Länge gewählt (4, 6, 8 oder benutzerdefiniert, mindestens 4); beim Eingeben wird nach der festgelegten Länge automatisch bestätigt.
- Programmführer: Das Play-Symbol des laufenden Senders steht rechts am Rand.
- App-Updates: Die automatische Suche nach Updates ist standardmäßig eingeschaltet (nur suchen, installiert wird erst nach Bestätigung; abschaltbar). Neben „Nach Updates suchen“ steht die letzte Prüfung mit Datum und Uhrzeit.

## 1.0.86

- Behoben: 1.0.85 ließ sich auf Android 11 (z. B. NVIDIA Shield) nicht starten („VerifyError“). Der Hauptbildschirm übergab seine sehr vielen Zustände als Parameter an einzelne Funktionen; ab einer Größe lehnt Android den erzeugten Code ab. Diese Werte liegen jetzt in Feldern (Compiler-Einstellung für Lambdas); der Start wurde auf der Shield mit einem Release-Build geprüft.

## 1.0.85

- Kindersicherung (neue Rubrik in den Einstellungen): PIN festlegen, Einstellungen per PIN schützen, Erotik-Kategorien automatisch sperren, einzelne Kategorien über „Kategorien verwalten“ sperren, gesperrte Inhalte bis zum Beenden der App freigeben.
- Kindermodus mit Altersstufe (FSK 0/6/12/16/18): nur Kinder- und Familienkategorien (ab FSK 16 alle außer Erotik), Erotik immer ausgeblendet. Filme und Serien werden beim Öffnen anhand der Altersfreigabe (TMDB, sonst Trakt) geprüft; unbekannte Freigabe bleibt gesperrt und lässt sich per PIN einmalig freigeben. Beenden nur mit PIN – wahlweise eigene Kindermodus-PIN oder dieselbe wie für die Einstellungen.
- Filme und Serien zeigen in den Details die Altersfreigabe (z. B. „FSK 12“).
- StreamWatch-Logo (Play-Symbol mit Schriftzug, transparenter Hintergrund) statt reinem Text in der oberen Leiste und im Menü.
- Programmführer: Der laufende Sender bekommt zusätzlich zur Umrandung ein kleines Play-Symbol in Schriftfarbe.
- Einstellungen: Unterseiten (Programmführer gestalten, Senderliste, EPG, Folgen, Steuerung, TMDB) öffnen sich nicht mehr als Popup, sondern ersetzen rechts die Zeilen der Rubrik – im selben Stil mit Schaltern und Werten. Zurück führt erst zur Rubrik, dann zum zuletzt genutzten Bereich (Live-TV, Filme oder Serien).
- Besetzung: Filme aus der Filmografie eines Schauspielers öffnen sich jetzt (vorher sprang die App zurück). Zurück geht immer genau einen Schritt: Titel → Filmografie → Besetzung → ursprünglicher Titel.
- Filmografie mit denselben Filtern wie die Film- und Serienlisten: Sortierung, Merkliste und Jahre.
- Vollbild Live-TV: Mit „nach unten“ erreicht man aus den Wiedergabeknöpfen wieder das Einstellungs-Zahnrad (die Zeitleiste ist bei Live gesperrt und blockierte den Weg).
- Programmführer: Zeitleiste zeigt Wochentag, Uhrzeit und Datum mit Jahr (z. B. „Mi., 03:12 · 25.11.2026“).

## 1.0.84

- Vollbild: Das Vollbild öffnete sich seit 1.0.82 nicht mehr (Start, langes Zurück, laufenden Sender wählen), weil der normale Bildschirm darübergezeichnet wurde. Behoben.
- Besetzung: Trakt-Suchen per IMDb-/TMDB-Kennung nutzten ein falsches Format und fanden nichts. Mit Trakt-Konto fragt die Besetzung jetzt zusätzlich direkt TMDB. Damit erscheint „Besetzung“ wieder bei Filmen und Serien.
- Programmführer: Überlappende oder doppelte Sendungen der Portale lagen im Raster übereinander. Die Sendungen werden jetzt bereinigt (doppelte Einträge zusammengefasst, jede Sendung endet spätestens beim Beginn der nächsten); Vorschau und Raster zeigen dieselben Daten.
- Programmführer im TiviMate-Stil: Überschrift entfällt; Kategorie, Statusmeldungen und Früher/Jetzt/Später/Nur vorgemerkte stehen gemeinsam in einer festen Zeile, der Ladebalken in einem festen Streifen darüber. Kleinere Vorschau ohne Rahmen, größerer Titel mit Restlaufzeit und längerer Beschreibung, Datum in der Zeitleiste und flachere Zeilen – deutlich mehr Sender sichtbar, nichts verrutscht beim Laden.
- Einstellungen neu aufgebaut: Rubriken mit Symbolen links, Inhalte rechts als Zeilen mit An/Aus-Schalter oder Wertanzeige. Beim Öffnen liegt der Fokus auf der ersten Rubrik; die doppelte Rubrikenliste entfällt, Zurück verlässt die Einstellungen.
- „Bald verfügbar“ erscheint bei Serien nur noch, wenn es tatsächlich angekündigte Folgen gibt.
- Alle Film- und Seriengenres haben ein eigenes farbiges Symbol (Action/Abenteuer, Animation/Anime, Komödie, Horror, Krimi, Doku, Familie, Romantik, Drama, Fantasy, Neu, Beliebt, Kino u. a.).

## 1.0.83

- Behebt, dass das Vollbild nach dem ersten Mal nicht mehr öffnete (langes Zurück, Auswahl des laufenden Senders): Die seit 1.0.81 ausgelagerte Vollbildsteuerung wird wieder bei jeder Aktualisierung der Oberfläche neu aufgebaut.
- App-Updates: Die automatische Suche läuft bei jedem Start (höchstens stündlich) und öffnet kein Fenster mehr. Ein verfügbares Update zeigt einen Hinweis „1“ bei Einstellungen und App-Updates; „Update anzeigen“ öffnet das Update-Fenster.
- Kinokategorien nach Jahr verwenden das ursprüngliche Erscheinungsjahr: Wiederaufführungen (etwa „Zurück in die Zukunft“ 2025) zählen nicht mehr. Die Liste lädt automatisch weitere TMDB-Seiten, bis genug Titel in den Portalen gefunden sind. Der Hinweis „Katalogdaten von TMDB“ über der Kategorie entfällt.
- Gesehen-Status von Filmen: Der Trakt-Abgleich lädt alle gesehenen Filme statt nur der letzten zehn; Filme gelten als gesehen, wenn Trakt sie kennt (über Titel, Jahr, IMDb- oder TMDB-ID), sofern sie nicht in der App als ungesehen markiert wurden. Der Filmverlauf zeigt weiter die letzten zehn.
- Besetzung: Die Abfrage wartet kurz auf die TMDB-Zuordnung und fällt sonst auf Trakt zurück, damit „Besetzung“ zuverlässig erscheint. Schauspielerseiten prüfen Titel, die im gespeicherten Katalog fehlen, gezielt im Portal, sodass vorhandene Filme und Serien gefunden werden.

## 1.0.82

- „Besetzung“ erscheint nur noch, wenn TMDB oder Trakt für den Titel Schauspieler kennen. Die Besetzung wird beim Öffnen der Details im Hintergrund abgefragt und danach ohne erneutes Laden angezeigt; ohne TMDB- und Trakt-Zugang gibt es den Punkt nicht.

## 1.0.81

- Besetzung: Film- und Seriendetails haben einen Punkt „Besetzung“ mit Schauspielern, Fotos und Rollen. Ein Schauspieler zeigt seine Filme und Serien getrennt in zwei Reitern, aber nur, was in den Portalen vorhanden ist. Daten kommen von TMDB, ohne TMDB-Zugang von Trakt.
- Jahresfilter: Überall gleich mit „Alle Jahre“, einzelnen Jahren und „Benutzerdefiniert …“ mit Von- und Bis-Auswahl, in Kategorien wie auf Schauspielerseiten.
- Live-TV, Filme und Serien erscheinen im Hauptmenü nur noch, wenn ein aktives Portal dafür Inhalte hat.
- Interner Umbau: Vollbild-Steuerung und Besetzung liegen in eigenen Funktionen, weil die Hauptansicht die Größengrenze einer JVM-Methode erreicht hatte.

## 1.0.80

- Portaltitel der Form „Name - 2026“ verlieren den übrig gebliebenen Bindestrich. Dadurch werden sie wieder Trakt und TMDB zugeordnet, etwa für den Gesehen-Status („Spider-Man: Brand New Day“).
- Filme haben einen eigenen Bereich „Weiter schauen“ mit angefangenen, nicht abgeschlossenen Filmen. Der Filmverlauf speichert dafür bis zu 200 Einträge; angezeigt werden dort weiterhin die letzten zehn.
- Neue Folgen einer abgeschlossenen Serie bringen sie zurück in „Weiter schauen“ (mit „Neue Folgen“). Findet die regelmäßige Folgenprüfung neue Folgen gesehener oder gemerkter Serien, erscheint die Meldung „Neue Folgen verfügbar“.
- Neuer Serienbereich „Bald verfügbar“: gesehene und gemerkte Serien mit angekündigter nächster Folge, nach Ausstrahlungsdatum sortiert, mit Datum und Folgentitel.
- Favoriten und Merkliste sind zusammengelegt. Die Taste und der Filter „Favoriten“ entfallen; bisherige Film- und Serienfavoriten werden einmalig in die Merkliste übernommen und bei verbundenem Trakt-Konto mit der Trakt-Merkliste abgeglichen.
- Die Suche vergleicht Wörter statt der genauen Zeichenfolge: Groß- und Kleinschreibung, Bindestriche, Doppelpunkte und Satzzeichen spielen keine Rolle mehr („spider-man brand new“ findet „Spider-Man: Brand New Day“). Portale werden nach dem längsten Suchwort gefragt und die Treffer lokal auf alle Wörter eingegrenzt; lokale Treffer bleiben auch erhalten, wenn ein Portal nichts liefert.

## 1.0.79

- Abspann mit Zusatzszene: Die App lernt den Abspann einer Serie aus den letzten fünf Minuten zweier Folgen. Folgt auf den Abspann noch weiteres Material (etwa bei Rick and Morty), erscheint während des Abspanns „Abspann überspringen“; „Nächste Folge“ kommt erst kurz vor dem tatsächlichen Ende mit der eingestellten Vorlaufzeit. Ohne Zusatzszene bleibt es bei „Nächste Folge“ zu Beginn des Abspanns. Zurück blendet den Hinweis aus; übersprungen wird nie automatisch.
- Tastentöne: Die Fernbedienungsnavigation spielt die Navigations- und Bestätigungstöne des Geräts, sofern dessen Systemtöne eingeschaltet sind. Im reinen Vollbild bleibt es still. Abschaltbar unter Einstellungen → Allgemein.
- „Dolby-Ton für Intro-Erkennung selbst dekodieren“ führte zu fehlendem Ton. Der normale Audioausgang des Geräts bleibt jetzt erhalten; die Einstellung greift nur noch, wenn das Gerät einen passenden Decoder besitzt (die NVIDIA Shield hat keinen Dolby-Decoder für Apps und gibt Dolby weiter durch).
- Folgenbeschreibungen, Folgenbilder und Serienbeschreibungen von Trakt erscheinen auch bei lokalisierten Portaltiteln (etwa „Die Simpsons“ zu „The Simpsons“): Die Suche nutzt bekannte IDs aus dem Verlauf und sonst den besten Treffer mit gleichem Erscheinungsjahr. Der Portaltitel bleibt sichtbar.
- Poster und Folgenbilder laden flüssiger: Der Bildspeicher richtet sich nach dem Gerät, Poster belegen nur noch halb so viel Speicher.

## 1.0.78

- Fällt ein laufender Sender aus und die App wechselt zur nächsten Variante, bleibt die aktuelle Ansicht erhalten. Der Programmführer springt dabei nicht mehr ins Vollbild; nur ein noch nicht gestarteter Sender oder Film öffnet wie gewählt das Vollbild.
- Sendergruppen und Kategorien werden schneller aufgebaut: Länder- und Sendernamenserkennung merken sich ihre Ergebnisse, sodass wiederkehrende Kategorienamen und gleiche Sender mehrerer Portale nur einmal ausgewertet werden. Die Diagnose misst die Dauer des Gruppierens.

## 1.0.77

- Behebt „App reagiert nicht“ kurz nach dem Start: Die Ländererkennung aller Sender läuft im Hintergrund statt beim Zeichnen der Oberfläche, und Katalogindizes werden nur einmal statt parallel mehrfach aufgebaut. Der Trakt-Abgleich verwendet je Portal einen eigenen, einmal erstellten Index.
- Eine Serie kann mehrere gelernte Intros haben (bis zu vier), etwa eine lange und eine kurze Fassung oder ein neues Intro einer späteren Staffel. Solange mehrere Fassungen möglich sind, springt „Intro überspringen“ nur bis zum frühesten möglichen Ende und rückt nach, sobald die laufende Fassung feststeht; verlässt die Folge ein erkanntes Intro, wird der Hinweis zurückgezogen. Ein zu kurz gelerntes Intro wird beim nächsten Folgenvergleich korrigiert.
- Zurück blendet „Intro überspringen“ aus; danach spulen Links und Rechts wieder. Ausgeblendet erscheint der Hinweis erst in der nächsten Folge wieder; nach dem Überspringen nur dann, wenn sich das Intro als deutlich länger herausstellt.
- Neue Einstellung „Dolby-Ton für Intro-Erkennung selbst dekodieren“ (standardmäßig aus): Durchgeleiteter Dolby-Ton liefert der App keine Tondaten. Ist die Einstellung an, dekodiert die App den Ton, damit auch die Titelmusik verglichen werden kann; über ARC oder optisch kommt dann unter Umständen nur Stereo an. Wirkt nach einem Neustart.

## 1.0.76

- Der Diagnosebericht wird nach jeder Intro-Suche und beim App-Start zusätzlich im eigenen App-Ordner `Android/data/com.iptvplayer.app/files` abgelegt. Eine Ordnerauswahl ist dafür nicht nötig, was auf TV-Geräten oft nicht möglich ist.

## 1.0.75

- Der Diagnosebericht enthält Messwerte der Intro-Erkennung: Anzahl erfasster Bilder und Tonmuster, die von Bild- und Tonvergleich erkannten Zeitbereiche und den Zustand der Tonanalyse. Titel, Adressen und Inhalte werden nicht aufgezeichnet.

## 1.0.74

- Schnellerer Start mit deutlich weniger Speicher: Die gespeicherte Portalbibliothek wird blockweise ver- und entschlüsselt und als Datenstrom gelesen, statt als Ganzes über den Android-Keystore und als vollständiges JSON-Dokument im Speicher. Das verringert Abstürze auf Geräten mit wenig Arbeitsspeicher wie dem Fire TV Stick.
- Die vorhandene Bibliothek wird beim ersten Start automatisch übernommen; die bisherige Datei bleibt bis zum nächsten erfolgreichen Start als Sicherung erhalten. Der Programm-Cache verwendet dasselbe Format.
- Der Trakt-Abgleich und die Titelzuordnung durchsuchen große Kataloge über einen Index statt Titel für Titel. Merklisteneinträge werden parallel in den Portalen gesucht.
- Bei verbundenem Trakt-Konto zeigen Folgen auch Trakt-Folgenbilder, wenn TMDB keines liefert.

## 1.0.73

- Die Intro-Erkennung beendet ein Intro erst nach mehreren abweichenden Sekunden, damit schnelle Schnitte es nicht vorzeitig abschließen. Bild- und Tonergebnisse werden zusammengeführt. Ein zu kurz gelerntes Intro wird beim nächsten Folgenvergleich automatisch verlängert.
- Bei zusammengeführten Portalen gilt die Länderauswahl jetzt auch für Film- und Seriengenres sowie für die Fassungen eines Titels. Fassungen aus Kategorien anderer Länder, etwa mit englischem Ton oder niederländischen Untertiteln, werden nicht mehr gewählt.
- In den Seriendetails ist Staffel 1 wieder mit Links erreichbar, ohne dass sich die Detailansicht schließt. Dasselbe gilt für die erste Aktion der Detailansicht und die Programmzeitleiste.
- Serien-, Film- und Folgenbeschreibungen verwenden bei verbundenem Trakt-Konto die deutsche Trakt-Übersetzung.

## 1.0.72

- Die Intro-Erkennung vergleicht zusätzlich die Titelmusik. Ein Audio-Fingerabdruck erkennt das Intro auch bei wechselnden Bildern, eingeblendetem Text oder fehlender Bildaufnahme.
- Der Bildvergleich toleriert fehlende Stichproben, Pufferpausen und zeitlichen Versatz zwischen Folgen. Schwarzbilder und Standbilder gelten nicht mehr als Treffer; Fingerabdrücke werden aus Flächenmittelwerten berechnet.
- Ein gelerntes Intro wird bereits nach wenigen Sekunden bestätigt, auch wenn einzelne Bilder abweichen. Die Überspringen-Schaltfläche erscheint dadurch zuverlässig, solange das Intro läuft.
- Intros nach einem längeren Vorspann werden bis zur fünften Minute gesucht.
- Intro-Fingerabdrücke liegen begrenzt in einem eigenen lokalen Speicher außerhalb der Android-Sicherung. Ältere, nicht mehr kompatible Einträge werden entfernt und automatisch neu gelernt; dazu sind wieder zwei angesehene Folgen einer Serie nötig.

## 1.0.71

- Trakt episode summaries now fill missing German TMDB or portal descriptions.
- TMDB still images take priority over generic portal episode posters.

## 1.0.70

- Intro skipping no longer seeks backwards when image matching finishes late.
- Player D-pad order follows the visible settings and subtitle button positions.
- Audio and forced subtitles prefer the device language; non-German overview text is filtered.
- TMDB episode descriptions and still images are loaded for each available season.

## 1.0.69

- Die automatische Intro-Erkennung gleicht Bildfolgen auch bei moderaten Kompressionsabweichungen ab und richtet gelernte Intros zeitlich zuverlässiger aus.

## 1.0.68

- Manuelle Gesehen- und Ungesehen-Markierungen werden an ein verbundenes Trakt-Konto übertragen.
- Der Trakt-Folgenimport übernimmt auch nachträglich markierte Folgen anhand des vollständigen Gesehen-Stands.

## 1.0.67

- Filme, Folgen, Staffeln und Serien lassen sich lokal als gesehen oder ungesehen markieren.
- Nach dem Unterbrechen einer Wiederholung zeigt der Fortschrittsbalken die aktuelle Wiedergabeposition.
- Beim Trakt-Folgenimport erfolgt die Titelzuordnung anhand des gespeicherten Katalogs.

## 1.0.66

- Die Bereinigung des Trakt-Serienverlaufs importiert bereits abgeglichene Daten nicht erneut und zeigt den lokalen Abgleich separat an.

## 1.0.65

- Der Trakt-Abgleich blendet entfernte Serien lokal im Serienverlauf und unter „Weiter schauen“ aus. Gespeicherte Fortschrittsdaten bleiben erhalten.

## 1.0.64

- Im Programmführer hat die Navigation mit der Rechts-Taste in der EPG-Zeitleiste Vorrang vor der allgemeinen Fernbedienungsbelegung.

## 1.0.63

- Behebt einen Absturz beim Öffnen des Players, indem die Untertitel-Schaltfläche in der Media3-Buttonleiste neben den Einstellungen eingefügt wird.

## 1.0.62

- Trakt prüft nach dem ersten Abgleich nur geänderte Bereiche. Neue gesehene Episoden werden seit dem gespeicherten Verlaufscursor importiert; Wiedergabestände werden nur bei geänderter Aktivität neu geladen.
- Outro-Marken aus der Bildanalyse werden pro Folge gespeichert. Die HLS-Prüfung umfasst bis zu acht Minuten vor Ende; stabile Abspannbilder werden auch erkannt, wenn danach ein helles Schlussbild folgt.
- Die Intro-Schaltfläche bleibt nach einer knapp verspäteten Erkennung noch kurz verfügbar.

## 1.0.61

- Die Untertitelauswahl sitzt neben den Player-Einstellungen und verwendet dieselbe Fokusumrandung wie andere Aktionen. Allgemein bleiben Untertitel standardmäßig aus; erzwungene Spuren lassen sich automatisch wählen. Pro Serie kann eine einmalige oder dauerhafte Auswahl gespeichert werden.
- Stalker-EPG fragt das eingestellte Zeitfenster über `get_epg_info` ab, statt pro Sender nur maximal 100 kurze Programme anzufordern. Nicht unterstützende Portale behalten den Kurz-EPG als Rückfallweg.
- Die Startberechnung der zusammengeführten Quellenzahlen läuft erst beim Öffnen der Quellen-Seite und konkurriert nicht mehr mit dem Wiederherstellen des gespeicherten Live-TV-Katalogs.
- Portalprüfungen beginnen parallel zum Aufbau der gespeicherten Senderansicht.
- Unabhängige Trakt-Bibliotheksbereiche werden mit begrenzter Parallelität geladen. Die Fortschrittsanzeige benennt Wiedergabestände für Filme und Folgen getrennt.

## 1.0.60

- Die Startberechnung der zusammengeführten Quellenzahlen läuft erst beim Öffnen der Quellen-Seite und konkurriert nicht mehr mit dem Wiederherstellen des gespeicherten Live-TV-Katalogs.
- Portalprüfungen beginnen parallel zum Aufbau der gespeicherten Senderansicht.
- Unabhängige Trakt-Bibliotheksbereiche werden mit begrenzter Parallelität geladen. Die Fortschrittsanzeige benennt Wiedergabestände für Filme und Folgen getrennt.

## 1.0.59

- D-Pad-Rechts vom fokussierten Sender setzt den Fokus im Programmführer direkt auf den aktuellen Zeitleisteneintrag; die Zeitleiste bleibt anschließend mit Links/Rechts navigierbar.
- Im Live-Vollbild führt D-Pad-Runter von der Zeitachse gezielt zu den sichtbaren unteren Player-Einstellungen. Die Einstellungen bleiben erreichbar, auch wenn Media3 sie zunächst als nicht fokussierbar markiert.
- Der periodische Anbieter-EPG-Abgleich fragt nur sichtbare, ausgewählte, laufende und angeheftete Sender ab. Ein alter Cacheeintrag allein löst keinen erneuten Abruf für alle zuvor besuchten Sender aus.
- Portal-, Stream- und EPG-Snapshots sowie Trakt-Zugangstoken bleiben mit AES-GCM und Android-Keystore geschützt. Die vorhandenen Dateien und Datenbankinhalte werden in diesem Release nicht migriert oder gelöscht.

## 1.0.58

- Beim Starten oder Fortsetzen von Filmen und Serien bleibt die Detailansicht sichtbar, bis Media3 die Wiedergabe tatsächlich startet. Fehler beim Laden führen nicht mehr direkt in einen leeren Vollbildschirm; automatische Folgenwechsel bleiben im Vollbild.
- Der Vollbildplayer bietet für Filme und Serien eine fokussierbare Untertitelauswahl mit erkannter Spur und der Option „Aus“.

## 1.0.57

- Wiederholte Trakt-Verläufe verwenden für bereits verknüpfte Serien den lokal gespeicherten Serien- und Portalbezug, statt beim erneuten Abgleich wieder eine Portalsuche für jede Serie auszulösen.
- Bereits importierte Folgen werden nicht erneut als lokal neu gesehen markiert. Der Fortschritt zeigt jetzt ausdrücklich an, wenn die empfangene Trakt-Historie mit gespeicherten Serien abgeglichen wird.

## 1.0.56

- Einstellungen kehren nach Zurück zum zuvor genutzten Bereich zurück, statt die alte Seitenliste und anschließend den Programmführer zu öffnen. Zurück arbeitet in Film-, Serien- und TV-Unterseiten schrittweise; erst auf der Bereichsübersicht erscheint die Beenden-Bestätigung.
- Laufende Sender sind im Programmführer und in der Vollbild-Senderliste nur mit einer Akzent-Umrandung markiert. Die Flächenhervorhebung bleibt dem aktuellen Fokus vorbehalten.

## 1.0.55

- Trakt verarbeitet importierte Episoden gebündelt pro Serie. Bei einer geänderten Trakt-Aktivität werden die Folgen weiterhin für geräteübergreifende Synchronisierung abgefragt, bereits zusammengehörige Episoden lösen aber nicht mehr für jede einzelne Folge erneut Portalabgleich und Serienzusammenführung aus.

## 1.0.54

- Parallele Portalimporte sichern ihre Ergebnisse und Fortschrittsmeldungen jetzt synchronisiert. Der Abschluss eines Live-, Film- oder Serienimports kann dadurch keine gleichzeitig aktualisierten Bereiche mehr aus dem gespeicherten Portalstand verdrängen.

## 1.0.53

- Automatische vollständige Portalabgleiche beim Start werden pro Quelle auf höchstens einen Abruf in sechs Stunden begrenzt. Dazwischen bleibt der Katalogcache direkt nutzbar und es wird nur der Portalzugang geprüft; eine manuelle Aktualisierung bleibt sofort möglich.
- Die Live-TV-Gruppierung verarbeitet nach der Ländererkennung nur die ausgewählten Länder. Verfügbare Länder werden weiterhin aus dem vollständigen lokalen Quellbestand ermittelt.
- Die Quellen- und Architekturhinweise erklären, dass Stalker-Portale Länder üblicherweise nicht als Serverfilter unterstützen; der Länderfilter spart daher lokale Gruppierungsarbeit, nicht den Download einer vollständigen Senderantwort.

## 1.0.52

- Media3-Steuerleiste verknüpft Zeitstrahl, Untertitel- und Einstellungsaktionen mit expliziten D-Pad-Fokuswegen. Der Untertitelknopf öffnet die verfügbare Spurauswahl einschließlich „Aus“.
- Vor- und Zurückspringen bei Filmen und Serien zeigt den Player-Zeitstrahl und fokussiert den Suchlauf. Suchpunkt, Fortschrittsleiste und Fernbedienungsfokus verwenden die gewählte Akzentfarbe.
- Der Start einer Wiedergabe öffnet den Vollbildplayer unmittelbar. Ein bereits laufender Startversuch kann über „Fortsetzen“ wieder in den Vollbildmodus geholt werden, statt den Aufruf ohne sichtbare Aktion zu beenden.

## 1.0.51

- Quellenverwaltung trennt „Alle Quellen aktualisieren“ von einer Mehrfachauswahl einzelner Quellen; einzelne Portalzeilen haben keine eigenen Aktualisieren-Schaltflächen mehr. Manuelle Aktualisierungen werden während einer Startprüfung eingereiht statt verworfen.
- Gespeicherte Portale werden sofort angezeigt und können während der Hintergrundprüfung weiter bedient werden. Der optionale vollständige Startabgleich läuft im Hintergrund; unveränderte Kataloge lösen keine erneute Gruppierung aus.
- Der Programmführer verbindet Rechts vom fokussierten Sender explizit mit dem aktuellen EPG-Zeitstrahl. Im Wiedergabemenü erscheint auch bei Live-TV Sender, laufender Sendung, Uhrzeit und Kurzbeschreibung.
- Einstellungen öffnen die Rubrikenliste mit Fokus auf der ersten Rubrik. Auf TV-Geräten ist die sichtbare Zurück-Schaltfläche standardmäßig ausgeblendet und kann in den Einstellungen zugeschaltet werden; die Zurück-Taste navigiert weiterhin schrittweise.
- Film- und Seriendetails setzen den Fokus auf die primäre Wiedergabeaktion: Fortsetzen, Film starten oder erste Folge starten.

## 1.0.50

- Neue Portalquellen werden direkt nach erfolgreicher Anmeldung gespeichert; fertig geladene Live-, Film- und Serienbereiche werden einzeln nachgesichert.
- Quellenverwaltung bietet eine Mehrfachauswahl zum Aktualisieren sowie eine eigene Aktualisieren-Schaltfläche pro Portal.
- Fehler beim Wiederherstellen der gespeicherten Portale werden mit ihrer technischen Fehlerklasse intern diagnostiziert, ohne Zugangsdaten oder Portalinhalte zu protokollieren.

## 1.0.49

- Portalaktualisierungen vergleichen gespeicherte Sender, Kategorien und Kataloge mit der Antwort. Unveränderte Daten lösen keine erneute Speicherung oder Sendergruppierung aus; geänderte Inhalte werden übernommen.
- Beim Verlassen des Live-Vollbilds wird der laufende Sender anhand stabiler Kanalmerkmale in seiner Kategorie wiederhergestellt und fokussiert.

## 1.0.48

- Gespeicherte Sender und Kategorien bleiben beim Start die sofort nutzbare Quelle. Standardmäßig prüft der Start nur den Portalzugang; vollständige Kataloge werden nicht bei jedem App-Start neu geladen.
- Vollständige Portalaktualisierung beim Start bleibt optional. EPG-Aktualisierung ist davon getrennt.

## 1.0.47

- Portal-Eingabe bietet zusätzliche MAC-Präfixe sowie ein benutzerdefiniertes Präfix per Textfeld und Hex-Zeichen-Auswahl. Gespeicherte Portale und manuelle HTTP-/HTTPS-Adressen haben getrennte Eingabemodi.
- „Portal laden“ und „Alle Quellen aktualisieren“ verwenden die einheitlichen Fokus-, Hover- und Deaktivierungszustände.
- Die Links-/Rechts-Navigation der oberen Live-TV-Leiste wird nicht mehr von der Tastenbehandlung des Programmführers abgefangen.

## 1.0.46

- Trakt-Merkliste sucht fehlende Film- und Serientitel zusätzlich in den gespeicherten Portalen, damit verfügbare Titel übernommen werden, auch wenn sie im lokalen Katalogcache fehlen.
- Die Live-Navigation lässt rechts/links in der oberen Leiste wieder zwischen Bereichen wechseln. Links aus der Senderkategorie führt zur oberen Navigation statt in ein zweites Seitenmenü.
- Beim Start werden gespeicherte Sender und Kategorien vor dem automatischen Portalrefresh aufbereitet und zuerst bedienbar gemacht.

## 1.0.45

- Trakt prüft vor dem großen Abruf, ob Bibliotheksdaten geändert wurden; unveränderte Daten werden aus dem lokalen Stand übernommen. Folgenfortschritt kommt gebündelt statt mit einer Zusatzanfrage je Serie.
- Trakt-Merkliste wird mit lokalen Film- und Serienquellen abgeglichen; lokale Änderungen markieren die Merkliste für den nächsten Abgleich.
- Portal-, Sendergruppen- und Trakt-Abgleich zeigen ihren Fortschritt in der App.
- Kategorie „Weiter schauen“ zeigt ihren Namen ohne die Zahl der Titel.
- Titel und Beschreibung im Film-/Serien-Player sind heller lesbar. Die Akzentfarbe lässt sich in den Darstellungseinstellungen aus Rot, Blau, Grün, Violett und Orange wählen.

## 1.0.44

- Rechts öffnet Programmdetails nur noch im Live-Vollbild. Im normalen Programmführer bleibt Rechts bei der Zeitleisten- und Listenbedienung.

## 1.0.43

- Live-TV und Einstellungen verwenden durchgehend die gemeinsame dunkle Kartenoptik, eine einheitliche Hauptnavigation und den roten Fokusakzent; das EPG-Zeitraster bleibt erhalten.
- Quellen zeigen TV-, Film- und Serienzahlen pro Portal sowie zusammengeführte Gesamtzahlen ohne doppelte Sender und Titel.
- Trakt lädt gesehene Serien vollständig paginiert, synchronisiert Episodenstände und erkennt den Abschlussstatus pro Serie.
- Neue Serienansicht „Weiter schauen“; abgeschlossene Serien und Filme erhalten keinen Fortsetzen-Button.
- Lokale Anbietersymbole für Prime Video, Disney+ und Peacock ergänzt.

## 1.0.42

- Gemeinsames dunkles Streaming-Design mit rotem Fokusakzent in Live-TV, Filmen, Serien, Einstellungen und Dialogen eingeführt; die Programmzeitleiste bleibt erhalten.
- Film- und Serienkategorien zeigen bis zu zwei beliebte Titel im großen Bildbereich und anschließend die Titel der gewählten Kategorie. „Neu hinzugefügt“ bleibt eine eigene Kategorie und entfällt als zusätzliche Reihe.
- Links/Rechts spulen bei Filmen und Serien nur noch bei ausgeblendeter Steuerleiste. Bei sichtbaren Player-Controls bedienen die Tasten Fokus und Fortschrittsregler.
- Die eingeblendete Player-Info zeigt bei Serien Staffel, Folge und Episodenbeschreibung, bei Filmen Titel und Beschreibung.

## 1.0.41

- Automatische Portalaktualisierung beim Start lässt sich unabhängig von der EPG-Aktualisierung schalten. Gespeicherte Kataloge bleiben während des Hintergrundabgleichs bedienbar und werden bei fehlgeschlagenem Abruf behalten.
- Automatisch deaktivierte, nicht erreichbare Portale werden bei später erfolgreicher Prüfung wieder aktiviert. Manuell deaktivierte Quellen bleiben davon unberührt.
- Trakt importierte Episoden werden direkt in der Episodenliste als gesehen markiert; gespeicherte Episodenfortschritte werden als Balken angezeigt.
- Fokusrahmen und Seitenmarkierung sind in allen gemeinsamen Menüschaltflächen vereinheitlicht; die App öffnet nach einem frischen Start wieder den Live-Bereich.

## 1.0.40

- Poster und Senderlogos werden in einem begrenzten Cache auf dem Gerät abgelegt und nach einem App-Neustart wiederverwendet. Der Cache wird nach 30 Tagen erneuert und auf 48 MB begrenzt; der Bitmap-RAM-Cache ist auf 12 MB begrenzt.
- Unerwartete Fehler während manueller oder automatischer Portalaktualisierungen beenden den Aktualisierungsvorgang mit einer verständlichen Meldung; bereits gespeicherte Inhalte bleiben erhalten.

## 1.0.39

- Auswahlmarkierungen zeigen weiterhin den Seitenstrich; der Rahmen erscheint nur am aktuell fokussierten Element.

## 1.0.38

- Verlauf und Merkliste zeigen Titel in größeren Poster-Karten mit einheitlichem Hintergrund, passend zur Streaming-Startseite.
- Serien-Fortsetzen bleibt im Verlauf auch bei Trakt-Einträgen ohne lokalen Prozentwert sichtbar.

## 1.0.37

- Streaming-Anbieter-Kategorien lassen sich in den Darstellungseinstellungen als Marken-Symbol, Symbol mit Namen oder nur als Name anzeigen.
- Verlauf und Merkliste verwenden weiterhin die Posteransicht. Serien aus dem Trakt-Verlauf zeigen „Fortsetzen“ auch ohne lokalen Prozentwert; die Detailseite setzt anhand des Trakt-Stands bei der nächsten offenen Folge an.
- Episoden-Fortschrittsschlüssel normalisieren Staffel- und Folgennummern, damit Trakt-Gesehen-Markierungen mit den Folgen aus den Portalen übereinstimmen.
- Die Serienübersicht setzt den Titel mit der höchsten verfügbaren Popularität in die große Startfläche und ergänzt ein Poster-Regal für Serien mit neuen Folgen.

## 1.0.36

- Große VOD-Kataloggruppen werden im Hintergrund berechnet, damit die erste App-Ansicht nicht auf der UI-Schleife warten muss.
- Fehler beim Aktivieren oder Aktualisieren einer Portalquelle werden abgefangen und protokolliert, statt die App zu beenden.
- Diagnoseberichte lassen sich auch direkt als Datei exportieren; eine Ordnerauswahl ist nicht mehr erforderlich.
- Der Diagnoseverlauf vermerkt den Start und Abschluss von Bibliothekswiederherstellung und Portalaktualisierung, ohne Portaladressen oder Inhalte zu protokollieren.

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
