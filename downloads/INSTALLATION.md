# Bodenwelle 0.7.0 – Android-Testversion

[APK 0.7.0 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.7.0.apk). Installiere sie **als Update über die vorhandene App**. App-ID und Signatur bleiben gleich; Einträge, Fahrtspur und Einstellungen bleiben erhalten. Bitte nicht zuerst deinstallieren.

## Neu ausprobieren

1. **Vertikal kalibrieren:** Handy fest befestigen, **Fahrt starten**, zwei Sekunden im Stand ruhig halten und auf **Kalibriert** warten. Automatische Erkennung wartet auf diese Kalibrierung. **Im Stand neu kalibrieren** steht an der Karte bereit. Ohne verfügbaren Android-Lagesensor zeigt die App an, dass die kalibrierte Lage fest bleibt.
2. **Zusammenfassbar:** Die Fundstellenliste zeigt Vorschlagsgruppen mit streng unter zwei Metern Abstand, auch vor der Freigabe. GPS-Ungenauigkeit kann größer sein. Über **Vorschläge prüfen** die gemeinsame Unebenheit bestätigen; Originale, Fotos und Freigaben bleiben einzeln erhalten.
3. **Unebener Straßenabschnitt:** Anhaltende Schwingungen können einen Abschnitt mit Anfang, Ende und geschätzter Länge ergeben. Er wird als ein ungeprüfter Eintrag gespeichert. Die orange Linie ist eine ungefähre Anfang-Ende-Verbindung. Einzelstoßkandidaten desselben Abschnitts werden nicht zusätzlich gespeichert. Die Einzelstoßschwelle bleibt bei 40 m/s² einstellbar.
4. **Kartenaktionen:** Einen Punkt auf der Startseitenkarte oder der großen Karte antippen. Das Menü bietet **Stelle prüfen und Fotos ergänzen** sowie **Löschen**. Bei bestätigten Bündeln bleibt jede Messung einzeln auswählbar. Auch orange Abschnittslinien lassen sich antippen.

Fotos, Bündelung, CSV und Mailentwürfe funktionieren weiterhin mit persönlicher Freigabe. Die Mail nennt Abschnittsenden und geschätzte Länge, enthält aber weiterhin nur Kalenderwochen statt genauer Messzeiten. Erst im externen Mailprogramm wird versendet. Bei unklarer Bezirkszuordnung eines Abschnitts bleibt die zuständige Stelle offen.

[Kalibrierung, Algorithmus und Grenzen](../docs/KALIBRIERUNG-ABSCHNITTE.md) · [Bündelung und Fotos](../docs/BUENDELUNG-FOTOS.md)

## Prüfstand

211 automatisierte Tests bestehen, einschließlich Kalibrierung im Stand, Unterdrückung horizontaler Beschleunigung, anhaltenden Schwingungen, gespeichertem Abschnitt beim Fahrtende, Abschnittsmetadaten, Kalenderwochen, Bezirksgrenzen, Kartenbearbeitung auf Android 9 und 15, direktem Löschen, Abbrechen, Auswahl einzelner Bündelbeobachtungen, Erhalt anderer Fotos und der Fahrtspur, Datenmigration, reversibler Bündelung, einzelner Freigaben, Kamera-/Bildauswahl und Mailanhängen auf simuliertem Android 9 und 15. Eine echte JPEG-Prüfung mit nativer Grafikverarbeitung auf simuliertem Android 15 bestätigt die Entfernung von GPS-, Zeit- und Kamerametadaten sowie korrekte Orientierung und ein unverändertes Original. Android-Build und APK-Signatur sind geprüft. Lint meldet keine Fehler, einen Hinweis zur älteren Annotation-Abhängigkeit und die bestehende Konstruktorwarnung der Kartenansicht.

Android 8.0 oder neuer und ein Beschleunigungssensor werden benötigt. Kamera und dein konkretes Mailprogramm müssen auf dem Fairphone praktisch geprüft werden. Die Bezirksgrenzen stammen weiterhin aus 2013 und liefern nur korrigierbare Vorschläge. Die App berechnet keine automatische Schadenshöhe und trifft keine rechtlich verbindliche Einstufung.

Dateigröße: 1020713 Bytes. SHA-256-Prüfsumme: [SHA256SUMS](SHA256SUMS). Fahrtest: [FAHRTEST.md](../docs/FAHRTEST.md).
