# Klassifizierung und OSM – Stand 0.12.0

## Architektur und Umsetzung

Die bestehende native Java-App verwendet `RecordingService` für Beschleunigung und GPS, `FindingStore` für SQLite und osmdroid für die Karte. Eine eigene Cloud-Datenbank ist nicht beteiligt. Sensoraufzeichnung, Empfindlichkeitsregler, Positionskorrektur, Fahrtspur und Freigabeexport bleiben erhalten.

Die Erweiterung besteht aus stabilen Kategorie-IDs in `Classification`, getrennten Messbewertungen in `Intensity`, strukturierten Metadaten in `FindingDetails`, einer aufklappbaren Bearbeitungsmaske und den separaten OSM-Komponenten. Geodaten werden außerhalb des Sensor- und UI-Threads ergänzt. Seit Schema 4 liegen strukturierte JSON-Metadaten und ein begrenzter Geodatencache vor. Schema 8 vereinheitlicht die Beobachtungsangaben gebündelter Fundstellen; Messwerte und Fahrtspur bleiben erhalten. Im JSON stehen Messwert, Einheit, Methode, Klasse, Gefährdungsmerkmale und Geodaten jeweils in eigenen Feldern.

## Unebenheitsarten und Migration

| Interne ID | Anzeige |
|---|---|
| `root_heave` | Wurzelaufbruch |
| `pothole` | Schlagloch |
| `transverse_groove` | Querrille |
| `longitudinal_groove` | Längsrille |
| `paving_displacement` | Pflasterversatz |
| `long_wave` | Langgezogene Bodenwelle |
| `other` | Sonstige Unebenheit |

„Wurzel“ wird Wurzelaufbruch, „Schiefe Bodenplatte“ wird Pflasterversatz. Die früheren allgemeinen Kategorien „Bodenwelle“, „Straßenschaden“, „Sonstiges“, „Ungeklärt“ und unbekannte Angaben werden `other`; ihre ursprüngliche Angabe bleibt als `legacy_category` erhalten. Insbesondere wird aus „Bodenwelle“ keine langgezogene Welle geraten. Bereits geprüfte Einträge behalten Notiz und Freigabe. Automatische Erkennung wählt zunächst `other`; die Art bestimmt die Person nach Sichtprüfung. Es gab bisher keine Kategorie- oder Intensitätsfilter und keine entsprechende Statistik.

## Zwei getrennte Intensitätsskalen

Nur ein **tatsächlich vor Ort gemessener und ausdrücklich bestätigter Höhenunterschied** verwendet `manual_measured_height_v1`. Die App berechnet derzeit keine Höhe aus Sensorwerten.

| Messhöhe | Klasse | Farbe |
|---|---|---|
| 0–5 mm | 1 · Sehr gut | Dunkelgrün |
| >5–10 mm | 2 · Gut | Hellgrün |
| >10–20 mm | 3 · Beeinträchtigt | Gelb |
| >20–30 mm | 4 · Problematisch | Orange |
| >30 mm | 5 · Potenziell gefährlich | Rot |

Diese Grenzen sind projektintern und keine gesetzlichen Grenzwerte. Form, Richtung und Umfeld einer Unebenheit können ihre Wirkung zusätzlich verändern; die Höhenklasse allein ist keine umfassende Sicherheitsbewertung.

Ohne bestätigte Höhenmessung bewertet `sensor_peak_provisional_v1` ausschließlich den ursprünglichen Betrag der gefilterten vertikalen Spitzenbeschleunigung nach geschätztem Schwerkraftabzug:

| Sensorreaktion | Vorläufige Klasse | Farbe |
|---|---|---|
| 0–10 m/s² | 1 · Sehr schwache Sensorreaktion | Dunkelgrün |
| >10–20 m/s² | 2 · Schwache Sensorreaktion | Hellgrün |
| >20–40 m/s² | 3 · Mittlere Sensorreaktion | Gelb |
| >40–60 m/s² | 4 · Starke Sensorreaktion | Orange |
| >60 m/s² | 5 · Sehr starke Sensorreaktion | Rot |

