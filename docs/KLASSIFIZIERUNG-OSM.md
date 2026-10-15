# Klassifizierung und OSM in Version 0.3.0

## Architektur und Umsetzung

Die bestehende native Java-App verwendet `RecordingService` für Beschleunigung und GPS, `FindingStore` für SQLite und osmdroid für die Karte. Eine eigene Cloud-Datenbank ist nicht beteiligt. Sensoraufzeichnung, Empfindlichkeitsregler, Positionskorrektur, Fahrtspur und Freigabeexport bleiben erhalten.

Die Erweiterung besteht aus stabilen Kategorie-IDs in `Classification`, getrennten Messbewertungen in `Intensity`, strukturierten Metadaten in `FindingDetails`, einer aufklappbaren Bearbeitungsmaske und den separaten OSM-Komponenten. Geodaten werden außerhalb des Sensor- und UI-Threads ergänzt. Das Datenbankschema 4 erhält bestehende Felder und die Fahrtspur und fügt strukturierte JSON-Metadaten sowie einen begrenzten Geodatencache hinzu. Im JSON stehen Messwert, Einheit, Methode, Klasse, Gefährdungsmerkmale und Geodaten jeweils in eigenen Feldern.

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

Die Zuordnung verwendet den kürzesten Abstand zur Weggeometrie und, sofern brauchbar, die GPS-Fahrtrichtung. Gegenläufige Fahrtrichtungen auf demselben Weg sind zulässig. Wege mit mehr als 60° Richtungsabweichung werden ausgeschlossen. Seit 0.9.0 werden Weggeometrien bis **50 m** Abstand berücksichtigt. Die bereits zuvor größere Abfragebox muss dafür nicht vergrößert werden. Eine ursprüngliche GPS-Genauigkeit über **50 m** oder ungültige Koordinaten erlauben weiterhin keine Zuordnung; schlechtere Genauigkeit innerhalb der Grenze führt zu unsicheren Vorschlägen. Für zeitlich projizierte Positionen wird zusätzlich eine heuristische Unsicherheitsmarge von 25 % der Verschiebung, höchstens 10 m, verwendet. Sie ist **keine validierte Fehlergrenze**.

Das Rangmaß ist Abstand in Metern plus Richtungsabweichung geteilt durch drei. Alle Kandidaten, deren Rangmaß höchstens `max(8 m, ursprüngliche GPS-Genauigkeit + Projektionsmarge)` über dem besten liegt, gelten als plausibel. Bei mehreren Kandidaten wird kein einzelner Weg behauptet. Nur wenn eine bekannte Verkehrsfläche oder Oberfläche bei **allen** plausiblen Wegen übereinstimmt, erscheint diese Angabe als unsicherer **Gebietsvorschlag** (`area_suggestion`, keine Weg-ID). Die beiden Felder werden unabhängig verglichen; ein fehlender oder widersprüchlicher Wert bleibt unbekannt. Originaltags werden dabei nur als übereinstimmende Teilmenge übernommen; `bodenwelle:candidate_way_ids` nennt die verglichenen Weg-IDs. Sonst bleibt der Status `ambiguous`. GPS bis 8 m, Projektionsmarge bis 2 m, Wegabstand bis 8 m und widerspruchsfreie Tags ergeben höchstens „mittlere Sicherheit“; ansonsten „unsicher“. Es handelt sich um eine Heuristik, nicht um eine statistische Wahrscheinlichkeit oder vollständige Navigationszuordnung. Kreuzungen, Kurven, parallele Wege, fehlende Richtung und unvollständige Geometrien begrenzen sie.

Bei einer Zuordnung werden Weg-ID, alle gelieferten Originaltags, Abfragezeitpunkt, Quelle, Abstand und Sicherheit gespeichert. Bei weder eindeutiger Zuordnung noch übereinstimmenden Gebietsvorschlägen bleiben Verkehrsfläche und Oberfläche unbekannt. Ein größerer Radius kann fehlende OSM-Tags nicht ersetzen. Die Oberfläche bleibt unbekannt, wenn das Material nicht hinreichend beschrieben ist. Der tatsächliche Wegabstand und fehlende Oberflächenangaben werden in den Details genannt. Die Rohantwort im Cache enthält auch alternative Wege; mehrdeutige Wege werden nicht als gesicherte einzelne Fundstelle exportiert.

