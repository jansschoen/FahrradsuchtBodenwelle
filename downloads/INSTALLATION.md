# Bodenwelle 0.2.1 – Android-Testversion

Dies ist eine frühe Testversion für persönliche Fahrtests, noch keine geprüfte öffentliche Produktversion.

## Herunterladen und installieren

Öffne `Bodenwelle-0.2.1.apk` im Browser und lade die Datei herunter. Öffne sie anschließend auf deinem Android-Handy. Falls Android nachfragt, erlaube die Installation aus dieser Quelle. Die Datei ist eine direkt installierbare APK; sie muss nicht entpackt werden.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Starte die App und erlaube vor der Fahrt den genauen Standort während der Nutzung. OpenStreetMap-Karten benötigen Internet. Neue Kartenausschnitte und die IP-Adresse werden dem Kartenanbieter bekannt; Fundstellen und die letzte Fahrtspur bleiben lokal.

Die App erfasst experimentelle Erschütterungshinweise, keine verlässliche Bodenwellenhöhe oder rechtliche Einstufung. Die Karte zeigt Standort, Fahrtspur und Fundstellen. Nur bewusst geprüfte Fundstellen werden auf Wunsch als CSV gespeichert. Es gibt keinen automatischen Versand an Behörden.

## Prüfstand

50 automatisierte Tests, Android-Build und APK-Signatur wurden erfolgreich geprüft. Die neuen Tests prüfen Dienststart, Standort-/Sensordaten und SQLite-Speicherung auf simuliertem Android 9 und 15. Die genaue Ursache des zuerst gemeldeten Fairphone-Verhaltens ist ohne Gerätelogs nicht nachgewiesen; die Korrekturen müssen auf dem Gerät getestet werden.

## Datei prüfen

Die APK ist 616707 Bytes groß. Die SHA-256-Prüfsumme steht in `SHA256SUMS`.

## Update von 0.2.0

Installiere 0.2.1 über die vorhandene App. App-ID und Signatur sind identisch; lokale Einträge bleiben beim Update erhalten. Unter dem Startknopf steht der Startstatus. An der Karte siehst du die geschätzte Genauigkeit in Metern und das Alter der Messung. Falls der Start weiterhin scheitert, notiere den genauen Text im Fehlerdialog oder am Startknopf.
