package de.fahrradsucht.bodenwelle;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SurfaceDetectorTest {
    private SurfaceDetector warmed() {
        SurfaceDetector detector = new SurfaceDetector();
        for (long t = 0; t < 2500; t += 10) detector.accept(t, 0, 40, true, 5);
        return detector;
    }
    @Test public void sustainedSubthresholdOscillationsBecomeOneSectionRatherThanManyPoints() {
        SurfaceDetector detector = warmed();
        List<BumpDetector.Detection> points = new ArrayList<>(); List<SurfaceDetector.Section> sections = new ArrayList<>();
        for (long t = 2500; t < 8500; t += 10) {
            SurfaceDetector.Update update = detector.accept(t, t < 7500 ? (t % 20 == 0 ? 12 : -12) : 0, 40, true, 5);
            points.addAll(update.singles()); if (update.section() != null) sections.add(update.section());
        }
        assertTrue(points.isEmpty()); assertEquals(1, sections.size());
        assertEquals(12, sections.get(0).peak(), 0); assertTrue(sections.get(0).lengthMeters() >= 24);
        assertTrue(sections.get(0).endMillis() - sections.get(0).startMillis() >= 4800);
    }
    @Test public void sustainedLargeOscillationsSuppressBufferedSingleShockDetections() {
        SurfaceDetector detector = warmed(); List<BumpDetector.Detection> points = new ArrayList<>();
        for (long t = 2500; t < 7500; t += 10) points.addAll(detector.accept(t, t % 20 == 0 ? 60 : -60, 40, true, 5).singles());
        SurfaceDetector.Update stopped = detector.finish(); points.addAll(stopped.singles());
        assertTrue(points.isEmpty()); assertNotNull(stopped.section());
    }
    @Test public void isolatedImpulseStillUsesTheSelectedThresholdAndIsNotARoadSection() {
        SurfaceDetector detector = warmed(); List<BumpDetector.Detection> points = new ArrayList<>();
        for (long t = 2500; t < 6000; t += 10) {
            SurfaceDetector.Update update = detector.accept(t, t == 3000 ? -60 : 0, 40, true, 5);
            assertNull(update.section()); points.addAll(update.singles());
        }
        assertEquals(1, points.size()); assertEquals(60, points.get(0).peak(), 0); assertEquals(3000, points.get(0).elapsedMillis());
    }
    @Test public void steadyOffsetAndSparseImpulsesDoNotImplyContinuousRoughness() {
        SurfaceDetector detector = warmed();
        for (long t = 2500; t < 10000; t += 10) assertNull(detector.accept(t, 12, 40, true, 5).section());
        assertNull(detector.finish().section());
        detector = warmed();
        for (long t = 2500; t < 10000; t += 10) assertNull(detector.accept(t, t % 2000 == 0 ? 60 : 0, 40, true, 5).section());
        assertNull(detector.finish().section());
    }
    @Test public void positionLossFinalizesButDoesNotBridgeSections() {
        SurfaceDetector detector = warmed();
        for (long t = 2500; t < 6500; t += 10) detector.accept(t, t % 20 == 0 ? 12 : -12, 40, true, 5);
        assertNotNull(detector.accept(6500, 0, 40, false, 0).section());
        for (long t = 6510; t < 8000; t += 10) assertNull(detector.accept(t, t % 20 == 0 ? 12 : -12, 40, true, 5).section());
        assertNull(detector.finish().section());
    }
    @Test public void shortVibrationBurstRemainsASingleObservationOnStopping() {
        SurfaceDetector detector = warmed();
        for (long t = 2500; t < 3300; t += 10) detector.accept(t, t % 20 == 0 ? 60 : -60, 40, true, 5);
        SurfaceDetector.Update stopped = detector.finish(); assertNull(stopped.section()); assertEquals(1, stopped.singles().size());
    }
    @Test public void insufficientSampleRateCannotCreateASection() {
        SurfaceDetector detector = warmed();
        for (long t = 2500; t < 10000; t += 250) assertNull(detector.accept(t, t % 500 == 0 ? 12 : -12, 40, true, 5).section());
        assertNull(detector.finish().section());
    }
    @Test public void longRoughRoadIsSavedInBoundedChunksAndThresholdChangeEndsThePreviousChunk() {
        SurfaceDetector detector = warmed(); int sections = 0;
        for (long t = 2500; t < 127000; t += 10) if (detector.accept(t, t % 20 == 0 ? 12 : -12, 40, true, 5).section() != null) sections++;
        SurfaceDetector.Update changed = detector.accept(127000, 12, 50, true, 5);
        assertNotNull(changed.section()); assertEquals(1, sections);
        assertTrue(changed.section().endMillis() - changed.section().startMillis() <= 120000);
    }
}