Diese Sensorgrenzen sind **unvalidierte Testbänder**, keine physikalische Höhenumrechnung und kein Gefahrennachweis. Sie dienen zunächst dem Vergleich unter ähnlichen Bedingungen auf derselben Kombination von Fahrrad, Handy und Halterung. Geschwindigkeit, Reifen, Beladung und Befestigung beeinflussen die Werte. Die Auslöseschwelle (standardmäßig 40 m/s², einstellbar 5–100) ist von dieser Bewertung getrennt: Sie entscheidet, ob ein neuer Treffer entsteht. Ohne Höhe und Sensorwert bleibt Klasse 0 „Intensität unbekannt“, grau. Ein bestätigter Höhenwert bestimmt die Höhenklasse, während ursprünglicher Sensorwert und Erkennungsschwelle zusätzlich erhalten bleiben. Entfernen der Höhenbestätigung stellt die Sensorbewertung wieder her.

Gefährdungsmerkmale sind unabhängige Kennzeichen: `poor_visibility`, `no_escape`, `downhill`, `bend`, `standing_water`. Sie verändern weder Messwert noch Klasse. Eine kombinierte Risikobewertung wird nicht berechnet.

## OSM-Daten und räumliche Zuordnung

Die App fragt `https://overpass-api.de/api/interpreter` per HTTPS nach Wegen mit `highway` und ihrer Geometrie. Der Ausschnitt wird auf ein 0,002°-Raster gelegt und mit einem Rand von 0,003° erweitert. Die Abfrage umfasst in Berlin ungefähr einen Kilometer oder weniger pro Seitenlänge. Anfragen enthalten den Ausschnitt und die IP-Adresse, keine Notiz, Beschleunigungswerte oder Freigaben. Die Ergänzung ist unter **Mehr → Karte & Geodaten** abschaltbar; Kartenkacheln sind eine separate Netzwerkfunktion.

Es gibt höchstens eine neue Abfrage pro Minute und Installation. Nach Fehlern gilt mindestens fünf Minuten Pause; ein längeres `Retry-After` des Servers wird berücksichtigt. Die App setzt einen identifizierenden User-Agent, 20 Sekunden Serverlaufzeit, 10 Sekunden Verbindungs- und 30 Sekunden Lesetimeout sowie maximal 4 MiB Antwortgröße. Unvollständige Antworten werden nicht gespeichert. Der lokale Cache hält höchstens 64 Ausschnitte für sieben Tage frisch. Offline können ältere Daten mit niedriger Sicherheit und ursprünglichem Abfragedatum weiterverwendet werden.

Seit **0.12.0 / Matcher 3** verwenden Abfrage, Cache-Zelle und geometrische Zuordnung stets den **ursprünglichen GPS-Mittelpunkt** (`gps_latitude`, `gps_longitude`), nicht die zeitlich verschobene Schätzung des Sensorereignisses und nicht einen zufälligen Punkt im Genauigkeitskreis. Die korrigierte Ereignisposition bleibt für Messung und Karte unverändert gespeichert. `bodenwelle:position_basis=gps_circle_center` dokumentiert diese Wahl. Ein Kreismittelpunkt ist eine Positionsschätzung, keine Garantie für den tatsächlichen Weg. Bei einer alten GPS-Position kann er entlang der Fahrt versetzt sein; diese bewusste Wahl verbessert nicht automatisch die Ortungsgenauigkeit.

Die Zuordnung verwendet den kürzesten Abstand zur Weggeometrie und, sofern brauchbar, die GPS-Fahrtrichtung. Gegenläufige Richtungen auf demselben Weg sind zulässig; über 60° Abweichung werden ausgeschlossen. Der Suchradius bleibt **50 m**. Die Abfragebox ist bereits deutlich größer und muss dafür nicht erweitert werden. Eine ursprüngliche GPS-Genauigkeit über **50 m** oder ungültige Koordinaten ergeben `gps_uncertain` ohne neue Netzabfrage. Ein größerer Radius würde fehlende OSM-Tags nicht ersetzen und öfter Nachbarstraßen einbeziehen.

