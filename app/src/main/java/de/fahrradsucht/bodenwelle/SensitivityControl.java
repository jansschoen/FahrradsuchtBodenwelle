package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Local, persistent threshold control; changing it applies to subsequent sensor samples. */
public final class SensitivityControl extends LinearLayout {
    private final DetectionPreferences preferences;
    private final Button summary;
    private final SeekBar slider;
    public SensitivityControl(Context context) {
        super(context);
        setOrientation(VERTICAL);
        preferences = new DetectionPreferences(context);
        summary = new Button(context);
        summary.setAllCaps(false);
        summary.setText(context.getString(R.string.sensitivity_summary, preferences.threshold()));
        addView(summary);
        LinearLayout settings = new LinearLayout(context);
        settings.setOrientation(VERTICAL);
        settings.setVisibility(GONE);
        addView(settings);
        summary.setOnClickListener(v -> settings.setVisibility(settings.getVisibility() == VISIBLE ? GONE : VISIBLE));
        TextView instruction = new TextView(context);
        instruction.setText(R.string.sensitivity_hint);
        instruction.setTextSize(14); settings.addView(instruction);
        slider = new SeekBar(context);
        slider.setId(R.id.sensitivity_slider);
        slider.setMax(DetectionPreferences.MAX - DetectionPreferences.MIN);
        slider.setProgress(DetectionPreferences.MAX - preferences.threshold());
        slider.setMinimumHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        slider.setContentDescription(context.getString(R.string.sensitivity_description));
        settings.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int threshold = DetectionPreferences.MAX - progress;
                preferences.setThreshold(threshold);
                summary.setText(context.getString(R.string.sensitivity_summary, threshold));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        Button reset = new Button(context);
        reset.setText(R.string.sensitivity_reset); reset.setAllCaps(false);
        reset.setOnClickListener(v -> slider.setProgress(DetectionPreferences.MAX - DetectionPreferences.DEFAULT));
        settings.addView(reset);
    }
    public void refresh() {
        int progress = DetectionPreferences.MAX - preferences.threshold();
        if (slider.getProgress() != progress) slider.setProgress(progress);
    }
}
