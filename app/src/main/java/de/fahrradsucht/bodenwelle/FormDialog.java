package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;

/** One bounded scroll area leaves the standard action buttons outside the scrolling form. */
final class FormDialog {
    private FormDialog(){}
    static ScrollView content(Activity activity,View form) {
        ScrollView scroll=new ScrollView(activity){
            private final Rect visible=new Rect();
            @Override protected void onMeasure(int widthSpec,int heightSpec){
                activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
                int available=visible.height()>0?visible.height():getResources().getDisplayMetrics().heightPixels;
                int limit=Math.max(1,Math.min(Math.round(520*getResources().getDisplayMetrics().density),Math.round(available*.55f)));
                if(MeasureSpec.getMode(heightSpec)!=MeasureSpec.UNSPECIFIED)limit=Math.min(limit,MeasureSpec.getSize(heightSpec));
                super.onMeasure(widthSpec,MeasureSpec.makeMeasureSpec(limit,MeasureSpec.AT_MOST));
            }
        };
        scroll.setId(R.id.edit_form_scroll);scroll.setFillViewport(false);scroll.addView(form);return scroll;
    }
    static void prepare(AlertDialog dialog){if(dialog.getWindow()!=null)dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
}
