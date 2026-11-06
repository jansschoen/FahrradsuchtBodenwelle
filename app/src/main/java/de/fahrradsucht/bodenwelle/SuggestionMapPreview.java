package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.List;
import org.osmdroid.views.MapView;

/** Only visible proposal cards create/download maps. Off-screen cards release their map providers. */
// Created only in code: every preview requires its own findings.
@android.annotation.SuppressLint("ViewConstructor")
final class SuggestionMapPreview extends FrameLayout {
    private final List<Finding> findings;
    private MapView map;
    private boolean active;
    SuggestionMapPreview(Context context,List<Finding> findings) {
        super(context);this.findings=List.copyOf(findings);setMinimumHeight(dp(210));
        setContentDescription(context.getString(R.string.suggestion_map_description,FindingBundle.references(findings)));
        TextView placeholder=new TextView(context);placeholder.setText(R.string.suggestion_map_title);addView(placeholder);
    }
    void active(boolean active){this.active=active;updateVisibility();}
    void updateVisibility() {
        Rect visible=new Rect();boolean showing=active&&getWindowVisibility()==VISIBLE&&isAttachedToWindow()&&getGlobalVisibleRect(visible)&&visible.height()>dp(24);
        if(showing&&map==null) {
            removeAllViews();map=MapTiles.create(getContext());map.setId(R.id.suggestion_map);map.setMultiTouchControls(true);map.setTilesScaledToDpi(true);map.setMaxZoomLevel(19.0);
            map.getOverlays().add(new FindingMapOverlay(findings,findings.stream().map(f->"#"+f.id()).collect(java.util.stream.Collectors.toList()),getResources().getDisplayMetrics().density));
            FindingMapOverlay.center(map,findings);addView(map,new LayoutParams(-1,-1));
            TextView attribution=new TextView(getContext());attribution.setText(R.string.map_copyright);attribution.setTextSize(10);attribution.setBackgroundColor(0xeeffffff);
            attribution.setPadding(dp(4),dp(2),dp(4),dp(2));addView(attribution,new LayoutParams(-2,-2,android.view.Gravity.BOTTOM|android.view.Gravity.END));
            map.onResume();
        } else if(!showing)release();
    }
    private void release(){if(map!=null){map.onPause();map.onDetach();removeAllViews();map=null;}}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);post(this::updateVisibility);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);post(this::updateVisibility);}
    @Override public boolean onInterceptTouchEvent(MotionEvent e){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(e.getActionMasked()!=MotionEvent.ACTION_UP&&e.getActionMasked()!=MotionEvent.ACTION_CANCEL);return super.onInterceptTouchEvent(e);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
