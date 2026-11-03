package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.*;
import android.view.WindowInsets;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.util.GeoPoint;

/** Dismissible post-ride comparison; every merge goes through the shared conflict/confirmation dialog. */
public final class RideReviewActivity extends Activity {
    private FindingStore store;
    private MapView map;
    private TextView summary;
    private final List<RideReviewCandidates.Pair> pairs=new ArrayList<>();
    private int index;
    private long after,through;
    private Finding first,second;
    private Overlay comparison;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);store=new FindingStore(this);
        if(RecordingService.running){finish();return;}
        after=getIntent().getLongExtra("after",0);through=getIntent().getLongExtra("through",0);
        long[] restored=state==null?null:state.getLongArray("pairs");
        if(restored!=null){for(int i=0;i+1<restored.length;i+=2)pairs.add(new RideReviewCandidates.Pair(restored[i],restored[i+1]));index=state.getInt("index");}
        else pairs.addAll(RideReviewCandidates.pairs(store.bundles(),after,through));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(8),dp(16),dp(8));
        root.setBackgroundColor(0xfff4f7f5);
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);root.setOnApplyWindowInsetsListener((v,insets)->{
            Insets b=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(16)+b.left,dp(8)+b.top,dp(16)+b.right,dp(8)+b.bottom);return insets;
        });}
        button(root,"Prüfung jetzt beenden",R.id.ride_review_finish,this::finish);
        summary=new TextView(this);summary.setId(R.id.ride_review_summary);summary.setTextSize(16);summary.setTextColor(0xff1b302e);
        ScrollView scroll=new ScrollView(this);scroll.addView(summary);root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(166)));
        map=MapTiles.create(this);map.setId(R.id.ride_review_map);map.setMultiTouchControls(true);map.setTilesScaledToDpi(true);
        map.setMaxZoomLevel(19.0);root.addView(map,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=new TextView(this);hint.setText(R.string.ride_review_hint);hint.setTextSize(12);root.addView(hint);
        button(root,"Ja, zusammenfassen …",R.id.ride_review_merge,()->{
            if(RecordingService.running){finish();return;}
            Finding a=first==null?null:store.get(first.id()),b=second==null?null:store.get(second.id());
            if(a==null||b==null||a.bundleKey()==b.bundleKey()){showNext();return;}
            BundleConfirmation.show(this,store,List.of(a.bundleKey(),b.bundleKey()),()->{index++;showNext();});
        });
        button(root,"Nein, getrennt lassen",R.id.ride_review_skip,()->{index++;showNext();});
        setContentView(root);showNext();
    }
    private void showNext() {
        if(RecordingService.running){finish();return;}
        while(index<pairs.size()) {
            RideReviewCandidates.Pair pair=pairs.get(index);Finding a=store.get(pair.first()),b=store.get(pair.second());
            if(a!=null&&b!=null&&RideReviewCandidates.near(new FindingBundle(a.bundleKey(),store.observations(a.bundleKey())),
                    new FindingBundle(b.bundleKey(),store.observations(b.bundleKey())))){first=a;second=b;break;}
            index++;
        }
        if(index>=pairs.size()){Toast.makeText(this,"Vergleich beendet. Deine Messungen bleiben gespeichert.",Toast.LENGTH_SHORT).show();finish();return;}
        double distance=BundleSuggestions.distance(first,second);
        summary.setText(String.format(Locale.GERMANY,"Fahrt beendet · Vergleich %d von %d\nGehören diese Stellen zusammen? Abstand %.1f m\nA: %s\nB: %s\nMindestens eine Messung stammt aus dieser Fahrt.",index+1,pairs.size(),distance,describe(first),describe(second)));
        if(comparison!=null)map.getOverlays().remove(comparison);
        comparison=new PairOverlay(first,second,getResources().getDisplayMetrics().density);map.getOverlays().add(comparison);
        map.setContentDescription("Vergleich von Messung #"+first.id()+" und #"+second.id());
        map.getController().setZoom(19.0);map.getController().setCenter(new GeoPoint((first.latitude()+second.latitude())/2,(first.longitude()+second.longitude())/2));
        map.invalidate();
    }
    private String describe(Finding f) {
        String refs=FindingBundle.references(store.observations(f.bundleKey()));
        return refs+" · "+Classification.label(Classification.BUMPS,f.category())+String.format(Locale.GERMANY," · GPS ± %.0f m",f.accuracy())
                +(store.observations(f.bundleKey()).stream().anyMatch(m->m.id()>after&&m.id()<=through)?" · neu":" · gespeichert")
                +String.format(Locale.ROOT," (%.6f, %.6f)",f.latitude(),f.longitude());
    }
    @Override protected void onResume(){super.onResume();if(map!=null)map.onResume();if(RecordingService.running)finish();}
    @Override protected void onPause(){if(map!=null)map.onPause();super.onPause();}
    @Override protected void onDestroy(){if(map!=null)map.onDetach();if(store!=null)store.close();super.onDestroy();}
    @Override public void onSaveInstanceState(Bundle state){long[] ids=new long[pairs.size()*2];for(int i=0;i<pairs.size();i++){ids[2*i]=pairs.get(i).first();ids[2*i+1]=pairs.get(i).second();}state.putLongArray("pairs",ids);state.putInt("index",index);super.onSaveInstanceState(state);}
    private void button(LinearLayout parent,String label,int id,Runnable action){Button b=new Button(this);b.setText(label);b.setId(id);b.setAllCaps(false);b.setMinHeight(dp(48));parent.addView(b);b.setOnClickListener(v->action.run());}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}

    /** Offset labels and leaders keep even identical GPS points distinguishable. */
    private static final class PairOverlay extends Overlay {
        private final Finding a,b;private final float scale;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        PairOverlay(Finding a,Finding b,float scale){this.a=a;this.b=b;this.scale=scale;}
        @Override public void draw(Canvas canvas,MapView map,boolean shadow){if(shadow)return;dot(canvas,map,a,-1,0xff087f73,"A");dot(canvas,map,b,1,0xffcf6d11,"B");}
        private void dot(Canvas canvas,MapView map,Finding f,int direction,int color,String letter){
            Point p=map.getProjection().toPixels(new GeoPoint(f.latitude(),f.longitude()),null);
            float x=p.x+direction*58*scale,y=p.y+direction*34*scale;
            paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*scale);canvas.drawLine(p.x,p.y,x,y,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);canvas.drawCircle(p.x,p.y,7*scale,paint);
            paint.setColor(color);canvas.drawCircle(p.x,p.y,5*scale,paint);
            paint.setTextSize(14*scale);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);
            String label=letter+" #"+f.id();float half=paint.measureText(label)/2+8*scale;
            paint.setColor(Color.WHITE);canvas.drawRoundRect(new RectF(x-half,y-14*scale,x+half,y+10*scale),8*scale,8*scale,paint);
            paint.setColor(color);canvas.drawText(label,x,y+4*scale,paint);
        }
    }
}