Das Rangmaß ist Abstand in Metern plus Richtungsabweichung geteilt durch drei. Kandidaten bis `max(8 m, ursprüngliche GPS-Genauigkeit)` über dem besten gelten als plausibel. Stimmen bekannte Verkehrsfläche oder Oberfläche bei allen Kandidaten überein, werden diese Felder unabhängig als unsicherer **Gebietsvorschlag** übernommen (`area_suggestion`, keine einzelne Weg-ID). Bei widersprüchlichen Kandidaten kann der bestbewertete Weg einen **ausdrücklich unsicheren Vorschlag** liefern (`nearest_suggestion`, Sicherheit `low`), wenn sein Rangmaß mindestens einen Punkt besser ist. Diese Trennschwelle ist heuristisch. Als fehlend erkannte Felder des besten Wegs werden nicht vom Nachbarweg übernommen. Gleichwertige Kandidaten liefern nur übereinstimmende Angaben; ohne solche bleibt `ambiguous`. Das Ergebnis darf nicht als gesicherte Wegzuordnung verwendet werden. Alternative Weg-IDs stehen in `bodenwelle:candidate_way_ids`. Fehlende Belagsangaben bleiben unbekannt.

Bei nur einem plausiblen Weg ergeben GPS bis 8 m, Wegabstand bis 8 m und widerspruchsfreie Tags höchstens „mittlere Sicherheit“, sonst „unsicher“. Quelle, Weg-ID soweit vorhanden, Tags, Abstand und Zeitpunkt bleiben nachvollziehbar. Diese Heuristik ist keine statistische Wahrscheinlichkeit. Kreuzungen, Kurven, parallele Wege, fehlende Richtung und unvollständige Geometrien begrenzen sie.

## Bedeutung der Tags

`highway=cycleway` oder `bicycle=designated` allein ergibt **keine Benutzungspflicht**. Nur explizite `traffic_sign=DE:237` beziehungsweise `DE:241` auf einem eigenen passenden Weg liefern einen Vorschlag für „Benutzungspflichtiger Radweg“. `DE:240` liefert „Gemeinsamer Geh- und Radweg“. Das sind OSM-Vorschläge, keine rechtlich verbindlichen Feststellungen; tatsächliche Beschilderung, Geltungsrichtung und Kartenaktualität müssen vor Ort geprüft werden. Widersprüchliche Tags bleiben unbekannt. `bicycle_road=yes`, `foot`, `segregated` und seitliche `cycleway`-Tags werden berücksichtigt. Seitliche Radverkehrsanlagen an Straßen ergeben die breitere Kategorie **Straße mit Radverkehrsanlage (Zuordnung offen)**. Ein für Fuß- und Radverkehr geeigneter Weg ohne eindeutige Trennungs-/Beschilderungsangabe kann **Weg für Fuß- und Radverkehr (Beschilderung offen)** ergeben. Beide bleiben unsicher; eine konkrete Spur oder Benutzungspflicht wird daraus nicht behauptet.

`surface` bestimmt die verständliche Oberfläche. `paving_stones` wird nur mit ausdrücklichem Betonmaterial als Betonpflaster bezeichnet; ohne Materialangabe wird es **Pflaster (Material unbekannt)**. `paved` wird **Befestigt (Material unbekannt)**. Damit bleibt eine nützliche allgemeinere Angabe erhalten, ohne Beton oder Asphalt zu erfinden. `smoothness`, `tracktype` und `width` werden unverändert mitgeführt, aber nicht als Messung dieses einzelnen Stoßes interpretiert. Insbesondere `smoothness` beschreibt die allgemeine Befahrbarkeit des Wegs. Fehlende Werte werden nicht erfunden.

Manuelle Verkehrsflächen- und Oberflächenangaben haben unabhängige Herkunftskennzeichen. Hintergrundantworten ändern nur die automatischen Vorschläge. Eine manuelle Angabe, einschließlich „Unbekannt“, wird nicht überschrieben. Die Bearbeitung berücksichtigt auch Antworten, die während einer geöffneten Eingabemaske eintreffen. Entfernen des entsprechenden Häkchens aktiviert wieder den letzten OSM-Vorschlag.

## Offline und spätere Ergänzung