## Bedeutung der Tags

`highway=cycleway` oder `bicycle=designated` allein ergibt **keine Benutzungspflicht**. Nur explizite `traffic_sign=DE:237` beziehungsweise `DE:241` auf einem eigenen passenden Weg liefern einen Vorschlag für „Benutzungspflichtiger Radweg“. `DE:240` liefert „Gemeinsamer Geh- und Radweg“. Das sind OSM-Vorschläge, keine rechtlich verbindlichen Feststellungen; tatsächliche Beschilderung, Geltungsrichtung und Kartenaktualität müssen vor Ort geprüft werden. Widersprüchliche Tags bleiben unbekannt. `bicycle_road=yes`, `foot`, `segregated` und seitliche `cycleway`-Tags werden berücksichtigt. Eine auf einer Fahrbahn eingezeichnete seitliche Radverkehrsanlage wird ohne sichere Spurzuordnung nicht als konkrete befahrene Fläche behauptet.

`surface` bestimmt die verständliche Oberfläche. `paving_stones` wird nur mit ausdrücklichem Betonmaterial als Betonpflaster bezeichnet; ohne Materialangabe bleibt es „Sonstige Oberfläche“. `paved` allein nennt kein Material. `smoothness`, `tracktype` und `width` werden unverändert mitgeführt, aber nicht als Messung dieses einzelnen Stoßes interpretiert. Insbesondere `smoothness` beschreibt die allgemeine Befahrbarkeit des Wegs. Fehlende Werte werden nicht erfunden.

Manuelle Verkehrsflächen- und Oberflächenangaben haben unabhängige Herkunftskennzeichen. Hintergrundantworten ändern nur die automatischen Vorschläge. Eine manuelle Angabe, einschließlich „Unbekannt“, wird nicht überschrieben. Die Bearbeitung berücksichtigt auch Antworten, die während einer geöffneten Eingabemaske eintreffen. Entfernen des entsprechenden Häkchens aktiviert wieder den letzten OSM-Vorschlag.

## Offline und spätere Ergänzung

Sensoraufzeichnung und lokale Speicherung hängen nicht vom OSM-Abruf ab. Neue Fundstellen werden unabhängig erfasst. Beim Öffnen der Haupt-/Kartenansicht und über **Fehlende OSM-Angaben ergänzen** werden fehlende, veraltete oder mit der alten Zuordnungslogik bearbeitete Einträge eingereiht. Bereits frische Cacheantworten werden neu ausgewertet; dafür ist kein neuer Netzabruf nötig. Das lokale Metadatum `bodenwelle:matcher_version` hält diese Auswertung nachvollziehbar.

Die deduplizierte Warteschlange behält zurückgestellte Einträge und setzt sie nach dem Minutenlimit beziehungsweise einer Fehlerpause fort. Verfügbare Cacheantworten für andere Punkte können sofort verwendet werden. Überlappende Abfrageboxen benachbarter Rasterzellen werden wiederverwendet, sofern sie den neuen Suchkreis vollständig abdecken. Höchstens eine Netzabfrage pro Minute, mindestens fünf Minuten Fehlerpause und längere serverseitige `Retry-After`-Fristen bleiben erhalten. Offline ohne Cache wird frühestens nach einer Minute erneut versucht; Daten werden dabei nicht gelöscht.

Wiederholungen laufen bei geöffneter Haupt-/Kartenansicht oder während einer Aufzeichnung. Bei geschlossener Ansicht ohne laufende Fahrt oder abgeschalteten Geodaten werden weitere Arbeiten beendet; beim nächsten Öffnen werden ausstehende Einträge erneut eingereiht. Es gibt keine dauerhafte Synchronisierung bei geschlossener App. Frische, mit aktueller Logik geprüfte mehrdeutige/erfolglose Antworten werden nicht fortwährend neu abgefragt. Manuelle Korrekturen einschließlich „Unbekannt“ bleiben geschützt.

