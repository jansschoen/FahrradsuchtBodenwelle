package de.fahrradsucht.bodenwelle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MailReportsTest {
    private Finding finding(long id, boolean reviewed, String time, String note) {
        return new Finding(id, Instant.parse(time).toEpochMilli(), 52.5 + id * .00001, 13.4, 5, 6f, 48.0, 40.0,
                "automatic", "pothole", note, reviewed, 1234);
    }
    @Test public void calendarWeekUsesBerlinAndIsoWeekYearAtYearAndWeekBoundaries() {
        assertEquals("KW 53/2020", MailReports.calendarWeek(Instant.parse("2021-01-01T12:34:56Z").toEpochMilli()));
        assertEquals("KW 01/2021", MailReports.calendarWeek(Instant.parse("2021-01-04T00:00:00Z").toEpochMilli()));
        assertEquals("KW 42/2026", MailReports.calendarWeek(Instant.parse("2026-10-11T22:30:00Z").toEpochMilli()));
    }
    @Test public void splitDistrictsNeverIncludesUnreviewedEntriesOrMixesDistricts() {
        List<MailReports.Group> groups = MailReports.groups(List.of(
                new MailReports.Item(finding(1, true, "2026-10-13T07:24:19Z", ""), "01", "automatic"),
                new MailReports.Item(finding(2, true, "2026-10-13T07:24:19Z", ""), "02", "manual"),
                new MailReports.Item(finding(3, false, "2026-10-13T07:24:19Z", "private"), "01", "automatic")), true);
        assertEquals(2, groups.size()); assertEquals("01", groups.get(0).districtId()); assertEquals("02", groups.get(1).districtId());
        assertEquals(1, groups.get(0).items().size()); assertEquals(1, groups.get(1).items().size());
    }
    @Test public void reportOnlyContainsWeekNotExactTimeRawGpsMetadataOrDefaultPrivateNotes() {
        Finding f = finding(1, true, "2026-10-13T07:24:19Z", "Privat: am 13.10.2026 um 09:24 Name X");
        MailReports.Group group = MailReports.groups(List.of(new MailReports.Item(f, "01", "automatic")), true).get(0);
        MailReports.Draft draft = MailReports.draft(group, "Mitte", true, false, "");
        assertTrue(draft.body().contains("KW 42/2026")); assertTrue(draft.body().contains("48,0 m/s²"));
        for (String privateValue : new String[]{"2026-10-13", "07:24", "09:24", "13.10.2026", "Privat", "gps_fix_age_ms", "1234", "time_utc"}) assertFalse(privateValue, draft.body().contains(privateValue));
        assertTrue(draft.body().contains("keine gemessenen Schadenshöhen")); assertTrue(draft.body().contains("Prüfung vor Ort"));
    }
    @Test public void personalStyleAndExplicitNoteChoiceDoNotInventIdentityOrRequireNetworkAi() {
        MailReports.Group g = MailReports.groups(List.of(new MailReports.Item(finding(1, true, "2026-10-13T07:24:19Z", "sichtbare Kante"), "", "unknown")), true).get(0);
        MailReports.Draft personal = MailReports.draft(g, "", false, true, "Jan");
        assertFalse(personal.body().contains("Bodenwelle")); assertTrue(personal.body().contains("sichtbare Kante")); assertTrue(personal.body().endsWith("Jan"));
        MailReports.Draft transparent = MailReports.draft(g, "", true, false, "");
        assertTrue(transparent.body().contains("mit Bodenwelle zusammengestellt")); assertFalse(transparent.body().contains("[Name]"));
    }
    @Test public void largeSelectionsBecomeCompleteManageablePartsAndCanRemainUnsplit() {
        List<MailReports.Item> items = new ArrayList<>();
        for (int i = 0; i < 41; i++) items.add(new MailReports.Item(finding(i, true, "2026-10-13T07:24:19Z", ""), i < 21 ? "01" : "02", "manual"));
        List<MailReports.Group> split = MailReports.groups(items, true), joined = MailReports.groups(items, false);
        assertEquals(3, split.size()); assertEquals(3, joined.size());
        assertEquals(41, split.stream().mapToInt(g -> g.items().size()).sum()); assertEquals(41, joined.stream().mapToInt(g -> g.items().size()).sum());
        assertTrue(joined.stream().allMatch(g -> g.items().size() <= 20 && g.districtId().isEmpty()));
    }
    @Test public void reportOrderIsGeographicalInsteadOfRideTimeOrder() {
        Finding early = finding(2, true, "2026-10-13T07:24:19Z", ""), late = finding(1, true, "2026-10-13T09:24:19Z", "");
        MailReports.Group g = MailReports.groups(List.of(new MailReports.Item(early, "01", "automatic"), new MailReports.Item(late, "01", "automatic")), true).get(0);
        assertEquals(1, g.items().get(0).finding().id());
    }
}
