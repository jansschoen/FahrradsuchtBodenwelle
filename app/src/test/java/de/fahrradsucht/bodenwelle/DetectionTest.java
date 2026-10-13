package de.fahrradsucht.bodenwelle;

import org.junit.Test;
import static org.junit.Assert.*;

public class DetectionTest {
    private BumpDetector warmed() {
        BumpDetector detector = new BumpDetector();
        for (int t = 0; t <= 2500; t += 10) assertNull(detector.accept(t, Math.sin(t) * 0.2, true));
        return detector;
    }

    @Test public void stableRoadDoesNotCreateFindings() {
        BumpDetector d = warmed();
        for (int t = 2510; t < 20_000; t += 10) assertNull(d.accept(t, Math.sin(t) * 0.6, true));
    }
    @Test public void briefShockIsRecordedWithPeak() {
        BumpDetector d = warmed();
        assertNull(d.accept(3000, 40, true));
        assertNull(d.accept(3010, -60, true));
        BumpDetector.Detection finding = d.accept(3250, 0, true);
        assertNotNull(finding);
        assertEquals(60, finding.peak(), 0.0001);
        assertEquals(3010, finding.elapsedMillis());
        assertEquals(40, finding.threshold(), 0);
    }
    @Test public void warmupIgnoresMountingShock() {
        BumpDetector d = new BumpDetector();
        assertNull(d.accept(0, 0, true));
        assertNull(d.accept(1000, 60, true));
        assertNull(d.accept(1250, 0, true));
    }
    @Test public void oneBumpWithSeveralOscillationsCreatesOneFinding() {
        BumpDetector d = warmed();
        d.accept(3000, 60, true);
        assertNotNull(d.accept(3250, 0, true));
        for (int t = 3260; t < 4700; t += 10) assertNull(d.accept(t, t % 100 < 20 ? 60 : 0, true));
    }
    @Test public void distinctLaterShockCreatesAnotherFinding() {
        BumpDetector d = warmed();
        d.accept(3000, 60, true);
        assertNotNull(d.accept(3250, 0, true));
        for (int t = 3260; t < 5000; t += 10) d.accept(t, 0, true);
        d.accept(5000, 60, true);
        assertNotNull(d.accept(5250, 0, true));
    }
    @Test public void lossOfPositionCancelsPendingFinding() {
        BumpDetector d = warmed();
        d.accept(3000, 60, true);
        assertNull(d.accept(3100, 0, false));
        assertNull(d.accept(3250, 0, true));
    }
    @Test public void standstillDoesNotCreateAutomaticFinding() {
        BumpDetector d = warmed();
        assertNull(d.accept(3000, 60, false));
        assertNull(d.accept(3250, 0, false));
    }
    @Test public void continuousSignalBelowFortyDoesNotTrigger() {
        BumpDetector d = new BumpDetector();
        for (int t = 0; t < 10_000; t += 10) assertNull(d.accept(t, t % 20 == 0 ? 39.99 : -39.99, true));
        assertNull(d.accept(10_000, 39.99, true));
        assertNull(d.accept(10_250, 0, true));
    }
    @Test public void sensorGapRequiresNewWarmup() {
        BumpDetector d = warmed();
        assertNull(d.accept(10_000, 60, true));
        assertNull(d.accept(10_250, 0, true));
    }
    @Test public void invalidSensorValuesDoNotCreateFindings() {
        BumpDetector d = warmed();
        assertNull(d.accept(3000, Double.NaN, true));
        assertNull(d.accept(3000, Double.POSITIVE_INFINITY, true));
    }
    @Test public void exactlyFortyTriggersWhileThirtyNineDoesNot() {
        BumpDetector d = warmed();
        assertNull(d.accept(3000, 39.99, true));
        assertNull(d.accept(3010, -40, true));
        BumpDetector.Detection result = d.accept(3260, 0, true);
        assertNotNull(result); assertEquals(40, result.peak(), 0);
    }
    @Test public void sensitivityChangeTakesEffectWithoutRestart() {
        BumpDetector d = warmed();
        assertNull(d.accept(3000, 20, true));
        d.setThreshold(15);
        assertNull(d.accept(3010, 20, true));
        BumpDetector.Detection result = d.accept(3260, 0, true);
        assertNotNull(result); assertEquals(15, result.threshold(), 0);
    }
    @Test public void increasingThresholdCancelsCandidateFromOldSetting() {
        BumpDetector d = warmed(); d.accept(3000, 60, true);
        d.setThreshold(80);
        assertNull(d.accept(3250, 60, true));
        assertNull(d.accept(3500, 0, true));
    }
    @Test public void projectionWorksForDifferentPhoneOrientations() {
        AccelerationProjector upright = new AccelerationProjector();
        AccelerationProjector sideways = new AccelerationProjector();
        for (long t = 1_000_000_000L; t < 4_000_000_000L; t += 10_000_000L) {
            assertEquals(0, upright.accept(t, 0, 0, 9.81), 1e-6);
            assertEquals(0, sideways.accept(t, 9.81, 0, 0), 1e-6);
        }
        double a = upright.accept(4_000_000_000L, 0, 0, 17.81);
        double b = sideways.accept(4_000_000_000L, 17.81, 0, 0);
        assertTrue(a > 7);
        assertEquals(a, b, 1e-6);
    }
    @Test public void invalidProjectionSampleDoesNotPoisonLaterValues() {
        AccelerationProjector p = new AccelerationProjector();
        p.accept(1_000_000_000L, 0, 0, 9.81);
        assertEquals(0, p.accept(1_010_000_000L, Double.NaN, 0, 9.81), 0);
        assertTrue(p.accept(1_020_000_000L, 0, 0, 17.81) > 7);
    }
    @Test public void oldInaccurateOrMissingFixPreventsDetection() {
        assertFalse(RideEligibility.locationUsable(5001, true, 5));
        assertFalse(RideEligibility.locationUsable(-1, true, 5));
        assertFalse(RideEligibility.locationUsable(1000, true, 26));
        assertFalse(RideEligibility.locationUsable(1000, false, 0));
        assertFalse(RideEligibility.locationUsable(1000, true, Float.NaN));
        assertTrue(RideEligibility.locationUsable(1000, true, 5));
    }
    @Test public void speedMustBePresentAndPlausible() {
        assertFalse(RideEligibility.autoDetectionAllowed(100, true, 5, false, 5, false, 0));
        assertFalse(RideEligibility.autoDetectionAllowed(100, true, 5, true, 0.2f, false, 0));
        assertFalse(RideEligibility.autoDetectionAllowed(100, true, 5, true, 21, false, 0));
        assertFalse(RideEligibility.autoDetectionAllowed(100, true, 5, true, 5, true, 3));
        assertTrue(RideEligibility.autoDetectionAllowed(100, true, 5, true, 5, true, 0.4f));
    }
}