## Export, Lizenz und Prüfstand

Der CSV-Export enthält weiterhin nur freigegebene Fundstellen und keine Fahrtspur. Die bisherigen 18 Spalten bleiben in ihrer Reihenfolge erhalten; zusätzliche Spalten enthalten bestätigte Höhe, Bewertungswert, Einheit, Methode, numerische Klasse, Gefährdungs-IDs, frühere Kategorie, Verkehrsfläche/Oberfläche mit Herkunft sowie OSM-ID, Originaltags als JSON, Zeitpunkt, Sicherheit, Status, Quelle und Abstand. `height_mm` bleibt ohne bestätigte Messung leer. Klassennummern sind nur zusammen mit `intensity_method` interpretierbar.

OSM-Daten stammen von © OpenStreetMap contributors und unterliegen der [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/). Die App verlinkt die [OSM-Copyrightseite](https://www.openstreetmap.org/copyright); jeder exportierte Datensatz führt Attribution und Lizenzlink mit. Bei öffentlicher Weitergabe einer aus OSM abgeleiteten Datenbank sind die ODbL-Bedingungen einschließlich gegebenenfalls erforderlicher Weitergabe unter derselben Lizenz zu berücksichtigen. Eigene Sensorbeobachtungen werden separat als eigene Messfelder geführt. Die Apache-Lizenz der Kartenbibliothek ist davon unabhängig.

Die Live-Abfrage und der Abruf der aktuellen Overpass-Dokumentation wurden am 13. und erneut am 15. Oktober 2026 durch den Cloud-Proxy mit HTTP 403 blockiert. Daher ist die aktuelle Erreichbarkeit und Nutzungsregelprüfung des öffentlichen Endpunkts nicht bestätigt. Die konservativ begrenzte Anbindung ist für den persönlichen Prototyp umgesetzt; vor breiter öffentlicher Nutzung müssen aktuelle Betreiberregeln und Kapazität geprüft und gegebenenfalls ein eigener oder vereinbarter Geodatenanbieter verwendet werden.

Automatisierte Tests prüfen Höhen- und Sensorgrenzen, fehlende und widersprüchliche Tags, Oberflächen, parallele und kreuzende Wege, Cache, Offline-Ergänzung, Fehlerpausen, manuelle Korrekturen und deren Nebenläufigkeit, Datenmigration sowie aktualisierte Kartenfarben. Native Android-Frameworktests laufen auf simuliertem Android 9 und 15. Der tatsächliche Abruf auf dem Fairphone und die Zuordnungsqualität im Fahrtest bleiben zu prüfen; die App trifft keine verbindliche Aussage über Zulässigkeit oder Beseitigungspflichten.

Ab Version 0.4.0 steigen die Zahlenwerte des Sensitivitätsreglers nach rechts; eine direkte Zahleneingabe ist zusätzlich vorhanden. Mailentwürfe und Kalenderwochen sind in [MAIL-MELDUNGEN.md](MAIL-MELDUNGEN.md) dokumentiert.

## Straßenabschnitte

Seit 0.7.0 ergänzt `rough_road` die Kategorie **Unebener Straßenabschnitt**. Die optionale Abschnittsgeometrie bleibt beim Prüfen und bei OSM-Ergänzungen erhalten. Anfang und Ende sind GPS-Schätzungen, die Länge stammt aus Geschwindigkeit und Zeit. Quelle, Unsicherheit und einzelne Freigaben bleiben nachvollziehbar. Die CSV-Spalte `rough_section` enthält die optionalen Abschnittsmetadaten als JSON; die bisherigen Spalten bleiben in derselben Reihenfolge. [Erkennungslogik und Bedienung](KALIBRIERUNG-ABSCHNITTE.md).
