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

Automatische Aufzeichnung setzt GPS-Fix-Alter ≤ 5 s, Genauigkeitsangabe ≤ 25 m und Geschwindigkeit von 1,4 bis 20 m/s voraus. Ist eine Geschwindigkeitsgenauigkeit vorhanden, darf sie höchstens 1,5 m/s betragen. Die vertikale Sensorreaktion muss eine adaptive Schwelle von mindestens 3 m/s² überschreiten. Das sind technische Ausgangswerte, keine rechtlichen Grenzwerte. Ein Treffer sammelt die Spitzenreaktion über 250 ms; der gespeicherte Ereigniszeitpunkt liegt am Ende dieses Fensters. Der GPS-Fix kann einige Sekunden vor dem Ereignis liegen, wodurch zusätzlich zur GPS-Genauigkeit eine Verschiebung entlang der Strecke entstehen kann.

## Nächste Schritte nach echten Fahrdaten

1. Bekannte Stellen und glatte Vergleichsabschnitte erfassen; Fehlalarme und Auslassungen nachvollziehen. Halterung, Reifendruck und Geschwindigkeit berücksichtigen.
2. Die integrierte Karte im Fahrtest prüfen; Zusammenführung benachbarter Fundstellen und Fotoanhänge hinzufügen; eigene Messpositionen korrigierbar machen.
3. Berliner Zuständigkeiten und aktuelle Meldewege verifizieren; menschenlesbare Einzelberichte und Bearbeitungsstatus entwickeln.
4. Unterschiedliche Android-Geräte, Berechtigungsverläufe, Batterieeinstellungen und längere Fahrten testen.
5. Erst danach: öffentliche Testversion, signiertes Release, Datenschutzinformationen, barrierearme Bedienung, Übersetzungen und Wartungsprozess.

## Spätere Erweiterungen

Radwegbreiten müssen vor Ort verlässlich gemessen und in ihrem rechtlichen Kontext bewertet werden. Eine manuelle Notiz kann zunächst auf eine Engstelle hinweisen.

Kennzeichenerkennung und Falschparker-Meldungen sind ein separates Vorhaben: Meldeweg, Rechtsgrundlage, betroffene Personen, Speicherung von Bildern und Kennzeichen sowie die Freigabe müssen vor Umsetzung konkret geklärt werden. Diese Version enthält keine Kamera- oder Kennzeichenfunktionen.

## Verifikation

JVM-Tests prüfen synthetische Signalfolgen, GPS-/Geschwindigkeitsbedingungen, Spurunterbrechungen und Exportfreigaben. Robolectric-Tests prüfen im simulierten Android-Framework die Datenbankmigration, lokale Spurerhaltung, den Ausschluss der Spur beim Fundstellenexport und den Start der Kartenansichten. Der Android-Build und Lint prüfen die technische Kompilierbarkeit und statische Kompatibilität. Diese Prüfungen ersetzen weder einen Android-Gerätetest noch eine reale Fahrt oder den Live-Kachelabruf. Für eine Bewertung der Erkennungsqualität sind unabhängige Vor-Ort-Beobachtungen erforderlich.
