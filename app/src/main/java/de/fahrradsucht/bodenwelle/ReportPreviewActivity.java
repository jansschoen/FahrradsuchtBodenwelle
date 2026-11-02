package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;

/** Private, native PDF preview with one bounded page bitmap and pinch/double-tap zoom. */
public final class ReportPreviewActivity extends Activity {
    private PDFRenderer renderer;private PDDocument document;private Bitmap bitmap;private int index;
    private ZoomPage image;private TextView count;private Button previous,next;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xffe6eee9);
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});}
        Button back=button("Zurück zur Meldung",this::finish);root.addView(back);
        TextView title=new TextView(this);title.setText(R.string.report_preview_title);title.setTextSize(20);title.setPadding(16,8,16,8);root.addView(title);
        count=new TextView(this);count.setId(R.id.report_page_count);count.setPadding(16,4,16,8);root.addView(count);
        image=new ZoomPage();root.addView(image,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=new TextView(this);hint.setText(R.string.report_zoom_hint);hint.setPadding(16,8,16,8);root.addView(hint);
        LinearLayout navigation=new LinearLayout(this);previous=button("Vorherige Seite",()->{index--;showPage();});next=button("Nächste Seite",()->{index++;showPage();});
        previous.setId(R.id.report_previous);next.setId(R.id.report_next);navigation.addView(previous,new LinearLayout.LayoutParams(0,-2,1));navigation.addView(next,new LinearLayout.LayoutParams(0,-2,1));root.addView(navigation);setContentView(root);
        try {
            android.net.Uri uri=getIntent().getData();
            if(uri==null||!getPackageName().concat(".photos").equals(uri.getAuthority())||!"report".equals(uri.getPathSegments().get(0)))throw new IOException("Ungültiger Bericht.");
            PDFBoxResourceLoader.init(this);
            try(java.io.InputStream input=getContentResolver().openInputStream(uri)){if(input==null)throw new IOException("Bericht nicht vorhanden.");document=PDDocument.load(input);}
            renderer=new PDFRenderer(document);
            index=state==null?0:state.getInt("page");index=Math.max(0,Math.min(document.getNumberOfPages()-1,index));showPage();
        }catch(IOException|RuntimeException failure){count.setText(R.string.report_preview_error);previous.setEnabled(false);next.setEnabled(false);}
    }
    private void showPage() {
        if(renderer==null||index<0||index>=document.getNumberOfPages())return;
        image.setImageDrawable(null);if(bitmap!=null)bitmap.recycle();
        try {
            int width=Math.min(1200,getResources().getDisplayMetrics().widthPixels);
            float scale=width/document.getPage(index).getMediaBox().getWidth();
            bitmap=renderer.renderImage(index,scale,ImageType.RGB);
            image.setImageBitmap(bitmap);image.reset();image.setContentDescription(getString(R.string.report_page,index+1,document.getNumberOfPages()));
        }catch(IOException|RuntimeException failure){count.setText(R.string.report_preview_error);return;}
        count.setText(getString(R.string.report_page,index+1,document.getNumberOfPages()));previous.setEnabled(index>0);next.setEnabled(index+1<document.getNumberOfPages());
    }
    private Button button(String label,Runnable action){Button button=new Button(this);button.setAllCaps(false);button.setText(label);button.setOnClickListener(v->action.run());return button;}
    @Override public void onSaveInstanceState(Bundle state){state.putInt("page",index);super.onSaveInstanceState(state);}
    @Override public void onDestroy(){image.setImageDrawable(null);if(bitmap!=null)bitmap.recycle();try{if(document!=null)document.close();}catch(IOException ignored){}super.onDestroy();}
    private final class ZoomPage extends ImageView {
        private final Matrix transform=new Matrix();private float scale=1,fit=1;
        private final ScaleGestureDetector zoom;private final GestureDetector gestures;
        ZoomPage(){super(ReportPreviewActivity.this);setScaleType(ScaleType.MATRIX);
            zoom=new ScaleGestureDetector(ReportPreviewActivity.this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector detector){float target=Math.max(fit,Math.min(fit*5,scale*detector.getScaleFactor()));transform.postScale(target/scale,target/scale,detector.getFocusX(),detector.getFocusY());scale=target;setImageMatrix(transform);return true;}});
            gestures=new GestureDetector(ReportPreviewActivity.this,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent event){return true;}@Override public boolean onScroll(MotionEvent start,MotionEvent end,float dx,float dy){transform.postTranslate(-dx,-dy);setImageMatrix(transform);return true;}@Override public boolean onDoubleTap(MotionEvent event){if(scale>fit*1.1f)reset();else{transform.postScale(3,3,event.getX(),event.getY());scale*=3;setImageMatrix(transform);}return true;}});
        }
        void reset(){if(bitmap==null||getWidth()==0){post(this::resetAfterLayout);return;}resetAfterLayout();}
        private void resetAfterLayout(){if(bitmap==null||getWidth()==0)return;fit=Math.min((float)getWidth()/bitmap.getWidth(),(float)getHeight()/bitmap.getHeight());scale=fit;transform.reset();transform.postScale(fit,fit);transform.postTranslate((getWidth()-bitmap.getWidth()*fit)/2,(getHeight()-bitmap.getHeight()*fit)/2);setImageMatrix(transform);}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);resetAfterLayout();}
        @Override public boolean onTouchEvent(MotionEvent event){zoom.onTouchEvent(event);if(!zoom.isInProgress())gestures.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
