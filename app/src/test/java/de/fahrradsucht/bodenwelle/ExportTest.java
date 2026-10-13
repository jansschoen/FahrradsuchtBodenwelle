package de.fahrradsucht.bodenwelle;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExportTest {
    private Finding finding(boolean reviewed, String note) {
        return new Finding(1, 0, 52.5, 13.4, 5, 5f, 8.0, 3.0, "automatic", "Wurzel", note, reviewed, 1000);
    }
    @Test public void unreviewedLocationsNeverAppearInExport() {
        String csv = FindingCsv.exportReviewed(List.of(finding(false, "Private observation")));
        assertEquals(1, csv.lines().count());
        assertFalse(csv.contains("52.5"));
        assertFalse(csv.contains("Private observation"));
    }
    @Test public void reviewedFindingIncludesMeasurementQuality() {
        String csv = FindingCsv.exportReviewed(List.of(finding(true, "Wurzel gesehen")));
        assertTrue(csv.contains("1970-01-01T00:00:00Z,52.5,13.4,5.0,1000,18.0,8.0,3.0"));
        assertTrue(csv.contains("Wurzel gesehen"));
    }
    @Test public void notesWithCommasQuotesAndNewlinesAreEscaped() {
        String csv = FindingCsv.exportReviewed(List.of(finding(true, "Kante, \"rechts\"\nHöhe unklar")));
        assertTrue(csv.contains("\"Kante, \"\"rechts\"\"\nHöhe unklar\""));
    }
    @Test public void spreadsheetFormulaInNoteIsExportedAsText() {
        String csv = FindingCsv.exportReviewed(List.of(finding(true, "  =HYPERLINK(\"https://example.com\")")));
        assertTrue(csv.contains("\"'  =HYPERLINK"));
    }
    @Test public void manualFindingDoesNotInventSpeedOrHeight() {
        Finding f = new Finding(2, 0, 52.5, 13.4, 5, null, null, null, "manual", "Straßenschaden", "", true, 500);
        String csv = FindingCsv.exportReviewed(List.of(f));
        assertTrue(csv.contains(",500,,,,\"manual\""));
        assertFalse(csv.contains("height"));
    }
}
