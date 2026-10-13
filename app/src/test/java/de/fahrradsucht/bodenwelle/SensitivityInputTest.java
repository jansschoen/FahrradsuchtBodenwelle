package de.fahrradsucht.bodenwelle;

import android.widget.EditText;
import android.widget.SeekBar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 35})
public class SensitivityInputTest {
    private SensitivityControl control;
    private DetectionPreferences preferences;
    @Before public void before() {
        preferences = new DetectionPreferences(RuntimeEnvironment.getApplication()); preferences.setThreshold(40);
        control = new SensitivityControl(RuntimeEnvironment.getApplication());
    }
    @Test public void sliderNowGrowsNumericallyToTheRight() {
        SeekBar slider = control.findViewById(R.id.sensitivity_slider);
        assertEquals(35, slider.getProgress()); slider.setProgress(0); assertEquals(5, preferences.threshold());
        slider.setProgress(95); assertEquals(100, preferences.threshold());
        assertEquals("100", ((EditText) control.findViewById(R.id.sensitivity_number)).getText().toString());
    }
    @Test public void typedValueAppliesAndSynchronizesSliderAndPersistence() {
        EditText number = control.findViewById(R.id.sensitivity_number); number.setText("55");
        control.findViewById(R.id.sensitivity_apply).performClick(); assertEquals(55, preferences.threshold());
        assertEquals(50, ((SeekBar) control.findViewById(R.id.sensitivity_slider)).getProgress());
        SensitivityControl reopened = new SensitivityControl(RuntimeEnvironment.getApplication());
        assertEquals("55", ((EditText) reopened.findViewById(R.id.sensitivity_number)).getText().toString());
    }
    @Test public void invalidOrIncompleteInputDoesNotChangeLiveThreshold() {
        EditText number = control.findViewById(R.id.sensitivity_number);
        for (String value : new String[]{"", "4", "101", "NaN", "5.5"}) {
            number.setText(value); control.findViewById(R.id.sensitivity_apply).performClick();
            assertEquals(40, preferences.threshold()); assertNotNull(number.getError());
        }
    }
    @Test public void mapRefreshDoesNotEraseANumberBeingTyped() {
        EditText number = control.findViewById(R.id.sensitivity_number); number.setText("6"); control.refresh();
        assertEquals("6", number.getText().toString()); assertEquals(40, preferences.threshold());
    }
}
