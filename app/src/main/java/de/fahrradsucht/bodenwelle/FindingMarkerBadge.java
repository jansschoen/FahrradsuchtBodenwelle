package de.fahrradsucht.bodenwelle;

import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Keeps intensity and selection visible; consent is a separate, fully contained corner badge. */
final class FindingMarkerBadge extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String label;
    private final Intensity intensity;
    private final boolean selected;
    private final int released,total,width,height;
    private final float density;
    private int alpha=255;
    FindingMarkerBadge(Resources resources,String label,Intensity intensity,boolean selected,int released,int total) {
        density=resources.getDisplayMetrics().density;this.label=label;this.intensity=intensity;this.selected=selected;
        this.released=released;this.total=total;paint.setTextSize(dp(13));paint.setTypeface(Typeface.DEFAULT_BOLD);
        width=Math.round(Math.max(dp(36),paint.measureText(label)+dp(18))+dp(8));height=Math.round(dp(44));
    }
    boolean allReleased(){return total>0&&released==total;}
    @Override public int getIntrinsicWidth(){return width;}
    @Override public int getIntrinsicHeight(){return height;}
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds();float sx=b.width()/(float)width,sy=b.height()/(float)height;
        int saved=canvas.saveLayerAlpha(new RectF(b),alpha);canvas.translate(b.left,b.top);canvas.scale(sx,sy);
        RectF pill=new RectF(dp(2),dp(2),width-dp(10),height-dp(10));
        paint.setStyle(Paint.Style.FILL);paint.setColor(intensity.color());canvas.drawRoundRect(pill,dp(18),dp(18),paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(selected?4:2));paint.setColor(selected?0xff087f73:Color.WHITE);
        canvas.drawRoundRect(pill,dp(18),dp(18),paint);
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(intensity.level()<=1||intensity.level()==5?Color.WHITE:0xff1b302e);
        canvas.drawText(label,pill.centerX(),pill.centerY()-(paint.ascent()+paint.descent())/2,paint);
        if(released>0) {
            float x=width-dp(10),y=height-dp(10);paint.setColor(Color.WHITE);canvas.drawCircle(x,y,dp(10),paint);
            paint.setColor(0xff167b37);canvas.drawCircle(x,y,dp(8),paint);
            paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);
            if(allReleased()) {
                Path tick=new Path();tick.moveTo(x-dp(4),y);tick.lineTo(x-dp(1),y+dp(3));tick.lineTo(x+dp(4),y-dp(3));canvas.drawPath(tick,paint);
            } else canvas.drawLine(x-dp(3),y,x+dp(3),y,paint);
            paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);
        }
        canvas.restoreToCount(saved);
    }
    private int dp(int n){return Math.round(n*density);}
    @Override public void setAlpha(int alpha){this.alpha=Math.max(0,Math.min(255,alpha));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
