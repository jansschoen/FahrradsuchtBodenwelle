# Mailentwürfe und Bezirkszuordnung in 0.4.0

## Bedienung

1. Beende die Fahrt, prüfe Fundstellen und gib nachvollziehbare Einträge frei.
2. Wähle unter **Fundstellen** die Schaltfläche **Meldung vorbereiten**.
3. Unter **Stellen und Bezirke prüfen** kannst du Einträge abwählen und Bezirksvorschläge manuell korrigieren. Bei unklaren Zuordnungen ist dieser Bereich zunächst geöffnet.
4. **Nach Bezirk aufteilen** erstellt getrennte Entwürfe; ohne Häkchen wird die gesamte Auswahl zusammengestellt. Große Auswahlen werden vollständig auf Teile mit höchstens 20 Fundstellen aufgeteilt. Ab 0.5.0 werden bestätigte Wiederholungen als gemeinsame Fundstelle gezählt.
5. Wähle, ob ein kurzer Hinweis auf Bodenwelle ergänzt werden soll. Ohne den Hinweis bleibt die gleiche sachliche Mail in persönlicher Form. Du kannst optional deinen Namen ergänzen. Eigene Notizen werden zunächst nicht übernommen, weil sie Namen und genaue Zeitangaben enthalten können.
6. **Mailentwürfe erzeugen** öffnet die Vorschau. Wähle den gewünschten Bezirk beziehungsweise Teil. Prüfe Anlaufstelle und Empfängeradresse; die offizielle Kontaktseite ist verlinkt. Betreff und Text sind direkt bearbeitbar. Änderungen bleiben beim Wechsel zwischen Entwürfen und beim Drehen des Handys erhalten. Neu-Erzeugen ersetzt die bisherigen Entwürfe durch neue Vorlagen.
7. **Im Mailprogramm öffnen** übergibt den Entwurf an das installierte Mailprogramm. Erst dort versendest du ihn. Bei fehlendem Mailprogramm erklärt die App die Möglichkeiten; **Mailtext kopieren** übernimmt Betreff und Text in die Zwischenablage.

Eine Übergabe wird ausdrücklich nicht als „gesendet“ bezeichnet. Die App kennt den Versand- oder Bearbeitungsstatus des externen Mailprogramms nicht. Es gibt weder SMTP-Zugang noch automatische Behördenkontakte oder einen Online-KI-Dienst. Der Text entsteht lokal aus einer festen, bearbeitbaren Vorlage.

## Inhalt und Zeitangaben

Mails enthalten ausgewählte freigegebene Fundstellen mit Art, Lage, einem OSM-Kartenlink, GPS-Messqualität, verfügbarer gemessener Höhe beziehungsweise ursprünglicher Sensorreaktion und optionalen Gefährdungsmerkmalen. Verkehrsfläche/Oberfläche werden mit ihrer manuellen oder OSM-Herkunft erkennbar angegeben. Sensorwerte werden nicht als Straßenhöhe oder rechtlicher Nachweis ausgegeben. Der Text bittet höflich um Ortsprüfung, Rückmeldung und nötigenfalls Weiterleitung.

Automatisch erzeugte Messzeitangaben enthalten ausschließlich **ISO-Kalenderwoche und ISO-Wochenjahr**, berechnet in `Europe/Berlin`, beispielsweise `KW 42/2026`. Das Wochenjahr kann am Jahreswechsel vom Kalenderjahr abweichen. Weder exakter Messzeitpunkt noch GPS-Fix-Alter, Richtung, OSM-Abfragezeit oder Fahrtspur werden in die Mail übernommen. Die Stellen werden geografisch statt nach zeitlicher Fahrtreihenfolge sortiert. Ab Version 0.5.0 sind ausgewählte bereinigte Fotoanhänge möglich. Es wird kein unveränderter Rohdatenexport angehängt.

Das ist eine Reduzierung der Messzeit, keine Zusicherung vollständiger Anonymität: Fundstellenkoordinaten sind für die Bearbeitung erforderlich, und das Mailprogramm ergänzt beim Versand üblicherweise Absender und Versandzeit. Wenn du eigene Notizen übernimmst oder den Mailtext bearbeitest, prüfe darin enthaltene Namen und konkrete Datums-/Uhrzeitangaben selbst. Der separate CSV-Rohdatenexport behält weiterhin seine genaue Zeit und ist deutlich als solcher angekündigt.

Beim Erzeugen und vor einer Übergabe beziehungsweise Kopie wird die Freigabe der verwendeten Einträge geprüft. Gelöschte oder zwischenzeitlich nicht mehr freigegebene Einträge verhindern die Übergabe des alten Entwurfs. Ein Übergeben an mehrere Bezirke erfolgt einzeln, damit du jede Adresse und Nachricht prüfen kannst.

