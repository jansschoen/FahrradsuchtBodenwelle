# FahrradsuchtBodenwelle

Android-Prototyp für Fahrradfahrten in Berlin: auf einer integrierten OpenStreetMap-Karte den eigenen Standort und die Fahrtspur sehen, auffällige Erschütterungen erfassen, nach der Fahrt prüfen und nur freigegebene Fundstellen exportieren.

Die App misst die Reaktion des montierten Smartphones. **Sie misst keine Bodenwellenhöhe und bewertet keine Rechtskonformität.** Fundstellen und die letzte Fahrtspur bleiben auf dem Handy. Die App lädt OSM-Kartenkacheln über HTTPS und übermittelt dafür angefragte Kartenausschnitte und die IP-Adresse an den Kartenanbieter. Sie versendet keine Schadensmeldungen oder Streckendateien automatisch. Geprüfte Fundstellen lassen sich als Mailentwurf an dein Mailprogramm übergeben.

## Karte und Fahrtspur

Die Startseite enthält eine zoombare native OSM-Karte. GPS-Genauigkeit und Alter der Position werden laufend angezeigt; fehlende und veraltete Messungen werden gekennzeichnet. **Mein Standort** aktiviert die Standortnachführung; Verschieben der Karte unterbricht die Nachführung. **Kartenoptionen → Fahrtspur ansehen** zeigt die gesamte gespeicherte Spur, **Karte vergrößern** öffnet die Vollbildansicht. Fundstellen sind als nach Intensität farbige Punkte sichtbar; ein grüner Haken zeigt vollständig freigegebene Stellen, ein grüner Strich teilweise freigegebene Anzeigegruppen. Ungeklärte gemeinsame Angaben erhalten keinen vollständigen Haken. Diese Markierungen sind kein Versandnachweis; im Stillstand lassen sie sich auf der Startseitenkarte antippen und prüfen. Alle gespeicherten Fundstellen bleiben auf der Karte sichtbar, auch aus früheren Fahrten. **#** bezeichnet eine einzelne Messungsnummer, **×** die Anzahl der Messungen in einer Anzeigegruppe. Antippen öffnet auch überlappende Fundstellen. Zoomabhängige Anzeigegruppen ändern keine gespeicherte Bündelung und helfen, Häufungen zu erkennen.

Die Fahrtspur beginnt mit einer bewusst gestarteten Aufzeichnung. GPS-Lücken über 15 Sekunden werden als getrennte Abschnitte dargestellt; unplausible GPS-Sprünge werden verworfen. Nach Fahrtende bleibt die Spur lokal sichtbar. Die nächste bestätigte Fahrt ersetzt diese Spur, nicht die Fundstellen. Die Spur lässt sich separat löschen und wird nicht mit dem CSV-Fundstellenexport weitergegeben.

Kartenanbieter: `https://tile.openstreetmap.org/`, © OpenStreetMap contributors. Karten werden lokal zwischengespeichert, unter Berücksichtigung der Server-Cacheangaben. Es gibt keinen Gebietsdownload oder vorauseilendes Herunterladen von Kacheln. Ohne Internet können bereits gespeicherte Kacheln verfügbar sein; eine vollständige Offline-Karte wird nicht zugesichert. GPS und lokale Aufzeichnung funktionieren unabhängig vom Laden neuer Kartenkacheln.

