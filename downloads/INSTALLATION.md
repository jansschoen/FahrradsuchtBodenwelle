# Bodenwelle 0.16.0 – Bündelvergleich, Übernehmen und eindeutige Dateinamen

[APK 0.16.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.16.0.apk). Als Update installieren, **nicht vorher deinstallieren**. Signatur, App-ID, Datenbankschema 9 und Projektformat 1 bleiben gleich. Android 8.0 oder neuer wird benötigt.

## Im Browser gemeinsame Angaben speichern

Den [Browsereditor 0.16.0 als ZIP herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-0.16.0.zip), entpacken und **Bodenwelle-Browser.html** öffnen. Über **Projektdatei öffnen** die bisherige zuletzt gespeicherte HTML-Datei auswählen. Ein neuer Handyexport ist dafür nicht erforderlich.

Im Bearbeitungsbereich gilt **Änderungen übernehmen** für die gemeinsamen Felder aller Bündelmitglieder. Höhen, einzelne Freigaben und Bezirke bleiben getrennt. Der Status zeigt, ob noch Eingaben im Formular stehen oder der Arbeitsstand noch als Datei gesichert werden muss. **Bearbeitete Datei speichern** sichert den gesamten Arbeitsstand als neue HTML-Datei; offene gültige Formulareingaben werden dabei übernommen. Den angebotenen Download prüfen und für den Rückimport diese neue Datei verwenden. Die ursprüngliche Datei bleibt eine eigene Kopie.

## Vergleich nur bei Unterschieden

Stimmen gemeinsame Angaben und Fotos überein, bleibt es in App und Browser bei der kurzen Bestätigung. Abweichende Sensorwerte, Koordinaten, Zeiten, Höhen oder Freigaben lösen keine ausführliche Vergleichsansicht aus.

Bei abweichenden gemeinsamen Angaben zeigt die Bestätigung die bisherigen Werte und das gemeinsame Ergebnis. Man kann den Vorschlag verwenden oder die Angaben einer Messung als Ausgangspunkt wählen und alle gemeinsamen Eigenschaften anpassen. Beschreibung, Unebenheitsart, Verkehrsfläche, Oberfläche und Gefährdungsmerkmale gelten danach für alle Mitglieder. Fotos werden immer vereinigt. Erst **Bündeln & übernehmen** speichert die Bündelung. Abbrechen ändert nichts; veraltete Vorschauen werden abgewiesen.

In der App speichert **Für alle speichern** eine gemeinsame Bearbeitung direkt lokal. [Ausführliche Anleitung](../docs/BUENDEL-ANGABEN.md) · [Bilder](../docs/screenshots/0.16.0/README.md).

## Eindeutige Dateinamen

Handy-Projekte, extern gespeicherte Importsicherungen und Browserdateien erhalten jeweils **Exportdatum, Uhrzeit in Europe/Berlin und eine neue UUID**. Auch unmittelbar aufeinanderfolgende Exporte haben unterschiedliche Namen. Beispiele: `Bodenwelle-Projekt-2026-11-11_18-42-07-<UUID>.html` und `Bodenwelle-Bearbeitet-2026-11-11_19-03-12-<UUID>.html`. Der Zeitstempel bezeichnet den Export, nicht die einzelnen Messungen.

## Daten und Karte

Der Browser lädt weiterhin OpenFreeMap auf OSM-Basis. Kartenbibliothek und Gestaltung sind lokal eingebettet; nur Kartendaten und Kartenbeschriftungen werden angefordert. Der Anbieter sieht Ausschnitt und IP-Adresse, keine Messbeschreibungen, Fotos oder Rohwerte. Für den Straßenhintergrund sind Internet und WebGL nötig; Punkte, Spur und Bearbeitung bleiben auf einem Raster verfügbar.

Neue Handymessungen und die aktuelle Spur bleiben beim Rückimport erhalten. Konflikte verlangen eine Auswahl; vor der bestätigten Übernahme entsteht eine lokale Sicherung. Für weitere Bearbeitungsrunden nach einem Rückimport frisch exportieren. Die Projektdatei gehört zur ursprünglichen App-Installation und enthält genaue Zeiten, Orte und private Angaben. Verwaltungsberichte weiterhin gesondert als PDF/Mail mit Kalenderwochen erstellen.

## Prüfung und Grenzen

390 automatisierte Android-Prüfungen bestehen. Die Browserprüfungen testen Übernehmen für alle Bündelmitglieder, Änderungen im noch fokussierten Textfeld, Download und erneutes Öffnen, Quellenwahl, Abbruch, gleiche Angaben, uneinheitliche Altbündel, Undo/Redo und eindeutige Dateinamen. Eine tatsächlich gespeicherte Browserdatei wird in Android auf gemeinsame Beschreibung, Belag und Gefährdungsmerkmale sowie erhaltene Rohwerte, neue Handymessungen und Spur geprüft. Die Kartenprüfung verwendet den echten WebGL-Renderer mit künstlichen Antworten.

Direkte `file://`-Navigation und die Live-Verbindung zu OpenFreeMap sind in der Cloud durch Browser-/Netzwerkrichtlinien blockiert; die Prüfungen rendern denselben HTML-Inhalt als Testdokument. Reales Fairphone, Firefox, Safari und die echte Kartenverbindung auf deinem Computer bleiben praktische Prüfungen. Lint meldet keine Fehler und zwei bisherige Hinweise zu Annotation-Abhängigkeit und Kartenkonstruktor. APK-Signatur und öffentliche Downloads werden geprüft. [SHA-256-Prüfsummen](SHA256SUMS).

APK-Dateigröße: 4882667 Bytes. SHA-256: `590f68a30c8b65e4e959872bb7e9292844a1bf1cc0bf60deddf0377ca0750a72`.
