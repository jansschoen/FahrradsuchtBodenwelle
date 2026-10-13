package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Photos belong to the original observation, so unbundling preserves their ownership. */
public final class PhotoPanel {
    public interface Actions { void pick(long findingId); void capture(long findingId); }
    public final LinearLayout root;
    private final Activity activity;
    private final FindingStore store;
    private final long findingId;
    private final Actions actions;
    public PhotoPanel(Activity a,FindingStore store,long findingId,Actions actions) {
        this.activity=a;this.store=store;this.findingId=findingId;this.actions=actions;
        root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);refresh();
    }
    public void refresh() {
        root.removeAllViews();
        TextView hint=new TextView(activity);hint.setText(R.string.photos_hint);root.addView(hint);
        java.util.List<FindingPhoto> photos=store.photos(findingId);
        for (FindingPhoto photo:photos) {
            LinearLayout row=new LinearLayout(activity);root.addView(row);
            ImageView image=new ImageView(activity);BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=8;
            image.setImageBitmap(BitmapFactory.decodeFile(PhotoFiles.localFile(activity,photo.filename()).getPath(),options));
            image.setContentDescription(activity.getString(R.string.photo_preview));
            int size=Math.round(100*activity.getResources().getDisplayMetrics().density);
            row.addView(image,new LinearLayout.LayoutParams(size,size)); image.setOnClickListener(v -> show(activity,photo));
            Button remove=new Button(activity);remove.setText(R.string.photo_remove);remove.setAllCaps(false);row.addView(remove);
            remove.setOnClickListener(v -> new AlertDialog.Builder(activity).setTitle("Foto löschen?").setMessage("Das Foto wird aus der App entfernt. Bereits weitergegebene Kopien bleiben bestehen.")
                    .setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,w)->{store.deletePhoto(photo.id());refresh();}).show());
        }
        Button pick=new Button(activity);pick.setText(R.string.photo_pick);pick.setAllCaps(false);pick.setId(R.id.photo_pick);root.addView(pick);
        Button capture=new Button(activity);capture.setText(R.string.photo_capture);capture.setAllCaps(false);capture.setId(R.id.photo_capture);root.addView(capture);
        pick.setEnabled(photos.size()<PhotoFiles.MAX_PER_OBSERVATION);capture.setEnabled(pick.isEnabled());
        pick.setOnClickListener(v->actions.pick(findingId));capture.setOnClickListener(v->actions.capture(findingId));
    }
    public static void show(Activity activity,FindingPhoto photo) {
        ImageView image=new ImageView(activity);image.setAdjustViewBounds(true);
        Bitmap bitmap=BitmapFactory.decodeFile(PhotoFiles.localFile(activity,photo.filename()).getPath());
        image.setImageBitmap(bitmap);image.setContentDescription(activity.getString(R.string.photo_preview));
        new AlertDialog.Builder(activity).setTitle("Foto ansehen").setView(image).setPositiveButton("Schließen",null)
                .setOnDismissListener(d->{image.setImageDrawable(null);if(bitmap!=null)bitmap.recycle();}).show();
    }
}