Für eine spätere breite öffentliche Veröffentlichung ist die aktuelle [OSM-Kachelnutzungsrichtlinie](https://operations.osmfoundation.org/policies/tiles/) zu prüfen und gegebenenfalls ein passender Kartenanbieter einzusetzen. Die Cloud-Prüfung ersetzt keinen Gerätetest der Kartenverbindung.

## Navigation

Die untere Leiste trennt **Fahrt**, **Fundstellen** und **Mehr**. Die Fahrtansicht enthält Karte und Aufzeichnung. Fundstellen erscheinen als kompakte Karten mit **Prüfen & Fotos**, **Löschen** und **Details & Karte**; Filter zeigen alle, offene oder freigegebene Einträge. **Meldung vorbereiten** und **Weitere Aktionen** öffnen Mail, Bündelung und CSV. Im Bereich **Mehr** liegen Messungseinstellungen, Geodaten, Datenverwaltung, Datenschutz und Hilfe. An beiden Karten öffnet **Kartenoptionen** Fahrtspuransicht, Empfindlichkeit, Neukalibrierung und Legende. GPS-Genauigkeit bleibt direkt sichtbar.

## Auf dem Fairphone ausprobieren

**[Android-Testversion 0.14.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.14.0.apk)**

Öffne die heruntergeladene APK auf deinem Android-Handy. Falls Android danach fragt, erlaube die Installation aus dieser Quelle. Die [kurze Installationsanleitung](downloads/INSTALLATION.md) und die [SHA-256-Prüfsummen](downloads/SHA256SUMS) liegen neben der APK. Version 0.14.0 verwendet dieselbe App-ID und Signatur wie die vorherigen Testversionen und ist als Update über die vorhandene Installation vorgesehen. Eine Deinstallation würde die lokalen Daten löschen. Dies ist eine Debug-Testversion; die Korrekturen müssen auf dem Fairphone noch praktisch überprüft werden.

### Neu nach der Fahrt

Ab **0.13.0** öffnet sich nach einer beendeten Fahrt eine überspringbare Kartenprüfung, sofern nahe Paare mit mindestens einer neuen Messung vorhanden sind. Sie vergleicht neue Treffer miteinander und mit älteren Fundstellen bis **einschließlich 5 m**. **Ja, zusammenfassen …** öffnet die gemeinsame Bestätigung samt Konfliktauswahl; **Nein, getrennt lassen** zeigt das nächste Paar. **Prüfung jetzt beenden** und die Zurück-Taste schließen den Ablauf sofort. Es wird nichts automatisch gebündelt, freigegeben oder gelöscht. Bei Fahrtende über die Benachrichtigung wartet die Prüfung auf die nächste sichtbare Haupt- oder Kartenansicht. Die übrigen Vorschläge unter vier Metern bleiben unverändert. [Anleitung und Grenzen](docs/FAHRTENDE-PRUEFUNG.md) · [Bilder der neuen Oberfläche](docs/screenshots/0.13.0/README.md).

### Parks und private Bereiche prüfen

Seit **0.14.0** berücksichtigt die optionale OSM-Ergänzung auch Park-/Gartenflächen sowie ausdrücklich gekennzeichnete private Flächen oder Zugänge. Nach Fahrtende werden betroffene **neue Einzelmessungen** mit Karte und Quellenhinweis zum Prüfen und optionalen Löschen vorgeschlagen. **Behalten und weiter**, Abbrechen und sofortiges Beenden ändern keine Messung. Auch die letzte Löschbestätigung enthält die Karte. Falls ein Hinweis erst später eintrifft, kann die App einmalig eine ergänzende Prüfung anbieten; nach bewusstem Beenden werden keine verspäteten Vorschläge mehr aufgedrängt. Unter **Mehr → Daten verwalten → Park-/Privatbereich prüfen** kannst du vorhandene Hinweise später erneut ansehen.

**Öffentliche Parks können zum Bezirksamt gehören, häufig zum Grünflächenamt.** `access=private` beschreibt privaten Zugang, keine gesicherte Eigentumsauskunft. Die Vorschläge sind keine Zuständigkeitsentscheidung und keine automatische Löschregel. Fehlende OSM-Angaben oder ungenaues GPS begrenzen die Erkennung. [Erkennung und Grenzen](docs/PARK-PRIVAT-PRUEFUNG.md) · [Bilder der neuen Oberfläche](docs/screenshots/0.14.0/README.md).

### Messungen prüfen, löschen und bündeln

**Löschen** steht direkt an jeder Fundstelle in der Liste. Die Bestätigung zeigt einen Kartenausschnitt und die Nummer der betroffenen Messung. Erst nach der Bestätigung wird die einzelne Messung entfernt. Fotos bleiben erhalten, solange sie noch anderen Messungen zugeordnet sind. Bei gebündelten Treffern wählst du zuerst die betroffene Messung; andere Beobachtungen und die Fahrtspur bleiben erhalten. Bereits exportierte Kopien werden nicht zurückgezogen.

**Zusammenfassbar** gruppiert Vorschläge unter vier Metern, auch vor der Freigabe. **Mehrfachtreffer bündeln** öffnet die Prüfung; GPS-Ungenauigkeit kann größer als der Abstand sein. Nach deiner Bestätigung erscheinen sie als gemeinsame Fundstelle in Liste und Karte. Alle Originalmessungen und Freigaben bleiben erhalten; die Bündelung lässt sich auflösen. Du kannst bei ungenauem GPS auch selbst auswählen. Die Vorschläge erscheinen als Vergleichskarten mit Messungsnummern, Status, Messwert und GPS-Genauigkeit. Seit **0.14.0** enthält jeder Vorschlag automatisch einen OSM-Kartenausschnitt mit den nummerierten Messpunkten. Nur sichtbare Karten werden geladen; sie nutzen den vorhandenen Kachelcache. Auf beiden Kartenansichten kannst du über **Kartenoptionen → Punkte auf der Karte bündeln** auswählen; das Punktmenü bietet außerdem **Mit anderem Punkt bündeln**. Bei überlappenden Markierungen öffnet sich eine Auswahl. **Auswahl bündeln** zeigt Nummern und Abstände vor der Bestätigung. Messungsnummern bleiben beim Bearbeiten, Bündeln und Auflösen gleich und stehen auch im CSV-Export und in Mailentwürfen; nicht freigegebene oder abgewählte Messungen werden nicht in die Mail übernommen. Ältere Listeneinträge sind über **Weitere Fundstellen anzeigen** erreichbar.

Seit **0.12.0** gelten **Unebenheitsart, Beschreibung, Fotos, Verkehrsfläche, Oberfläche und zusätzliche Gefährdungsmerkmale gemeinsam für alle Messungen eines bestätigten Bündels**. **Prüfen & Fotos** öffnet direkt die gemeinsame Fundstelle. Gefährdungsmerkmale werden beim Zusammenführen vereinigt; unterschiedliche Notizen bleiben zunächst als gemeinsame Absätze erhalten und können anschließend vereinheitlicht werden. Widersprüchliche bekannte Unebenheitsarten oder manuelle Verkehrsflächen/Oberflächen erfordern eine Auswahl. Jede spätere Bearbeitung gilt für alle Mitglieder. Messungsnummern, Zeit, Koordinaten, Sensorwerte, tatsächliche Höhen und individuelle Versandfreigaben bleiben erhalten. **Einzelmessungen, Höhen und Freigaben ansehen** führt zu den Originalmessungen. Freigaben werden nur auf ausdrückliche Auswahl für alle geändert. Auflösen trennt die Messungen wieder; übernommene gemeinsame Angaben bleiben bestehen. Bestehende Bündel werden beim Update angepasst, widersprüchliche bekannte Arten bleiben bis zur Auswahl erhalten.

Unter **Prüfen & Fotos** lassen sich gemeinsame Bilder auswählen oder über die Kamera aufnehmen. Der Fotospeicher eines Bündels umfasst fünf Plätze je enthaltener Messung; vorhandene Bilder bleiben auch beim späteren Auflösen oberhalb des Einzelmessungslimits erhalten. Jede Bilddatei wird nur einmal verkleinert und ohne ursprüngliche EXIF-Orts-/Zeitangaben gespeichert. Die Galerie ist bei allen Bündelmitgliedern identisch. Originale im Album bleiben erhalten. Prüfe Bildinhalte selbst vor dem Weitergeben.

Seit **0.11.0** wird ein gestalteter **PDF-Bericht mit nummerierter Karte, Fundstellentabelle, ergänzenden Angaben und ausgewählten Fotos** an die Mail angehängt. Die Lagefelder der Tabelle sind mit OpenStreetMap verlinkt. Das kurze, bearbeitbare Anschreiben bittet um Prüfung und Rückmeldung. Bestätigte Wiederholungen erscheinen als eine Fundstelle mit Anzahl, Kalenderwochen und Wertebereichen. Nur ausgewählte freigegebene Beobachtungen werden gemeldet. Ihre gemeinsamen Fundstellenfotos sind einzeln auswählbar und erscheinen je Fundstelle nur einmal, auch wenn sie mehreren Messungen zugeordnet sind. Bis zu 20 Fundstellen je Mail und zehn Fotos / 10 MB je Entwurf sind möglich. **PDF ansehen** öffnet die Vorschau mit Seitenwahl und Zoom direkt in der App. PDF und Mail verwenden dieselbe geprüfte Kopie. Fotos stehen im PDF; zusätzliche einzelne Bildanhänge sind optional. Erst im externen Mailprogramm wird versendet. [PDF-Anleitung](docs/PDF-BERICHT.md) · [PDF-Beispiel mit erfundenen Daten und gekennzeichneter Offline-Kartenansicht](docs/examples/0.11.0/Fundstellenbericht-Beispiel.pdf). Den aktuellen Prüfstand beschreibt die [Installationsanleitung](downloads/INSTALLATION.md). Kamera und Anhänge brauchen noch einen praktischen Fairphone-Test. Anleitung und Grenzen: [BUENDELUNG-FOTOS.md](docs/BUENDELUNG-FOTOS.md).

### Empfindlichkeit und Mailentwürfe

Der Schieberegler zeigt **links kleinere Werte und mehr Treffer, rechts größere Werte und weniger Treffer**. Eine ganze Zahl von 5 bis 100 m/s² lässt sich auch direkt eingeben und über **Übernehmen** anwenden; der Standard bleibt 40.

**Meldung an die Verwaltung vorbereiten** erstellt aus ausgewählten freigegebenen Fundstellen bearbeitbare Mailentwürfe. Automatische Messzeitangaben enthalten ausschließlich die Berliner ISO-Kalenderwoche und das Wochenjahr. Der geografische Bezirksvorschlag lässt sich prüfen und korrigieren; bei mehreren Bezirken gibt es getrennte Entwürfe. Zwölf auf offiziellen Berlin.de-Seiten geprüfte Kontaktadressen sind hinterlegt. Der gebündelte Grenzdatensatz stammt allerdings aus 2013 und ist ausdrücklich ein Vorschlag; Grenznähe und besondere Straßen erfordern eigene Prüfung.

Die Mail erläutert seit **0.13.0** das Ziel, das Bezirksamt beim Auffinden und Prüfen von Schlaglöchern und Unebenheiten zu unterstützen. Sie erklärt Smartphone-Methode, Einfluss von Geschwindigkeit/Bereifung/Halterung, mögliche Fehlalarme, GPS-Ungenauigkeit und fehlende automatische Höhen- oder Rechtsbewertung. Ein optionales, lokal gespeichertes Feld ergänzt die persönliche Vorstellung; Namen oder Lebensläufe werden nicht erfunden. Ein kurzer Hinweis auf Bodenwelle bleibt wählbar. Eigene Notizen bleiben zunächst aus. Die detaillierten Daten stehen im PDF-Bericht, ausgewählte bereinigte Fotos in dessen Fotodokumentation. Fahrtspur und Rohdatenexporte bleiben ausgeschlossen. Fehlende Kartenkacheln werden im Bericht klar als unvollständiger Straßenhintergrund gekennzeichnet; vorhandene Offline-Kacheln werden wiederverwendet. **Im Mailprogramm öffnen** übergibt ihn an das Handy-Mailprogramm, in dem du selbst versendest. Dokumentation, Quellen und Grenzen: [MAIL-MELDUNGEN.md](docs/MAIL-MELDUNGEN.md).

### Klassifizierung und OSM-Ergänzung

- Acht Unebenheitsarten: Wurzelaufbruch, Schlagloch, Querrille, Längsrille, Pflasterversatz, langgezogene Bodenwelle, unebener Straßenabschnitt und sonstige Unebenheit. Alte allgemeine Angaben bleiben nachvollziehbar erhalten.
- Fünf farbige Intensitätsklassen mit getrennten Grundlagen: selbst vor Ort gemessene und bestätigte Millimeterwerte verwenden die projektinternen Höhenklassen; Sensorwerte erhalten ausschließlich eine als vorläufig gekennzeichnete Beschleunigungsskala. Es gibt keine automatische Millimeterumrechnung und keine gesetzlichen Grenzwerte.
- Fünf unabhängige Gefährdungsmerkmale, optionale gemessene Höhe und manuelle Verkehrsflächen-/Oberflächenkorrekturen liegen in aufklappbaren Bereichen der Bearbeitung. Gefährdungsmerkmale verändern keine Messwerte.
- Verkehrsfläche und Oberfläche werden nach Möglichkeit aus OSM-Weggeometrien und Tags ergänzt. Die Suche berücksichtigt Wege bis 50 m Abstand. Die Abfrage verwendet den ursprünglichen GPS-Mittelpunkt. Bei mehreren plausiblen Wegen werden übereinstimmende Angaben als unsicherer Gebietsvorschlag übernommen. Bei unterscheidbaren Kandidaten kann der bestbewertete Weg einen ausdrücklich unsicheren Vorschlag liefern; gleichwertige Widersprüche und fehlende Belagsangaben bleiben unbekannt; eine Benutzungspflicht wird nicht aus einem bloßen Radweg-Tag abgeleitet. Allgemeinere Angaben wie „Pflaster (Material unbekannt)“ oder „Straße mit Radverkehrsanlage (Zuordnung offen)“ bleiben nutzbar, ohne Details zu erfinden. OSM-Vorschläge enthalten Quelle, Originaltags, Zeitpunkt und Sicherheit; manuelle Korrekturen bleiben geschützt.
- Die abschaltbare OSM-Ergänzung fragt über HTTPS `overpass-api.de` nach dem Gebiet um Fundstellen. Der Anbieter sieht Ausschnitt und IP-Adresse, keine Notizen oder Sensorwerte. Begrenzte Abfragen und ein lokaler, auch über Suchrastergrenzen nutzbarer Cache reduzieren Netzverkehr. **OSM-Vorschlag neu abfragen** und **OSM-Abrufdiagnose** stehen in der Bearbeitung unter **Verkehrsfläche und Oberfläche prüfen** bereit. Die Diagnose unterscheidet HTTP-Fehler, fehlende Netzverbindung, Cache und Zuordnung. Ein Internet-fähiges Netz braucht keine zusätzliche Android-Validierung. Zurückgestellte Abfragen werden bei geöffneter Haupt- oder Kartenansicht oder während einer Aufzeichnung nach Ablauf der Wartezeit nachgeholt; Aufzeichnung bleibt auch offline verfügbar und fehlende Angaben können später ergänzt werden.

Die [Dokumentation zu Klassifizierung und OSM](docs/KLASSIFIZIERUNG-OSM.md) beschreibt Kategorien, Testgrenzen, räumliche Zuordnung, Export und verbleibende Einschränkungen. **Der Live-Overpass-Abruf ist aus dieser Cloud durch den Proxy blockiert und muss auf dem Fairphone geprüft werden.** Die Logik ist mit kontrollierten OSM-Antworten getestet. Die Klassifizierungs- und OSM-Logik ist automatisiert geprüft; die jeweils aktuelle Gesamtprüfung steht in der [Installationsanleitung](downloads/INSTALLATION.md).

### Erkennung und Positionskorrektur

Nach Fahrtbeginn zwei Sekunden im Stand ruhig halten, bis **Kalibriert** erscheint. Die Erkennung verwendet ausschließlich die auf die kalibrierte Vertikale projizierte Beschleunigung nach Abzug der Ruheschwerkraft. Androids Lagesensor führt, sofern verfügbar, die Orientierung mit; die Beschleunigung beim Fahren passt die Achse nicht mehr laufend an. **Kartenoptionen → Im Stand neu kalibrieren** steht während der Fahrt an der Karte zur Verfügung.

Anhaltende Schwingungen werden nach mindestens drei Sekunden und zehn geschätzten Metern als **Unebener Straßenabschnitt** erfasst, mit Anfang, Ende und geschätzter Länge. Kandidaten innerhalb desselben Abschnitts erscheinen nicht zusätzlich als Einzelpunkte. Eine orange Verbindung markiert die ungefähren Endpunkte. [Algorithmus, Bedienung und Grenzen](docs/KALIBRIERUNG-ABSCHNITTE.md).

Auf beiden Karten öffnen Punkte und Abschnittslinien ein Menü zum Prüfen, Ergänzen von Fotos und Löschen.

- Die feste Auslöseschwelle startet bei **40 m/s²**. Gemeint ist der Betrag der gefilterten vertikalen Sensorreaktion nach Abzug der geschätzten Schwerkraft, nicht die Höhe einer Bodenwelle.
- **Kartenoptionen → Empfindlichkeit einstellen** an der Karte öffnet einen Schieberegler von 5 bis 100 m/s²: links mehr Treffer, rechts weniger Treffer. Die Einstellung wird gespeichert und gilt auch während einer laufenden Fahrt sofort für neue Messungen. Jede Fundstelle speichert ihre damalige Schwelle. Bediene das Telefon nur im sicheren Stand.
- Automatische Fundstellen verwenden den Zeitpunkt der stärksten Reaktion im 250-ms-Erkennungsfenster. Aus Geschwindigkeit, GPS-Fahrtrichtung und dem zeitlichen Abstand zur GPS-Messung wird die Position für diesen Zeitpunkt geschätzt. Eine ältere GPS-Messung wird entlang der Fahrtrichtung verschoben, eine spätere zurückgerechnet.
- Fehlen brauchbare Bewegungsdaten, bleibt die ursprüngliche GPS-Position erhalten. Die Korrektur setzt gleichbleibende Geschwindigkeit und Richtung voraus und beseitigt keine GPS-Messfehler. Ursprüngliche Koordinaten und Korrektur bleiben in Messdetails und Export nachvollziehbar. Bestehende Fundstellen bleiben unverändert.

Eine belastbare Höhenmessung braucht zusätzliche Referenzmessungen und einen Kalibrierungsmodus; die bisherigen Spitzenwerte reichen dafür nicht. Siehe [Höhenmessung und benötigte Daten](docs/HOEHENMESSUNG.md).

Installierbare Testdatei nach dem Build: `app/build/outputs/apk/debug/app-debug.apk`.

Die Anleitung für Installation, Vergleichsfahrten und Freigabe steht in [FAHRTEST.md](docs/FAHRTEST.md). Für sachliche Meldungen gibt es eine [Berliner Meldungsvorlage mit offenem Recherchestand](docs/BERLIN-MELDUNGEN.md). Umfang und nächste Schritte beschreibt der [Entwicklungsplan](docs/ENTWICKLUNGSPLAN.md).

## Entwickeln

- Android Studio oder JDK 17–21;
- Android SDK Platform 35 und Build Tools 35.0.0;
- Gradle 8.11.1 über den beigefügten Wrapper;
- Android 8.0 / API 26 oder neuer auf dem Testgerät.

Öffne dieses Verzeichnis in Android Studio oder setze `ANDROID_HOME` auf dein SDK-Verzeichnis. Alternativ verwende eine lokale, ignorierte `local.properties` mit `sdk.dir=...`.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Installation bei aktiviertem USB-Debugging und angeschlossenem Gerät:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Tests mit synthetischen Sensordaten überprüfen unter anderem Mehrfachschwingungen, ruhige Fahrten, fehlende Messbedingungen und den Ausschluss ungeprüfter Daten beim Export. Die Erkennung ist experimentell; die Funktionsprüfung auf einem realen Android-Gerät und Fahrtests sind weiterhin erforderlich.

In dieser Linux-Cloud sind JDK, Gradle und Android SDK unter `/workspace/.bodenwelle-tools` installiert. Der folgende Einstieg aktiviert diese Werkzeuge sowie den vorhandenen Proxy und den System-Zertifikatsspeicher, ohne Zugangsdaten auszugeben:

```sh
python3 tools/setup-cloud.py
python3 tools/cloud-gradle.py :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Der Bootstrap prüft die heruntergeladenen Werkzeuge anhand festgelegter Hersteller-Prüfsummen und erhält eine vorhandene `local.properties`. In der Cloud werden keine Geräteprozesse gestartet; der APK-Test auf dem Fairphone erfolgt separat.

## Datenmodell

Lokale SQLite-Fundstellen enthalten zusätzlich strukturierte Klassifizierungs- und OSM-Metadaten einschließlich manueller Herkunftskennzeichen und optionaler bestätigter Höhenmessung. Ein separater, begrenzter Cache speichert öffentliche OSM-Ausschnitte. Sie enthalten außerdem Ereigniszeit, Koordinaten, GPS-Genauigkeit, Zeitabstand zwischen GPS-Fix und Ereignis, gegebenenfalls Geschwindigkeit, Sensor-Spitzenwert und Erkennungsschwelle sowie Quelle, Kategorie, Notiz und Freigabestatus. Die zuletzt gestartete Fahrt wird zusätzlich als lokale GPS-Spur mit getrennten Abschnitten gespeichert. Die Datenbankmigration erhält vorhandene Fundstellen und Fahrtspuren. Schema 5 speichert zusätzlich eine optionale manuelle Bezirkszuordnung für Meldungen. Schema 6 ergänzt bestätigte Bündelungen und Fotometadaten. Schema 7 ergänzt gemeinsame Fotozuordnungen und übernimmt gemeinsame Angaben in bestehende Bündel; widersprüchliche manuelle Angaben bleiben bis zur Auswahl erhalten. Ursprüngliche Messungen werden nicht entfernt. Neue automatische Einträge enthalten zusätzlich ursprüngliche GPS-Koordinaten, Fahrtrichtung, geschätzte Verschiebung und Korrekturmethode. Die Erkennungsversion wird je Fundstelle gespeichert. Rohsensorzeitreihen werden nicht gespeichert. Manuelle Einträge enthalten keine erfundenen Sensorwerte. Der Fundstellenexport verwendet UTC-Zeitpunkte und dokumentierte SI-Einheiten beziehungsweise km/h und enthält keine Fahrtspur.

## Verwendete Kartenbibliothek

osmdroid 6.1.20 ist unter der Apache-Lizenz 2.0 lizenziert. Eine Kopie der Lizenz liegt unter `app/src/main/assets/licenses/osmdroid-apache-2.0.txt`. Die OSM-Datenattribution bleibt direkt unter jeder Kartenansicht sichtbar und verlinkt die [OSM-Copyrightseite](https://www.openstreetmap.org/copyright).

### Positionsangaben im CSV-Export

Die ersten 13 Spalten bleiben erhalten. `latitude` und `longitude` sind bei korrigierten neuen Fundstellen die geschätzten Ereigniskoordinaten; `gps_latitude` und `gps_longitude` bewahren die ursprüngliche Messung. `gps_accuracy_m` gilt weiterhin für diese ursprüngliche GPS-Messung, nicht als garantierte Genauigkeit der korrigierten Position.

Ab Version 0.2.2 ist `gps_fix_age_ms` der vorzeichenbehaftete Abstand **Ereigniszeit minus GPS-Messzeit**: positiv bei einer älteren, negativ bei einer späteren Messung. `bearing_deg` ist die GPS-Fahrtrichtung in Grad relativ zu Nord; `position_distance_m` enthält die Verschiebung mit Vorzeichen, `position_method` die Methode oder den Grund für unveränderte Koordinaten. `course_speed_time` bezeichnet die Projektion, `legacy_gps` frühere Fundstellen und `manual_gps` manuelle Markierungen. Mehr als fünf Sekunden Zeitabstand oder eine angegebene Richtungsunsicherheit über 45° führen zu keiner Projektion. Fehlende Werte bleiben leer. Frühere Einträge werden nicht nachträglich korrigiert, weil Richtung und Spitzenzeitpunkt fehlen.

AndroidX ExifInterface 1.3.7 (Apache 2.0) liest die Orientierung importierter Fotos. Die Lizenzkopie liegt unter `app/src/main/assets/licenses/androidx-exifinterface-apache-2.0.txt`.

PDF-Berichte und die interne Vorschau verwenden PDFBox-Android 2.0.27.0 (Apache 2.0); unbenötigte Verschlüsselungs-/Signaturmodule sind ausgeschlossen. Eine Lizenzkopie liegt unter `app/src/main/assets/licenses/pdfbox-apache-2.0.txt`. Die Noto-Sans-Schriften unter `app/src/main/assets/fonts/` stehen unter SIL Open Font License 1.1, siehe dort `LICENSE.txt`. Sie werden für eine verlässliche, durchsuchbare Darstellung in PDFs eingebettet.
