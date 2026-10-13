package de.fahrradsucht.bodenwelle;

import org.junit.Test;
import static org.junit.Assert.*;

public class PositionEstimatorTest {
    @Test public void oldGpsFixIsAdvancedNorthBySpeedTimesAge() {
        PositionEstimate p = PositionEstimator.estimate(52.5, 13.4, 3000, 6f, 0f, 5f);
        assertTrue(p.projected()); assertTrue(p.latitude() > 52.5);
        assertEquals(13.4, p.longitude(), 1e-8);
        assertEquals(18, RouteTracker.distanceMeters(52.5, 13.4, p.latitude(), p.longitude()), 0.001);
        assertEquals(52.5, p.gpsLatitude(), 0);
    }
    @Test public void fixAfterSensorPeakIsBackProjected() {
        PositionEstimate p = PositionEstimator.estimate(52.5, 13.4, -250, 8f, 90f, 5f);
        assertTrue(p.projected()); assertTrue(p.longitude() < 13.4);
        assertEquals(-2, p.distanceMeters(), 0);
        assertEquals(2, RouteTracker.distanceMeters(52.5, 13.4, p.latitude(), p.longitude()), 0.001);
    }
    @Test public void southernAndWesternCoursesUseCorrectDirection() {
        PositionEstimate south = PositionEstimator.estimate(52.5, 13.4, 2000, 5f, 180f, null);
        PositionEstimate west = PositionEstimator.estimate(52.5, 13.4, 2000, 5f, 270f, null);
        assertTrue(south.latitude() < 52.5); assertTrue(west.longitude() < 13.4);
    }
    @Test public void missingSpeedOrBearingDoesNotInventDisplacement() {
        PositionEstimate missingSpeed = PositionEstimator.estimate(52.5, 13.4, 2000, null, 0f, null);
        PositionEstimate missingBearing = PositionEstimator.estimate(52.5, 13.4, 2000, 5f, null, null);
        assertFalse(missingSpeed.projected()); assertFalse(missingBearing.projected());
        assertEquals(52.5, missingSpeed.latitude(), 0); assertEquals(13.4, missingBearing.longitude(), 0);
    }
    @Test public void unreliableBearingAndLongTimeGapKeepOriginalGps() {
        assertFalse(PositionEstimator.estimate(52.5, 13.4, 2000, 5f, 0f, 80f).projected());
        assertFalse(PositionEstimator.estimate(52.5, 13.4, 5001, 5f, 0f, 5f).projected());
    }
    @Test public void simultaneousFixAndStandstillNeedNoDisplacement() {
        assertFalse(PositionEstimator.estimate(52.5, 13.4, 0, 5f, 0f, null).projected());
        assertFalse(PositionEstimator.estimate(52.5, 13.4, 2000, 0f, 0f, null).projected());
    }
    @Test public void longitudeWrapsAtDateLine() {
        PositionEstimate p = PositionEstimator.estimate(0, 179.9999, 2000, 10f, 90f, null);
        assertTrue(p.longitude() < -179.99); assertTrue(p.longitude() >= -180);
        assertEquals(20, RouteTracker.distanceMeters(0, 179.9999, p.latitude(), p.longitude()), 0.001);
    }
    @Test public void nonFiniteInputsCannotProduceFakePosition() {
        assertFalse(PositionEstimator.estimate(52.5, 13.4, 2000, Float.NaN, 0f, null).projected());
        assertFalse(PositionEstimator.estimate(52.5, 13.4, 2000, 5f, Float.NaN, null).projected());
    }
}
