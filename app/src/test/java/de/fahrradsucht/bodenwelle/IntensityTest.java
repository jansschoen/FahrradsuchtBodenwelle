package de.fahrradsucht.bodenwelle;

import org.junit.Test;
import static org.junit.Assert.*;

public class IntensityTest {
    @Test public void allHeightBoundariesUseMillimetersOnly() {
        double[] values = {0, 5, 5.001, 10, 10.001, 20, 20.001, 30, 30.001};
        int[] levels = {1, 1, 2, 2, 3, 3, 4, 4, 5};
        for (int i = 0; i < values.length; i++) {
            Intensity result = Intensity.evaluate(values[i], 99.0);
            assertEquals(levels[i], result.level()); assertEquals("mm", result.unit());
            assertEquals(values[i], result.value(), 0); assertTrue(result.heightBased());
        }
    }
    @Test public void allSensorBoundariesStayAccelerationAndAreProvisional() {
        double[] values = {0, 10, 10.001, 20, 20.001, 40, 40.001, 60, 60.001};
        int[] levels = {1, 1, 2, 2, 3, 3, 4, 4, 5};
        for (int i = 0; i < values.length; i++) {
            Intensity result = Intensity.evaluate(null, values[i]);
            assertEquals(levels[i], result.level()); assertEquals("m/s²", result.unit());
            assertFalse(result.heightBased()); assertTrue(result.label().contains("vorläufig"));
            assertEquals(values[i], result.value(), 0);
        }
    }
    @Test public void sameNumericValueHasDifferentPhysicalMeaning() {
        assertEquals(5, Intensity.evaluate(40.0, null).level());
        assertEquals(3, Intensity.evaluate(null, 40.0).level());
        assertEquals(0, Intensity.evaluate(null, null).level());
    }
    @Test public void invalidMeasurementsAreRejected() {
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> Intensity.evaluate(invalid, null));
            assertThrows(IllegalArgumentException.class, () -> Intensity.evaluate(null, invalid));
        }
    }
    @Test public void hazardsAndGeodataCannotChangeIntensity() {
        FindingDetails original = FindingDetails.initial("other", 45.0);
        FindingDetails reviewed = original.reviewed(null, 45.0, 31, "carriageway", true, "asphalt", true);
        assertEquals(original.intensity(), reviewed.intensity());
        assertEquals(5, Classification.hazards(31).split(";").length);
        assertEquals(8, Classification.BUMPS.length);
    }
}
