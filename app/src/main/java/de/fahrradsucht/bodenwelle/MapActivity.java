package de.fahrradsucht.bodenwelle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

/** Full-screen map without interrupting the recording foreground service. */
public final class MapActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FindingStore store;
    private RideMapPanel panel;
    private TextView status;
    private LinearLayout rideControls;
    private FindingActions findingActions;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new FindingStore(this);
        findingActions = new FindingActions(this, store, () -> panel.refresh());
        findingActions.restore(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
            });
        }
        Button back = new Button(this);
        back.setText(R.string.back_to_ride);
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());
        root.addView(back);
        status = new TextView(this);
        status.setTextSize(14);
        int padding = Math.round(8 * getResources().getDisplayMetrics().density);
        status.setPadding(padding, padding, padding, padding);
        root.addView(status);
        panel = new RideMapPanel(this, store, () -> requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1), findingActions::show, true);
        findingActions.setMapBundler(panel::startBundling);
        panel.restoreSelection(state);
        root.addView(panel, new LinearLayout.LayoutParams(-1, 0, 1));
        rideControls = new LinearLayout(this);
        rideControls.setOrientation(LinearLayout.HORIZONTAL);
        Button mark = new Button(this);
        mark.setText(R.string.mark_place); mark.setAllCaps(false);
        mark.setOnClickListener(v -> startService(new Intent(this, RecordingService.class).setAction(RecordingService.MARK)));
        Button stop = new Button(this);
        stop.setText(R.string.stop_ride); stop.setAllCaps(false);
        stop.setOnClickListener(v -> startService(new Intent(this, RecordingService.class).setAction(RecordingService.STOP)));
        rideControls.addView(mark, new LinearLayout.LayoutParams(0, -2, 1));
        rideControls.addView(stop, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(rideControls);
        setContentView(root);
        if (getIntent().hasExtra("latitude") && getIntent().hasExtra("longitude"))
            panel.showCoordinate(getIntent().getDoubleExtra("latitude", 52.52), getIntent().getDoubleExtra("longitude", 13.405));
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            panel.refresh();
            status.setText(!RecordingService.running || RecordingService.lastAction.isEmpty()
                    ? RecordingService.status : getString(R.string.ride_status_with_action, RecordingService.status, RecordingService.lastAction));
            rideControls.setVisibility(RecordingService.running ? View.VISIBLE : View.GONE);
            handler.postDelayed(this, 1_000);
        }
    };
    @Override public void onResume() { super.onResume(); OsmEnricher.visible(this, true); panel.resume(); handler.post(tick); }
    @Override public void onPause() { handler.removeCallbacks(tick); OsmEnricher.visible(this, false); panel.pause(); super.onPause(); }
    @Override public void onDestroy() { panel.destroy(); store.close(); super.onDestroy(); }
    @Override public void onSaveInstanceState(Bundle state) { findingActions.save(state); panel.saveSelection(state); super.onSaveInstanceState(state); }
    @Override public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        findingActions.onActivityResult(request, result, data);
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] result) {
        super.onRequestPermissionsResult(code, permissions, result); panel.resume();
    }
}
