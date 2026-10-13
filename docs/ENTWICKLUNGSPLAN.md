# Entwicklungsrahmen

## Ausgangspunkt

Android, zunächst Fairphone 4 am Lenker, Valkental-Handyhalterung und Trekking N8. Einsatz in Berlin. Das Fahrrad- und Halterungsmodell werden als Angaben des Nutzers übernommen; Produktdetails und Eigenschaften wie Dämpfung wurden nicht unabhängig verifiziert.

Die App soll später öffentlich nutzbar sein. Jede Person behält die Kontrolle über ihre eigenen Aufzeichnungen und versendet nur nach eigener Prüfung. Aufzeichnungen werden lokal ohne Backend gespeichert; OSM-Kartenkacheln werden über das Internet geladen.

## Version 0.2: persönlicher Fahrtest mit integrierter Karte

Implementiert sind:

- Beschleunigungserfassung mit geschätzter Schwerkraftrichtung und experimenteller Stoßerkennung;
- GPS-Koordinaten, Positionsgenauigkeit, Alter des GPS-Fixes und verfügbare Geschwindigkeit;
- Aufzeichnung in einem Android-Vordergrunddienst einschließlich ausgeschaltetem Display;
- manuelle Markierungen während einer gestarteten Fahrt mit ausreichender Position;
- persistente lokale SQLite-Ablage;
- Kategorie, Notiz, Freigabe und Löschen nach der Fahrt;
- Einstiegshilfe, aufklappbare Messdetails und eine native OSM-Karte mit eigener Position, Standortnachführung, Fundstellen und Vollbildansicht;
- lokale Spur der zuletzt gestarteten Fahrt, inklusive Darstellung ohne Verbindungslinien über GPS-Lücken;
- CSV-Export ausschließlich freigegebener Einträge über die Android-Dateiauswahl.

Automatische Aufzeichnung setzt GPS-Fix-Alter ≤ 5 s, Genauigkeitsangabe ≤ 25 m und Geschwindigkeit von 1,4 bis 20 m/s voraus. Ist eine Geschwindigkeitsgenauigkeit vorhanden, darf sie höchstens 1,5 m/s betragen. Der Betrag der gefilterten vertikalen Sensorreaktion muss die eingestellte Schwelle erreichen: standardmäßig 40 m/s², per Schieberegler 5 bis 100 m/s². Das sind technische Testwerte, keine rechtlichen Grenzwerte. Die Einstellung wird lokal gespeichert und gilt sofort; pro Treffer bleibt die verwendete Schwelle erhalten. Ein Treffer sammelt die Spitzenreaktion über 250 ms; gespeichert wird der Zeitpunkt dieser Spitze.

Neue automatische Positionen werden über eine geodätische Projektion aus GPS-Koordinaten, GPS-Fahrtrichtung, Geschwindigkeit und vorzeichenbehaftetem Zeitabstand zum Sensorereignis geschätzt. Ältere Messungen werden vorwärts, spätere rückwärts projiziert. Bei fehlenden oder unbrauchbaren Bewegungsdaten, mehr als fünf Sekunden Zeitabstand oder angegebener Richtungsunsicherheit über 45° bleiben die ursprünglichen Koordinaten bestehen. Ursprüngliche GPS-Position, Verschiebung, Methode und Erkennungsversion werden gespeichert und exportiert. GPS-Fehler und die Annahme gleichbleibender Bewegung begrenzen die Aussagekraft; die Position ist keine vermessene Schadensstelle. Die Migration auf Datenbankschema 3 erhält bisherige Fundstellen und Spur. Bisherige Einträge werden nicht nachträglich korrigiert.

Die aktuelle App speichert keine Rohsensorzeitreihen und berechnet keine Bodenwellenhöhe. Anforderungen an einen späteren Kalibrierungsmodus beschreibt [HOEHENMESSUNG.md](HOEHENMESSUNG.md).

## Version 0.3: Klassifizierung und Geodaten

Sieben stabile Unebenheitskategorien ersetzen die früheren Angaben. Eindeutige alte Kategorien werden zugeordnet; bei allgemeinen oder unbekannten Angaben bleibt der ursprüngliche Text zusätzlich erhalten. Intensität verwendet entweder ausdrücklich bestätigte Vor-Ort-Höhenmessungen in Millimetern oder eine getrennte vorläufige Sensorreaktionsskala. Gefährdungsmerkmale werden unabhängig gespeichert.

