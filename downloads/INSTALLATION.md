# Bodenwelle 0.15.1 – Kartenkorrektur für den Browser

[APK 0.15.1 herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-0.15.1.apk). Als Update installieren, **nicht vorher deinstallieren**. Signatur, App-ID und Projektformat bleiben gleich. Android 8.0 oder neuer wird benötigt; die Straßenkarte im Browser benötigt WebGL-Grafikunterstützung.

## Bereits exportierte Datei zeigt „403 – Access blocked“

Die Meldung kommt vom OSM-Kartenserver. Lokale HTML-Dateien senden keine normale Webseitenadresse als Herkunft. Die bisherige direkte Anbindung an den OSM-Standardserver war dafür ungeeignet. 0.15.1 verwendet stattdessen OpenFreeMap mit OSM-Daten und eine lokal eingebettete Kartenbibliothek.

1. Falls im alten Browserfenster noch Änderungen offen sind, dort **Bearbeitete Datei speichern** wählen und den Download prüfen.
2. Den [korrigierten Browsereditor 0.15.1 als ZIP herunterladen](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-0.15.1.zip), entpacken und **Bodenwelle-Browser.html** im Browser öffnen.
3. **Projektdatei öffnen** wählen und deine bisherige, zuletzt gespeicherte HTML-Datei auswählen. Ihre Messungen, Fotos, Bündel und gespeicherten Änderungen werden übernommen.
4. Wie gewohnt bearbeiten und eine neue HTML-Datei speichern. Diese enthält jetzt auch den korrigierten Editor. Der Rückimport funktioniert weiterhin mit App 0.15.0 oder 0.15.1.

Für diese bestehende Datei ist kein neuer Handyexport und kein App-Update nötig. Die APK 0.15.1 ergänzt den korrigierten Editor für künftige Exporte. Neue Projekte entstehen weiterhin unter **Mehr → Daten verwalten → Am Computer bearbeiten / Rückimport**.

[Anleitung zur Kartenkorrektur](../docs/BROWSER-KARTENFEHLER.md) · [Vollständige Browseranleitung](../docs/BROWSER-NACHBEARBEITUNG.md) · [Beispiel mit erfundenen Daten](https://github.com/jansschoen/FahrradsuchtBodenwelle/raw/refs/heads/main/downloads/Bodenwelle-Browser-Beispiel.zip).

## Daten und Karten

Messbeschreibungen, Fotos und Rohwerte bleiben lokal. Nur Kartenstil, Kartendaten, Symbole und Kartenbeschriftungszeichen werden bei OpenFreeMap geladen; der Anbieter sieht Ausschnitt und IP-Adresse. Es gibt keine Anmeldung und keinen API-Schlüssel. Bei fehlender Verbindung oder WebGL bleiben Messpunkte, Spur und Bearbeitung auf einem Raster verfügbar. Fehler werden als Hinweis angezeigt. Der Straßenkarten-Schalter ermöglicht einen erneuten Versuch.

Neue Handymessungen und die aktuelle Spur bleiben beim Rückimport erhalten. Konflikte verlangen eine Auswahl; vor der bestätigten Übernahme entsteht eine lokale Sicherung. Für weitere Bearbeitungsrunden nach einem Rückimport frisch exportieren. Die Projektdatei gehört zur ursprünglichen App-Installation und enthält genaue Zeiten, Orte und private Angaben. Den Verwaltungsbericht weiterhin gesondert als PDF/Mail erzeugen.

## Prüfung und Grenzen

382 automatisierte Android-Prüfungen bestehen. Build, Lint und APK-Signatur sind geprüft. Die Browserprüfung testet Bearbeiten, Fotos, Bündeln, Löschen, Speichern und tatsächlichen Android-Rückimport. Die ergänzende Kartenprüfung betreibt den echten WebGL-Renderer mit künstlichen Kartenantworten und prüft die Übereinstimmung von Karten- und Messpunkten, bisherige Projektdateien, HTTP 403, erneutes Laden und die Bedienung ohne WebGL.

Die **Live-Verbindung zu OpenFreeMap ist in der Cloud durch den HTTPS-Proxy blockiert**. Die Cloud-Browserrichtlinie sperrt direkte `file://`-Navigation; derselbe HTML-Inhalt wurde deshalb als Testdokument gerendert. Die echte Verbindung und der direkte lokale Dateiaufruf müssen auf deinem Computer geprüft werden. Firefox, Safari und das reale Fairphone wurden nicht durch diese Browserprüfung abgedeckt.

Lint meldet keine Fehler und zwei bisherige Hinweise zur Annotation-Abhängigkeit und zum Kartenkonstruktor. Die öffentlichen Downloads werden bytegenau geprüft; ihre [SHA-256-Prüfsummen](SHA256SUMS) liegen daneben. Die bisherigen Fahr-, Foto-, Bündelungs-, PDF- und Mailfunktionen bleiben enthalten.

APK-Dateigröße: 4613985 Bytes. SHA-256: `342cd9e99161bcf0beda1b81f0c2c68ea2b2ff578e8a0aed48add92a5320877a`.
