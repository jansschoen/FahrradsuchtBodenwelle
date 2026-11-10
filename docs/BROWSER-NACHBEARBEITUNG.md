# Fundstellen am Computer bearbeiten · ab 0.15.0

Du brauchst eine einzige **HTML-Projektdatei**. Sie enthält den Browsereditor, alle gespeicherten Messungen, Beschreibungen, Bündelungen, Fotos und die letzte Fahrtspur. Eine Website, ein Benutzerkonto oder eine Installation auf dem Computer sind nicht nötig. Die Datei wird zwischen Handy und Computer übertragen; es gibt keine automatische Synchronisierung.

## Vom Handy zum Browser und zurück

1. Die APK als Update installieren, ohne die App vorher zu deinstallieren. Fahrt beenden und **Mehr → Daten verwalten → Am Computer bearbeiten / Rückimport** öffnen.
2. **Projektdatei für den Browser speichern** wählen und die HTML-Datei beispielsweise über USB auf den Computer kopieren. Die Projektdatei enthält die vollständigen gespeicherten Daten, einschließlich nicht freigegebener Stellen.
3. Die Datei mit einem aktuellen Desktopbrowser öffnen, etwa über Rechtsklick → **Öffnen mit**. Links stehen die Fundstellen, in der Mitte die Karte, rechts die Angaben zur ausgewählten Stelle. Falls der Computer HTML-Dateien standardmäßig im Texteditor öffnet, ausdrücklich den Browser auswählen.
4. Angaben bearbeiten und oben **Bearbeitete Datei speichern** wählen. Auch Strg+S beziehungsweise Cmd+S führt diese Funktion aus. Der Browser bietet eine neue HTML-Datei als Download an; je nach Browsereinstellung erscheint eine Speicherauswahl oder die Datei landet im Downloadordner. Dort prüfen, ob sie tatsächlich gespeichert wurde. Die ursprüngliche Datei bleibt als Ausgangskopie erhalten.
5. Die neue Datei zurück auf das Handy übertragen. **Bearbeitete Projektdatei öffnen** wählen, die Vorschau prüfen und erst dann **Ausgewählte Änderungen übernehmen → Sichern und importieren** bestätigen.

Nach einem erfolgreichen Rückimport für jede weitere Bearbeitungsrunde **eine neue Projektdatei vom Handy exportieren**. Ein Export wird in diesem Prototyp nur einmal übernommen. Die Rückgabe gehört zur ursprünglichen App-Installation; sie ist kein Umzugsformat auf ein anderes Handy. Deinstallation oder das Löschen der App-Daten entfernt auch die erforderlichen lokalen Exportgrundlagen.

[Beispiel zum Ausprobieren](../downloads/Bodenwelle-Browser-Beispiel.zip): entpacken und `Bodenwelle-Beispiel.html` im Browser öffnen. Die enthaltenen Messungen und das Foto sind erfunden und als Beispiel gekennzeichnet; diese Datei lässt sich nicht in deine App importieren. Der [leere Browsereditor](../downloads/Bodenwelle-Browser.html) kann ebenfalls eine exportierte Projektdatei über seine Dateiauswahl öffnen. Im normalen Ablauf genügt die vom Handy erzeugte Datei.

## Karte und Übersicht

Die zoombare Karte verwendet **OpenStreetMap** als Straßenhintergrund. Ein Klick auf einen Punkt öffnet die zugehörigen Angaben. Nummern entsprechen den ursprünglichen Messungsnummern; **×** bezeichnet ein Bündel. Überlappende Punkte öffnen eine Auswahl. Ein grüner Haken zeigt eine vollständig freigegebene Fundstelle. Suchfeld und Statusfilter wirken auf Liste und Karte. Große Listen werden zunächst in Portionen von 200 Stellen angezeigt.

Die letzte Fahrtspur lässt sich ein- und ausblenden. Bei einer ausgewählten Stelle erscheint der GPS-Genauigkeitskreis. **Alle Stellen** zeigt den gefilterten Überblick; **Auswahl** zentriert die ausgewählte Stelle. Verschieben und Zoomen ändern keine Koordinaten. Als Lagebasis dienen die gespeicherten Messpunkte, nicht der Rand des Genauigkeitskreises. Bei Bündeln wird der Mitgliedspunkt mit der besten angegebenen GPS-Genauigkeit dargestellt.