Eine begrenzte Overpass-Anbindung ergänzt OSM-Verkehrsfläche und Oberfläche mit Cache und vorsichtiger räumlicher Zuordnung. Mehrdeutige Wege bleiben unbekannt. Manuelle Korrekturen werden bei späteren Antworten erhalten. Die neue Bearbeitung verwendet aufklappbare optionale Bereiche; Karte und Fundstellen zeigen Intensität farblich und textlich. Der Export führt Messmethode, Herkunft und OSM-Attribution mit. Details und Einschränkungen: [KLASSIFIZIERUNG-OSM.md](KLASSIFIZIERUNG-OSM.md).

## Version 0.4: Meldung über das Mailprogramm

Der Sensitivitätsregler steigt numerisch nach rechts; zusätzlich ist eine Zahleneingabe vorhanden. Aus ausgewählten freigegebenen Einträgen entstehen bearbeitbare Mailentwürfe mit ISO-Kalenderwoche statt genauem Messzeitpunkt. Bezirksvorschläge nutzen einen gebündelten älteren Polygonbestand (2013); zwölf aktuelle, auf Berlin.de veröffentlichte Mailkontakte sind mit Quellen hinterlegt. Manuelle Korrektur, Aufteilung nach Bezirk und Übergabe per `ACTION_SENDTO` sind umgesetzt. Die App sendet keine Nachricht selbst und bestätigt keinen Versand. Dokumentation: [MAIL-MELDUNGEN.md](MAIL-MELDUNGEN.md).

## Version 0.5: Bündelung und Fotos

Bestätigte Bündelungen verknüpfen ursprüngliche Beobachtungen und bleiben reversibel. Nähe gleicher freigegebener Kategorien liefert Vorschläge, die eine Person bestätigen muss. Fotos werden über Android-Bildauswahl beziehungsweise externe Kamera ergänzt, lokal neu kodiert und ohne ursprüngliche EXIF-Metadaten abgelegt. Mails zählen bestätigte Fundstellen statt Einzelereignisse und übergeben ausgewählte Bilder mit begrenzten Leserechten. Anleitung und Grenzen: [BUENDELUNG-FOTOS.md](BUENDELUNG-FOTOS.md).

## Nächste Schritte nach echten Fahrdaten

1. Bekannte Stellen und glatte Vergleichsabschnitte erfassen; Fehlalarme und Auslassungen nachvollziehen. Halterung, Reifendruck und Geschwindigkeit berücksichtigen.
2. Die integrierte Karte im Fahrtest prüfen; Zusammenführung benachbarter Fundstellen und Fotoanhänge hinzufügen; eigene Messpositionen korrigierbar machen.
3. Aktuelle amtliche Bezirksgrenzen und konkrete Straßenbaulast verifizieren; Foto- und Bündelungsfunktion im Fahrtest prüfen und Bearbeitungsstatus entwickeln.
4. Unterschiedliche Android-Geräte, Berechtigungsverläufe, Batterieeinstellungen und längere Fahrten testen.
5. Erst danach: öffentliche Testversion, signiertes Release, Datenschutzinformationen, barrierearme Bedienung, Übersetzungen und Wartungsprozess.

## Spätere Erweiterungen

Radwegbreiten müssen vor Ort verlässlich gemessen und in ihrem rechtlichen Kontext bewertet werden. Eine manuelle Notiz kann zunächst auf eine Engstelle hinweisen.

Kennzeichenerkennung und Falschparker-Meldungen sind ein separates Vorhaben: Meldeweg, Rechtsgrundlage, betroffene Personen, Speicherung von Bildern und Kennzeichen sowie die Freigabe müssen vor Umsetzung konkret geklärt werden. Diese Version enthält keine Kamera- oder Kennzeichenfunktionen.

## Verifikation

JVM-Tests prüfen synthetische Signalfolgen, GPS-/Geschwindigkeitsbedingungen, Spurunterbrechungen und Exportfreigaben. Robolectric-Tests prüfen im simulierten Android-Framework die Datenbankmigration, lokale Spurerhaltung, den Ausschluss der Spur beim Fundstellenexport und den Start der Kartenansichten. Der Android-Build und Lint prüfen die technische Kompilierbarkeit und statische Kompatibilität. Diese Prüfungen ersetzen weder einen Android-Gerätetest noch eine reale Fahrt oder den Live-Kachelabruf. Für eine Bewertung der Erkennungsqualität sind unabhängige Vor-Ort-Beobachtungen erforderlich.