Sensoraufzeichnung und lokale Speicherung hängen nicht vom OSM-Abruf ab. Neue Fundstellen werden unabhängig erfasst. Beim Öffnen der Haupt-/Kartenansicht werden fehlende, veraltete oder mit der alten Zuordnungslogik bearbeitete Einträge eingereiht. Ein aktives Internet-fähiges Netz genügt seit 0.12.0; eine Android-Netzvalidierung wird nicht mehr zusätzlich vorausgesetzt, weil sie etwa bei besonderen VPN-/Datenschutzeinstellungen fehlen kann. Verbindungsfehler werden weiterhin regulär behandelt. Bereits frische Cacheantworten werden neu ausgewertet; dafür ist kein neuer Netzabruf nötig. Das lokale Metadatum `bodenwelle:matcher_version` hält diese Auswertung nachvollziehbar.

Die deduplizierte Warteschlange behält zurückgestellte Einträge und setzt sie nach dem Minutenlimit beziehungsweise einer Fehlerpause fort. Verfügbare Cacheantworten für andere Punkte können sofort verwendet werden. Überlappende Abfrageboxen benachbarter Rasterzellen werden wiederverwendet, sofern sie den neuen Suchkreis vollständig abdecken. Höchstens eine Netzabfrage pro Minute, mindestens fünf Minuten Fehlerpause und längere serverseitige `Retry-After`-Fristen bleiben erhalten. Offline ohne Cache wird frühestens nach einer Minute erneut versucht; Daten werden dabei nicht gelöscht.

Wiederholungen laufen bei geöffneter Haupt-/Kartenansicht oder während einer Aufzeichnung. Bei geschlossener Ansicht ohne laufende Fahrt oder abgeschalteten Geodaten werden weitere Arbeiten beendet; beim nächsten Öffnen werden ausstehende Einträge erneut eingereiht. Es gibt keine dauerhafte Synchronisierung bei geschlossener App. Frische, mit aktueller Logik geprüfte mehrdeutige/erfolglose Antworten werden nicht fortwährend neu abgefragt. Manuelle Korrekturen einschließlich „Unbekannt“ bleiben geschützt.

## Export, Lizenz und Prüfstand

Der CSV-Export enthält weiterhin nur freigegebene Fundstellen und keine Fahrtspur. Die bisherigen 18 Spalten bleiben in ihrer Reihenfolge erhalten; zusätzliche Spalten enthalten bestätigte Höhe, Bewertungswert, Einheit, Methode, numerische Klasse, Gefährdungs-IDs, frühere Kategorie, Verkehrsfläche/Oberfläche mit Herkunft sowie OSM-ID, Originaltags als JSON, Zeitpunkt, Sicherheit, Status, Quelle und Abstand. `height_mm` bleibt ohne bestätigte Messung leer. Klassennummern sind nur zusammen mit `intensity_method` interpretierbar.