Der OSM-Schalter steuert ausschließlich den Straßenhintergrund. Nur die im aktuellen Ausschnitt benötigten Kacheln werden abgerufen, ohne Gebietsdownload. Dabei erfährt der Anbieter Ausschnitt und IP-Adresse, aber keine Beschreibungen, Fotos oder Sensorwerte. Die OSM-Attribution bleibt sichtbar. Es gibt keine externen Skripte, Schriftarten, Analyse- oder Uploaddienste.

**Ohne Internet bleiben Bearbeitung, Messpunkte, Fotos und Fahrtspur verfügbar.** Eine vollständige Offline-Straßenkarte ist nicht enthalten. Bei fehlenden Kacheln erscheinen ein Raster und ein Hinweis. Bereits im Browsercache vorhandene Kacheln können verfügbar sein. Die Abschaltung des Hintergrunds entfernt die sichtbaren Kacheln, löscht aber keinen Browsercache.

## Was sich bearbeiten lässt

- Art der Unebenheit und Beschreibung / Notiz.
- Alle zusätzlichen Gefährdungsmerkmale sowie manuelle Verkehrsfläche und Oberfläche. **Automatisch / Exportstand** nutzt die beim Export vorhandenen OSM-Angaben. Der Browser führt keine neue Overpass-Abfrage durch.
- Fotos ergänzen, ansehen und aus der gemeinsamen Galerie entfernen. Neue Fotos werden lokal verkleinert und als neue JPEG-Kopie gespeichert. Android bereitet sie beim Import nochmals auf; ursprüngliche EXIF-Orts-/Zeitangaben werden nicht übernommen. Sichtbare Bildinhalte bleiben zu prüfen.
- Messungen derselben Stelle über die Auswahlkästchen bündeln oder eine bestehende Bündelung auflösen. Widersprüchliche bekannte Arten und manuelle Belagsangaben verlangen einen gemeinsamen Wert. Die gemeinsame Galerie bietet fünf Fotoplätze je enthaltener Messung; vorhandene Galerien bleiben nach dem Auflösen erhalten.
- Tatsächlich vor Ort gemessene Höhe, individuelle Freigabe und Bezirkszuordnung je Messung. Die App berechnet keine Höhe aus der Erschütterung.
- Einzelne Stellen oder die Auswahl löschen, Löschungen rückgängig machen und gelöschte Einträge über den Filter als Einzelmessung wiederherstellen. Dabei werden die ursprünglichen Angaben und die noch in der Datei enthaltenen Fotos übernommen. Eine Browserlöschung wirkt erst nach bestätigtem Rückimport auf das Handy.

Art, Beschreibung, Gefährdungsmerkmale, Verkehrsfläche, Oberfläche und Galerie gelten gemeinsam für alle Mitglieder eines Bündels. Höhen und Freigaben bleiben individuell. **Rückgängig / Wiederholen** hält die letzten 50 Bearbeitungsschritte der geöffneten Sitzung vor; diese Bedienhistorie wird nicht mitgespeichert.

Originalkoordinaten, genaue Messzeitpunkte, Sensorwerte, Erkennungsschwellen und weitere Rohdaten sind zur Nachverfolgung lesbar. Sie lassen sich im Editor nicht ändern. Die letzte Fahrtspur und die mitgeführten allgemeinen App-Einstellungen werden beim Rückimport nicht ersetzt. Der Browserprototyp erstellt keine neuen Sensormessungen und keinen Verwaltungsbericht; PDF und Mail werden anschließend wie bisher in der Android-App vorbereitet.

## Weiterfahren, Konflikte und Sicherung

Nach dem Export kannst du auf dem Handy weiter aufzeichnen. Neu hinzugekommene Messungen bleiben beim Rückimport erhalten. Während der eigentlichen Dateierstellung und des Rückimports muss die Fahrt beendet sein.

Die App vergleicht Ausgangskopie, Browserbearbeitung und aktuellen Handystand. Nur auf dem Handy geänderte Angaben bleiben erhalten; nur im Browser geänderte Angaben können übernommen werden. Wurde dieselbe Stelle auf beiden Seiten verändert, zeigt die Vorschau beide Stände und verlangt ausdrücklich **Handy beibehalten** oder **Browser übernehmen**. Fotos beider Stände lassen sich in der Konfliktprüfung ansehen. Es gibt keine vorausgewählte Konfliktlösung.

