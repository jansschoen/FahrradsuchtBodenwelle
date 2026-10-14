package de.fahrradsucht.bodenwelle;

import org.junit.Test;
import static org.junit.Assert.*;

public class CalibrationTest {
    private AccelerationProjector calibrated(double x, double y, double z) {
        AccelerationProjector p = new AccelerationProjector();
        for (long t = 0; t <= 2_100_000_000L; t += 10_000_000L) p.accept(t, x, y, z);
        assertTrue(p.calibrated());
        return p;
    }
    @Test public void mountingAndMovingSamplesCannotFinishCalibration() {
        AccelerationProjector p = new AccelerationProjector();
        for (long t = 0; t < 4_000_000_000L; t += 10_000_000L) p.accept(t, 0, 0, 9.81, false);
        assertFalse(p.calibrated());
        for (long t = 4_000_000_000L; t < 6_000_000_000L; t += 10_000_000L) p.accept(t, t % 20_000_000L == 0 ? 2 : -2, 0, 9.81);
        assertFalse(p.calibrated());
    }
    @Test public void arbitraryMountingRejectsHorizontalAccelerationWithoutDrifting() {
        double u = Math.sqrt(.5);
        AccelerationProjector p = calibrated(9.81*u, 0, 9.81*u);
        for (long t = 2_110_000_000L; t < 12_000_000_000L; t += 10_000_000L)
            assertEquals(0, p.accept(t, 9.81*u + 50*u, 30, 9.81*u - 50*u), 1e-8);
    }
    @Test public void upwardAndDownwardAccelerationKeepTheirSignAndFullMagnitude() {
        AccelerationProjector p = calibrated(0, 9.81, 0);
        assertEquals(40, p.accept(2_110_000_000L, 100, 49.81, 0), 1e-8);
        assertEquals(-40, p.accept(2_120_000_000L, 0, -30.19, 100), 1e-8);
    }
    @Test public void fusedOrientationTracksTiltButStaleOrientationSuppressesDetection() {
        AccelerationProjector p = calibrated(0, 0, 9.81);
        p.orientation(2_110_000_000L, 0, 1, 0);
        assertEquals(10, p.accept(2_110_000_000L, 50, 19.81, 0), 1e-8);
        assertTrue(Double.isNaN(p.accept(2_620_000_000L, 50, 19.81, 0)));
    }
    @Test public void sensorGapRequiresFreshStableCalibration() {
        AccelerationProjector p = calibrated(0, 0, 9.81);
        assertEquals(0, p.accept(4_000_000_000L, 0, 0, 69.81), 0);
        assertFalse(p.calibrated());
        for (long t = 4_010_000_000L; t <= 6_020_000_000L; t += 10_000_000L) p.accept(t, 9.81, 0, 0);
        assertTrue(p.calibrated());
        assertEquals(20, p.accept(6_030_000_000L, 29.81, 0, 0), 1e-8);
    }
    @Test public void invalidOrBackwardSamplesDoNotPoisonTheCalibratedAxis() {
        AccelerationProjector p = calibrated(0, 0, 9.81);
        assertTrue(Double.isNaN(p.accept(2_110_000_000L, Double.NaN, 0, 0)));
        assertTrue(Double.isNaN(p.accept(1_000_000_000L, 50, 50, 50)));
        assertEquals(10, p.accept(2_120_000_000L, 0, 0, 19.81), 1e-8);
    }
}
