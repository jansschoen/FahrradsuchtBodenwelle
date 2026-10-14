package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Local, persistent threshold control; changing it applies to subsequent sensor samples. */
public final class SensitivityControl extends LinearLayout {
    private final DetectionPreferences preferences;
    private final Button summary;
    private final SeekBar slider;
    private final EditText number;
    private final LinearLayout settings;
    public SensitivityControl(Context context) {
        super(context);
        setOrientation(VERTICAL);
        preferences = new DetectionPreferences(context);
        summary = new Button(context);
        summary.setAllCaps(false);
        summary.setText(context.getString(R.string.sensitivity_summary, preferences.threshold()));
        addView(summary);
        settings = new LinearLayout(context);
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
        slider.setProgress(preferences.threshold() - DetectionPreferences.MIN);
        slider.setMinimumHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        slider.setContentDescription(context.getString(R.string.sensitivity_description));
        settings.addView(slider);
        TextView numberLabel = new TextView(context);
        numberLabel.setText(R.string.sensitivity_number_label); settings.addView(numberLabel);
        LinearLayout input = new LinearLayout(context);
        number = new EditText(context);
        number.setId(R.id.sensitivity_number);
        number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        number.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        number.setSingleLine(true);
        number.setText(context.getString(R.string.sensitivity_number_value, preferences.threshold()));
        number.setContentDescription(context.getString(R.string.sensitivity_number_label));
        input.addView(number, new LayoutParams(0, -2, 1));
        Button apply = new Button(context);
        apply.setId(R.id.sensitivity_apply); apply.setText(R.string.sensitivity_apply); apply.setAllCaps(false);
        input.addView(apply); settings.addView(input);
        apply.setOnClickListener(v -> applyInput());
        number.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { applyInput(); return true; }
            return false;
        });
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                int threshold = DetectionPreferences.MIN + progress;
                preferences.setThreshold(threshold);
                summary.setText(context.getString(R.string.sensitivity_summary, threshold));
                number.setText(context.getString(R.string.sensitivity_number_value, threshold)); number.setError(null);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        Button reset = new Button(context);
        reset.setText(R.string.sensitivity_reset); reset.setAllCaps(false);
        reset.setOnClickListener(v -> { slider.setProgress(DetectionPreferences.DEFAULT - DetectionPreferences.MIN); syncNumber(); });
        settings.addView(reset);
    }
    public void refresh() {
        int progress = preferences.threshold() - DetectionPreferences.MIN;
        if (slider.getProgress() != progress) slider.setProgress(progress);
    }
    public void expand() { settings.setVisibility(VISIBLE); }
    private void applyInput() {
        try {
            int value = Integer.parseInt(number.getText().toString().trim());
            preferences.setThreshold(value); slider.setProgress(value - DetectionPreferences.MIN); syncNumber();
        } catch (IllegalArgumentException e) { number.setError(getContext().getString(R.string.sensitivity_number_error)); }
    }
    private void syncNumber() {
        number.setText(getContext().getString(R.string.sensitivity_number_value, preferences.threshold())); number.setError(null);
    }
}
