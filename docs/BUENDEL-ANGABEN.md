# Bündelangaben vergleichen und sicher speichern · 0.16.0

## Vergleich nur bei unterschiedlichen gemeinsamen Angaben

App und Browser prüfen vor dem Bündeln Unebenheitsart, Beschreibung, Gefährdungsmerkmale, Verkehrsfläche, Oberfläche und Fotozuordnungen. **Stimmen sie überein, bleibt es bei der kurzen Bestätigung.** Unterschiede bei Sensorwerten, Koordinaten, Zeitpunkten, gemessenen Höhen, Bezirken oder Freigaben öffnen keine zusätzliche Vergleichsansicht: Diese Angaben bleiben individuell erhalten.

Bei abweichenden gemeinsamen Angaben zeigt die Bestätigung das vorgeschlagene Ergebnis und die bisherigen Angaben mit Messungsnummern. **Bisherige Angaben vergleichen** öffnet den Quellenvergleich. Der Ausgangspunkt ist wählbar:

- **Vorschlag:** vorhandene manuelle Verkehrsflächen-/Oberflächenangaben haben Vorrang vor automatischen Vorschlägen. Eindeutige bekannte Unebenheitsarten werden übernommen. Unterschiedliche Notizen werden als gemeinsame Absätze erhalten, Gefährdungsmerkmale vereinigt. Widersprüchliche bekannte Arten und manuelle Belagsangaben verlangen eine Auswahl.
- **Angaben von Messung #N übernehmen:** Art, Beschreibung, Gefährdungsmerkmale und manuelle Belagsangaben dieser Quelle dienen als Ausgangspunkt. Damit können andere bisherige Beschreibungen und Gefährdungsmerkmale bewusst ersetzt werden. Vor der Bestätigung bleiben alle Felder anpassbar.

Die vollständigen gemeinsamen Werte sind sichtbar und bearbeitbar, auch wenn nur ein einzelnes Feld abweicht. Die Vorschau zeigt das Ergebnis für alle enthaltenen Messungen. Bei automatischen Belagsangaben gilt weiterhin die gemeinsame OSM-Auswertung; widersprüchliche automatische Vorschläge ergeben **Unbekannt**. Der Browser zeigt den Exportstand, die App den aktuell gespeicherten Stand.

**Fotos werden immer vereinigt**, auch bei Auswahl einer Quelle. Gleiche vorhandene Fotozuordnungen zählen nur einmal. Beschreibung, Art, Belag, Gefährdungsmerkmale und Galerie gelten anschließend bei allen Bündelmitgliedern. Höhen, Sensorwerte, ursprüngliche Positionen, Zeitpunkte, Bezirke und individuelle Freigaben bleiben erhalten.

Erst **Bündeln & übernehmen** bestätigt das Ergebnis. Abbrechen verändert keine Bündelung oder Beschreibung. Haben sich die ausgewählten Handydaten während der Prüfung geändert, verlangt die App einen erneuten Vergleich. Das verhindert das Überschreiben neuerer Angaben.

## Änderungen im Browser übernehmen und als Datei sichern

Der Bearbeitungsbereich enthält jetzt einen sichtbaren **Änderungen übernehmen**-Button mit Hinweis auf die Anzahl der betroffenen Messungen. Bei einem Bündel gilt die Bearbeitung der gemeinsamen Felder für sämtliche Mitglieder. Die Einzelmessungsfelder behalten ihre getrennte Bedeutung.

Eingaben bleiben zunächst im Formular. **Änderungen übernehmen** übernimmt sie als einen Bearbeitungsschritt in den Arbeitsstand. Auch beim Wechsel zu einer anderen Stelle, vor einer Bündelung und beim Dateispeichern werden gültige offene Eingaben übernommen. Ein noch fokussiertes Textfeld geht dadurch nicht verloren. Ungültige oder unvollständige bestätigte Höhen müssen zuerst korrigiert werden.