## Bezirke und tatsächliche Zuständigkeit

Die Bezirkszuordnung erfolgt offline mit zwölf gebündelten Polygonen. Die Daten stammen vom Amt für Statistik Berlin-Brandenburg, wurden durch m-hoerz zusammengeführt und durch die Berliner Morgenpost/FUNKE Interaktiv bereitgestellt. Verwendet wird die [pinnierte GeoJSON-Datei](https://github.com/funkeinteraktiv/Berlin-Geodaten/blob/dbbeef95049f25b2c246f052367385ba0e28d204/berlin_bezirke.geojson) mit Datenstand **3. September 2013**, unter [CC BY 3.0 DE](https://creativecommons.org/licenses/by/3.0/de/). Geometrien wurden unverändert übernommen; Metadaten wurden auf Bezirks-ID und Name reduziert. Herkunft, Lizenz, Änderungen und Prüfsummen liegen zusätzlich im App-Asset `licenses/berlin-districts-cc-by-3.0-de.md` und sind in der Oberfläche verlinkt.

Die direkte Abfrage heutiger amtlicher Grenzdienste über GDI beziehungsweise den bisherigen FIS-Broker war durch den Cloud-Proxy blockiert. Die gebündelten Geometrien sind deshalb ausdrücklich ein **älterer geografischer Vorschlag**, keine aktuelle amtliche Grenzfeststellung. Punkte werden nur zugeordnet, wenn sie innerhalb genau eines Bezirks und mindestens `max(50 m, ursprüngliche GPS-Genauigkeit + 25 % der zeitlichen Positionsverschiebung)` von einer Grenze entfernt liegen. Innenringe/Aussparungen werden berücksichtigt. Die zusätzliche Marge ist vorsichtig gewählt, keine validierte Fehlergrenze. Grenznähe, Überlappung, ungültige Position und außerhalb Berlins bleiben unklar. Manuelle Zuordnung bleibt pro Fundstelle gespeichert und wird von der OSM-Ergänzung nicht verändert.

Das Bezirksamt ist eine vorgeschlagene Anlaufstelle für örtliche Straßenmängel, nicht automatisch der nachgewiesene Straßenbaulastträger. Bei bekannten OSM-Angaben zu Autobahn, `trunk` oder privatem Zugang wird keine Bezirksadresse automatisch vorgegeben; eine passende Anlaufstelle ist selbst einzutragen. Auch ohne solche Tags können Sonderzuständigkeiten bestehen. Adressen sind bearbeitbar; selbst gewählte Adressen werden lokal je Bezirk gespeichert. Es werden keine Adressen aus ungesicherten Annahmen erzeugt.

## Kontaktverzeichnis

Alle zwölf veröffentlichten Adressen wurden am **13. Oktober 2026** auf den nachstehenden offiziellen Berlin.de-Seiten abgerufen und geprüft. Das belegt die Veröffentlichung, nicht die Zuständigkeit für jeden konkreten Straßenabschnitt oder die Annahme eines bestimmten Datenformats. Stand, Quelle und Hash der abgerufenen Seite werden im gebündelten Kontaktverzeichnis geführt. Auf dem Handy findet keine unbemerkte Kontaktaktualisierung statt.

| Bezirk | Vorgeschlagener Kontakt | Offizielle Quelle |
|---|---|---|
| Mitte | sga@ba-mitte.berlin.de | [Straßen- und Grünflächenamt](https://www.berlin.de/ba-mitte/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/) |
| Friedrichshain-Kreuzberg | tiefgruen@ba-fk.berlin.de | [Amtskontakt](https://www.berlin.de/ba-friedrichshain-kreuzberg/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/kontakte/) |
| Pankow | sga.allgemein@ba-pankow.berlin.de | [Straßen- und Grünflächenamt](https://www.berlin.de/ba-pankow/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/) |
| Charlottenburg-Wilmersdorf | tiefbau@charlottenburg-wilmersdorf.de | [Tiefbau](https://www.berlin.de/ba-charlottenburg-wilmersdorf/verwaltung/aemter/strassen-und-gruenflaechen/tiefbau/) |
| Spandau | sga@ba-spandau.berlin.de | [Straßen- und Grünflächenamt](https://www.berlin.de/ba-spandau/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/) |
| Steglitz-Zehlendorf | tiefbauamt@ba-sz.berlin.de | [Straßen- und Grünflächenamt](https://www.berlin.de/ba-steglitz-zehlendorf/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/) |
| Tempelhof-Schöneberg | fb-strassen@ba-ts.berlin.de | [Fachbereich Straßen](https://www.berlin.de/ba-tempelhof-schoeneberg/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/unsere-fachbereiche/strassen/) |
| Neukölln | sga@bezirksamt-neukoelln.de | [Straßen- und Grünflächenamt](https://www.berlin.de/ba-neukoelln/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/) |
| Treptow-Köpenick | SGA-Tief@ba-tk.berlin.de | [Fachbereich Tiefbau](https://www.berlin.de/ba-treptow-koepenick/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/artikel.28034.php) |
| Marzahn-Hellersdorf | SGA-Strassen@ba-mh.berlin.de | [Fachbereich Straßen](https://www.berlin.de/ba-marzahn-hellersdorf/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/) |
| Lichtenberg | sga@lichtenberg.berlin.de | [Behördenwegweiser SGA](https://www.berlin.de/ba-lichtenberg/politik-und-verwaltung/behoerdenwegweiser/artikel.250475.php) |
| Reinickendorf | sga@reinickendorf.berlin.de | [Straßenbau](https://www.berlin.de/ba-reinickendorf/politik-und-verwaltung/aemter/strassen-und-gruenflaechenamt/strassenbau/) |

## Technische Prüfung und nächste Verbesserungen

Schema 5 fügt nur die optionale manuelle Bezirkszuordnung hinzu; Fundstellen, Sensorwerte, Klassifizierung, OSM-Korrekturen und Fahrtspur bleiben erhalten. Die Android-Übergabe verwendet `ACTION_SENDTO` mit einem korrekt kodierten `mailto:`-URI und kompatiblen Text-Extras. Sie akzeptiert einen einzelnen Empfänger, verhindert zusätzliche Header durch Zeilenumbrüche und begrenzt zu große Entwürfe.

Tests prüfen Reglerumkehr, Zahleneingabe, ungültige Eingaben, ISO-Wochenjahr und Berliner Zeitzone, fehlende genaue Messzeiten, Ausschluss nicht freigegebener Stellen, Bezirksaufteilung und vollständige Teilmails, repräsentative Punkte aller zwölf Bezirke, Grenzfälle, Migration, Mail-Intent, manuelle Empfänger, Widerruf der Freigabe und erhaltene Bearbeitungen beim Drehen des Geräts. Frameworktests laufen auf simuliertem Android 9 und 15. Das konkrete Mailprogramm auf dem Fairphone muss noch praktisch geprüft werden; einige Programme können vorbefüllte Angaben unterschiedlich übernehmen.

Bündelung und bereinigte Fotoanhänge sind umgesetzt. Ein möglicher nächster Schritt ist die lokale Verwaltung von Aktenzeichen und Bearbeitungsständen. Aktuelle amtliche Grenzgeometrien und gegebenenfalls ein Straßenbaulastverzeichnis würden die Empfängervorschläge weiter verbessern.

## Erweiterung in 0.5.0

Bestätigte Bündelungen, lokale Fotos, Fotoauswahl und deren Übergabe sind in [BUENDELUNG-FOTOS.md](BUENDELUNG-FOTOS.md) dokumentiert. Der Mailtext nennt nur ausgewählte freigegebene Beobachtungen; exakte Messzeiten bleiben lokal.

## Unebene Straßenabschnitte

Abschnitte werden als ein persönlich freizugebender Eintrag mit Anfang, Ende und geschätzter Länge gemeldet. Bei einem Wochenwechsel erscheinen beide Kalenderwochen. Genaue Zeitpunkte und die Fahrtspur werden weiterhin nicht übernommen. Unterschiedliche Bezirksvorschläge für Anfang und Ende lassen die Zuständigkeit offen; ein Bezirk kann nach Prüfung manuell gesetzt werden. Die Abschnittserkennung und ihre Ausdehnung sind ausdrücklich vorläufig.

Seit **0.10.0** stehen Fotos, Verkehrsfläche, Oberfläche und Gefährdungsmerkmale gemeinsam für alle Messungen eines Bündels zur Verfügung. Gemeinsame Fotos erscheinen je gemeldeter Fundstelle einmal; ihre ursprüngliche Importmessung muss dafür nicht zusätzlich freigegeben werden. Die tatsächlich gemeldeten Messwerte, Notizen, Nummern und Kalenderwochen stammen weiterhin ausschließlich aus ausgewählten freigegebenen Einzelmessungen. Bei getrennten Fundstellen kann dasselbe gemeinsame Foto für jede Stelle unabhängig gewählt werden; diese Auswahl bleibt beim Drehen erhalten.
