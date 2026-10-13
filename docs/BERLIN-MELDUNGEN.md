# Von der Fundstelle zu einer brauchbaren Meldung

Ziel ist die Beseitigung nachvollziehbar dokumentierter Mängel. Eine automatisierte Erschütterungsmessung ist ein Hinweis für eine Ortsprüfung, kein Beweis einer rechtlichen Pflichtverletzung.

## Was die Verwaltung nachvollziehen können soll

- Genaue Lage: Straße, Abschnitt, Straßenseite, Fahrtrichtung und ein Bezugspunkt; zusätzlich Koordinaten mit ihrer GPS-Genauigkeit.
- Sichtbare Beobachtung: etwa angehobene Platte, Wurzelaufbruch oder unebene Oberfläche; Datum der Beobachtung.
- Auswirkung auf die Nutzung: beispielsweise eine schwer erkennbare Kante oder eine erforderliche Ausweichbewegung, soweit tatsächlich beobachtet.
- Ein Übersichtsfoto und ein Detailfoto, sicher im Stand aufgenommen. Die erste App-Version verwaltet noch keine Fotos; diese können separat zur Meldung hinzugefügt werden.
- Eine klare Bitte: Zustand vor Ort prüfen, die Zuständigkeit bestätigen und Rückmeldung zum weiteren Vorgehen geben.

Sensorwerte können ergänzen, dass sich eine auffällige Stelle bei wiederholten Fahrten bemerkbar gemacht hat. Werte verschiedener Fahrräder, Handys und Halterungen lassen sich ohne Kalibrierung nicht direkt als Schadensschwere vergleichen. Wiederholungen und mehrere Nutzer:innen sollten später zu einer übersichtlichen Meldung je Stelle zusammengeführt werden, anstatt jedes Sensorereignis einzeln zu versenden. Die erste Version hat diese Zusammenführung noch nicht.

## Berliner Anlaufstellen und App-Entwürfe

Version 0.4.0 erstellt nach Freigabe bearbeitbare Mailentwürfe und übergibt sie an dein Mailprogramm. Automatische Zeitangaben werden auf ISO-Kalenderwochen reduziert. Die App ermittelt einen geografischen Bezirksvorschlag und bietet Aufteilung nach Bezirk sowie manuelle Korrekturen. Sie nutzt einen älteren Grenzdatenstand von 2013; besonders nahe Grenzen ist keine automatische Festlegung zuverlässig.

Am 13. Oktober 2026 wurden veröffentlichte Kontaktadressen aller zwölf Bezirke auf offiziellen Berlin.de-Seiten abgerufen und geprüft. Das Verzeichnis mit Quellen steht in [MAIL-MELDUNGEN.md](MAIL-MELDUNGEN.md). Straßen- und Grünflächenämter sind vorgeschlagene erste Anlaufstellen; die tatsächliche Zuständigkeit hängt vom Straßenabschnitt ab. Adressen und Mailtext können vor der Übergabe geändert werden. Die Mail bittet nötigenfalls um Weiterleitung. Es gibt keine verifizierte automatisierte Schnittstelle eines Berliner Meldeportals und keine garantierte Verarbeitung eines CSV-Formats.

Die App versendet nicht selbst. Erst im externen Mailprogramm wird die Nachricht auf deine Veranlassung versendet. Die Kalenderwoche reduziert die Messzeitangabe; Adresse, Absender und selbst ergänzte Inhalte machen die Mail nicht vollständig anonym. Rohdatenexporte enthalten weiterhin genauere Zeiten und werden nicht an Mailentwürfe angehängt. Rechtliche Grenzwerte oder verpflichtende Beseitigungsfristen sind weiterhin nicht nachgewiesen.

## Vorlage für eine einzelne, geprüfte Stelle

> Betreff: Bitte um Prüfung einer Unebenheit auf dem Radweg – [Straße / Abschnitt]
>
> Guten Tag,
>
> in [Kalenderwoche / Wochenjahr] habe ich auf [genaue Lage, Straßenseite und Fahrtrichtung] folgenden Zustand beobachtet: [sachliche Beschreibung].
>
> Beim Radfahren wirkt sich die Stelle folgendermaßen aus: [eigene konkrete Beobachtung]. Fotos [liegen bei / können nachgereicht werden]. Die ergänzend aufgezeichnete Position lautet [Koordinaten], mit einer GPS-Genauigkeitsangabe von ungefähr [Meter].
>
> Ich bitte um eine Prüfung vor Ort und eine Rückmeldung zum weiteren Vorgehen. Falls Ihre Stelle nicht zuständig ist, bitte ich um Weiterleitung oder um einen Hinweis auf die zuständige Stelle. Bitte teilen Sie mir, sofern verfügbar, ein Aktenzeichen mit.
>
> Vielen Dank und freundliche Grüße
> [Name und gewünschte Kontaktmöglichkeit]

Die App schlägt veröffentlichte Kontaktadressen vor und öffnet einen Entwurf; den Versand übernimmst du im Mailprogramm. Jede Person entscheidet selbst über die Freigabe und den Meldeweg. Eine Meldung garantiert keine Reparatur. Für ein späteres Gespräch mit Bezirkspolitiker:innen sind wenige gut belegte, räumlich gebündelte Beispiele und der dokumentierte Bearbeitungsstand hilfreicher als ungeprüfte Rohdaten. Kommunikation mit Behörden oder Politiker:innen wird nicht automatisch ausgelöst.