**Bearbeitete Datei speichern** sichert danach den gesamten Arbeitsstand als neue HTML-Datei. Das Übernehmen einer Stelle allein schreibt noch keine Datei auf die Festplatte. Der Status unterscheidet offene Formulareingaben vom noch nicht als Datei gesicherten Arbeitsstand. Den angebotenen Download im Downloadordner prüfen und für den Rückimport diese neue Datei verwenden.

**Rückgängig / Wiederholen** behandelt eine Übernahme als einen gemeinsamen Schritt. Bei älteren, noch uneinheitlichen Bündeln verlangt **Gemeinsame Angaben abgleichen** eine ausdrückliche Vereinheitlichung, bevor normale gemeinsame Bearbeitung möglich ist.

In der App schreibt **Für alle speichern** die gemeinsame Bearbeitung direkt in den lokalen Speicher. Für die spätere Browserbearbeitung wird wie bisher eine Projektdatei exportiert.

## Eindeutige Dateinamen

Neue Handy-Projekte, extern gespeicherte Importsicherungen und Browser-Bearbeitungsdateien erhalten jeweils **Exportdatum und -uhrzeit in Berliner Ortszeit plus eine neue UUID**. Beispiele:

```text
Bodenwelle-Projekt-2026-11-11_18-42-07-<UUID>.html
Bodenwelle-Bearbeitet-2026-11-11_19-03-12-<UUID>.html
Bodenwelle-Sicherung-2026-11-11_19-15-30-<UUID>.html
```

Auch unmittelbar aufeinanderfolgende Speichervorgänge schlagen verschiedene Namen vor. Die Namen wachsen beim wiederholten Bearbeiten nicht durch zusätzliche „bearbeitet“-Anhänge an. Der Zeitstempel beschreibt den Export, nicht den Zeitpunkt einzelner Messungen. Das private Projektformat und die Kalenderwochen-Angaben im gesonderten Verwaltungsbericht bleiben unverändert.

## Vorhandene Projekte öffnen

Den [Browsereditor 0.16.0 als ZIP herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-0.16.0.zip), entpacken, **Bodenwelle-Browser.html** öffnen und über **Projektdatei öffnen** die bisherige gespeicherte HTML-Datei auswählen. Ihre Messungen, Fotos und Bearbeitungen bleiben erhalten. Ein erneuter Handyexport ist dafür nicht erforderlich. App 0.16.0 als Update installieren, ohne vorher zu deinstallieren.

Die OSM-basierte OpenFreeMap-Karte aus 0.15.1 bleibt enthalten. Der Straßenhintergrund braucht Internet und WebGL; Daten und Bearbeitung bleiben ohne Hintergrund verfügbar.

## Prüfung und Grenzen

390 automatisierte Android-Prüfungen decken unter anderem die kurze Bestätigung trotz unterschiedlicher Einzelhöhen/Freigaben, Quellenwahl, Abbruch, gemeinsame Fotoübernahme, Schutz vor veralteten Vorschauen und eindeutige Android-Dateinamen ab. Die Browserprüfungen testen das Übernehmen für alle Mitglieder, letzte Texteingaben ohne Fokuswechsel, Download und erneutes Öffnen, Quellenwahl, Abbruch, gleiche Angaben, uneinheitliche Altbündel und neue eindeutige Dateinamen. Eine tatsächlich gespeicherte Datei wird in Android zurückimportiert und dort auf gemeinsame Beschreibung, Belag und Gefährdungsmerkmale sowie erhaltene neue Handymessungen, Rohwerte und Spur geprüft.

Direkte `file://`-Navigation und die Live-Verbindung zu OpenFreeMap sind in der Cloud durch Browser-/Netzwerkrichtlinien blockiert. Die Browserprüfungen rendern denselben HTML-Inhalt als Testdokument; die Kartenprüfung verwendet den echten WebGL-Renderer mit künstlichen Antworten. Reales Fairphone, Firefox und Safari bleiben praktische Prüfungen. [Prüfkommandos](../tools/browser/README.md).