OSM-Daten stammen von © OpenStreetMap contributors und unterliegen der [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/). Die App verlinkt die [OSM-Copyrightseite](https://www.openstreetmap.org/copyright); jeder exportierte Datensatz führt Attribution und Lizenzlink mit. Bei öffentlicher Weitergabe einer aus OSM abgeleiteten Datenbank sind die ODbL-Bedingungen einschließlich gegebenenfalls erforderlicher Weitergabe unter derselben Lizenz zu berücksichtigen. Eigene Sensorbeobachtungen werden separat als eigene Messfelder geführt. Die Apache-Lizenz der Kartenbibliothek ist davon unabhängig.

Die Live-Abfrage wurde am **3. November 2026** erneut durch den Cloud-Proxy mit HTTP 403 beim HTTPS-Tunnel blockiert. Damit ist weder die tatsächliche Antwort auf dem Fairphone noch die Ursache dort bestätigt. Die Anbindung ist mit kontrollierten OSM-Antworten geprüft. Für breite öffentliche Nutzung bleiben Betreiberregeln und Kapazität zu prüfen und gegebenenfalls ein eigener oder vereinbarter Geodatenanbieter einzusetzen.

### Wenn weiterhin „unbekannt“ erscheint

Unter **Prüfen & Fotos → Verkehrsfläche und Oberfläche prüfen** zeigt **OSM-Abrufdiagnose** den letzten Netzversuch, HTTP-Status beziehungsweise DNS-/TLS-/Timeoutfehler, Anzahl der empfangenen Wege, Cache-Nutzung und letzte Zuordnung. Die Diagnose wird ausschließlich lokal gespeichert und enthält keine Koordinaten, Notizen oder Sensorwerte. **OSM-Vorschlag neu abfragen** fordert eine neue Antwort für diesen Punkt an. **Mehr → Karte & Geodaten → Fehlende OSM-Angaben ergänzen** fordert neue Antworten für fehlende Verkehrsflächen oder Oberflächen an. Beide umgehen einen älteren frischen Cache, aber keine Minutenlimits oder Fehlerpausen. Eine neue Antwort wird von allen gleichzeitig angeforderten Nachbarpunkten wiederverwendet. Offline helfen vorhandene Cacheantworten; erneute Versuche laufen bei aktiver App oder Fahrt.

Automatische Vorschläge aktualisieren sich während der geöffneten Bearbeitung; selbst ausgewählte Felder werden nicht überschrieben. Eine manuell gesetzte Angabe **Unbekannt** bleibt geschützt. Häkchen entfernen und Speichern aktiviert die gemeinsame OSM-Auswertung wieder. Erfolgreich geladene Kartenkacheln belegen nicht, dass auch die separate Overpass-Verbindung funktioniert.

OSM bleibt für diese Vorschläge geeignet: [highway](https://wiki.openstreetmap.org/wiki/Key:highway), [surface](https://wiki.openstreetmap.org/wiki/Key:surface) und [cycleway](https://wiki.openstreetmap.org/wiki/Key:cycleway) enthalten die relevanten Angaben, soweit sie vor Ort erfasst wurden. Eine andere Overpass-Instanz verwendet weiterhin OSM und ergänzt keine fehlenden Tags. Amtliche Berliner Geodaten könnten später zusätzliche Hinweise liefern, benötigen aber einen konkret geeigneten, räumlich und inhaltlich ausreichend aktuellen Datensatz samt Lizenzprüfung. Eine solche zweite Quelle wird hier nicht ohne geprüfte Eignung angeschlossen. Bei GPS-Kreisen über benachbarten Flächen bleibt eine Sichtprüfung beziehungsweise manuelle Korrektur nötig.

Automatisierte Tests prüfen Höhen- und Sensorgrenzen, fehlende und widersprüchliche Tags, Oberflächen, parallele und kreuzende Wege, Cache, Offline-Ergänzung, Fehlerpausen, manuelle Korrekturen und deren Nebenläufigkeit, Datenmigration sowie aktualisierte Kartenfarben. Native Android-Frameworktests laufen auf simuliertem Android 9 und 15. Der tatsächliche Abruf auf dem Fairphone und die Zuordnungsqualität im Fahrtest bleiben zu prüfen; die App trifft keine verbindliche Aussage über Zulässigkeit oder Beseitigungspflichten.

Ab Version 0.4.0 steigen die Zahlenwerte des Sensitivitätsreglers nach rechts; eine direkte Zahleneingabe ist zusätzlich vorhanden. Mailentwürfe und Kalenderwochen sind in [MAIL-MELDUNGEN.md](MAIL-MELDUNGEN.md) dokumentiert.

## Straßenabschnitte

Seit 0.7.0 ergänzt `rough_road` die Kategorie **Unebener Straßenabschnitt**. Die optionale Abschnittsgeometrie bleibt beim Prüfen und bei OSM-Ergänzungen erhalten. Anfang und Ende sind GPS-Schätzungen, die Länge stammt aus Geschwindigkeit und Zeit. Quelle, Unsicherheit und einzelne Freigaben bleiben nachvollziehbar. Die CSV-Spalte `rough_section` enthält die optionalen Abschnittsmetadaten als JSON; die bisherigen Spalten bleiben in derselben Reihenfolge. [Erkennungslogik und Bedienung](KALIBRIERUNG-ABSCHNITTE.md).