Die Entscheidung gilt für die gesamte zusammenhängende Gruppe, auch wenn nur unterschiedliche Felder bearbeitet wurden. Das erhält gemeinsame Bündelangaben. Beim Umgruppieren können später hinzugekommene Handymessungen aus dem alten Bündel getrennt werden; ihre Daten bleiben erhalten. Ändert sich der Handystand nach der Vorschau erneut, bricht die Übernahme ab und verlangt eine neue Prüfung.

Vor jeder tatsächlichen Übernahme legt die App eine vollständige lokale HTML-Sicherung an. **Letzten Import rückgängig machen** stellt deren Messungen einschließlich gelöschter Einträge wieder her und sichert vorher wiederum den aktuellen Stand. Später neu erfasste Messungen und die aktuelle Fahrtspur bleiben dabei erhalten. **Letzte Importsicherung speichern** kopiert die Sicherung an einen selbst gewählten Ort. Bis zu drei interne HTML-Sicherungen bleiben erhalten. **Alle Fundstellen löschen**, Deinstallation oder das Löschen der App-Daten entfernt auch interne Importsicherungen; externe Dateien bleiben eigenständige Kopien.

## Datenumfang und Grenzen des Prototyps

Die Projektdatei ist eine Arbeitskopie mit genauen Zeitpunkten, Standorten, privaten Notizen und Fotos – **kein anonymisierter Bericht ans Bezirksamt**. Ursprüngliche Beschreibungen bleiben zur Konfliktprüfung in der Ausgangskopie enthalten, auch wenn sie im Browser geändert wurden. Beim Speichern werden nur noch zugeordnete Fotodateien eingebettet. Verwende für die Verwaltung weiterhin die geprüfte PDF-/Mail-Funktion mit Kalenderwochen.

Aktuelle Obergrenzen: 10.000 Messungen, 100.000 Spurpunkte, 1.000 Fotos, insgesamt 128 MiB JPEG-Daten, 16 MiB Ausgangsmetadaten und 256 MiB Projektdatei. Neue Notizen sind auf 20.000 Zeichen begrenzt. Neu ausgewählte Quellfotos dürfen höchstens 32 MiB groß sein und werden auf höchstens 2.048 Pixel Kantenlänge und 1,5 MB je JPEG verkleinert. Grenzen können abhängig von Gerät und Datenstruktur früher erreicht werden. Zu große oder unvollständige Projekte werden abgewiesen; es werden keine Messungen stillschweigend weggelassen.

Für den Rückimport liest Android ausschließlich den eingebetteten Datenblock und führt keinen HTML- oder JavaScript-Code aus. Ausgangsdaten werden mit der privaten Exportgrundlage auf dem Handy verglichen. Änderungen an Rohdaten, fehlende verwendete Fotos oder eine fremde App-Installation führen zum Abbruch.

## Prüfstand

382 automatisierte Android-Prüfungen bestehen, einschließlich Export aller Datenfelder, Konflikten, neuen Handymessungen, gemeinsamen Fotos, Löschungen, Rücknahme, Metadatenbereinigung und Datenbankmigration auf Schema 9. Android-Build und Lint sind geprüft. Lint enthält keine Fehler; zwei ältere Hinweise betreffen die Annotation-Abhängigkeit und einen Kartenkonstruktor.

Zusätzlich wurde eine tatsächliche Android-Projektdatei in Chromium bearbeitet, mit neuem Foto und Bündelung gespeichert und die gespeicherte Datei erfolgreich in Android zurückimportiert. Messwerte, nachträgliche Handymessungen und Spur blieben erhalten. Die Browserprüfung enthält auch Dateiauswahl, Kartenbedienung, Suche, Lösch-Rücknahme und das erneute Öffnen der gespeicherten Daten.

**Die Cloud-Browserrichtlinie blockiert Navigation zu `file://`.** Deshalb wurde derselbe HTML-Inhalt im Browser-Test als Testdokument gerendert. Der Doppelklick auf die lokale Datei unter Windows/macOS/Linux ist damit noch nicht praktisch geprüft; ebenso wenig Firefox und Safari oder das reale Fairphone. Die Browserprüfung blockiert Kartenabrufe absichtlich, um den Offlinefall reproduzierbar zu testen. Die [Bilder](screenshots/0.15.0/README.md) zeigen deshalb den gekennzeichneten Kartenraster-Hintergrund, keine nachgewiesene Live-Kartenverbindung.

Die reproduzierbaren Prüfkommandos stehen unter [tools/browser](../tools/browser/README.md).
