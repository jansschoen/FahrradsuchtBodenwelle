package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.Toast;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Shared editing, photos and single-observation deletion for both map screens and the list. */
public final class FindingActions {
    private static final int PHOTO_PICK = 13, PHOTO_CAPTURE = 14;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.of("Europe/Berlin"));
    private static final java.util.concurrent.ExecutorService PHOTOS = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Activity activity;
    private final FindingStore store;
    private final Runnable changed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long pendingPhotoFinding;
    private String pendingCapture = "";
    private PhotoPanel activePhotoPanel;
    private java.util.function.Consumer<Finding> mapBundler;

    public FindingActions(Activity activity, FindingStore store, Runnable changed) {
        this.activity = activity; this.store = store; this.changed = changed;
    }
    public void setMapBundler(java.util.function.Consumer<Finding> action) { mapBundler = action; }
    public void restore(Bundle state) {
        if (state == null) return;
        pendingPhotoFinding = state.getLong("photoFinding");
        pendingCapture = state.getString("photoCapture", "");
    }
    public void save(Bundle state) {
        state.putLong("photoFinding", pendingPhotoFinding);
        state.putString("photoCapture", pendingCapture);
    }
    public void show(Finding finding) {
        Finding current = store.get(finding.id());
        if (current == null) { changed.run(); return; }
        String message = "Messungen " + FindingBundle.references(store.observations(current.bundleKey())) + "\n"
                + Classification.label(Classification.BUMPS, current.category())
                + "\n" + DATE.format(Instant.ofEpochMilli(current.timeMillis()))
                + (current.details().section() == null ? "" : "\n" + current.details().section().label())
                + "\n\nBitte nur im sicheren Stand bearbeiten.";
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle("Fundstelle auf der Karte").setMessage(message);
        if (mapBundler != null) {
            android.widget.Button bundle = new android.widget.Button(activity);
            bundle.setText(R.string.map_bundle_other); bundle.setAllCaps(false); bundle.setId(R.id.map_bundle_start);
            builder.setView(bundle);
            AlertDialog dialog = builder.setPositiveButton("Prüfen & Fotos", (d, w) -> review(current))
                    .setNeutralButton("Löschen", (d, w) -> deleteObservation(current)).setNegativeButton("Schließen", null).create();
            bundle.setOnClickListener(v -> { dialog.dismiss(); mapBundler.accept(current); }); dialog.show(); return;
        }
        builder
                .setPositiveButton("Prüfen & Fotos", (d, w) -> review(current))
                .setNeutralButton("Löschen", (d, w) -> deleteObservation(current))
                .setNegativeButton("Schließen", null).show();
    }
    public void review(Finding f) {
        List<Finding> members = store.observations(f.bundleKey());
        if (members.isEmpty()) { changed.run(); return; }
        if (members.size() == 1) { reviewObservation(members.get(0)); return; }
        new AlertDialog.Builder(activity).setTitle("Beobachtung auswählen")
                .setItems(observationLabels(members), (d, index) -> reviewObservation(members.get(index)))
                .setNegativeButton("Schließen", null)
                .setNeutralButton("Bündelung auflösen", (d, w) -> { store.separateBundle(f.bundleKey()); changed.run(); }).show();
    }

    private String[] observationLabels(List<Finding> members) {
        return members.stream().map(f -> (f.reviewed() ? "✓ " : "Offen · ")
                + DATE.format(Instant.ofEpochMilli(f.timeMillis())) + " · "
                + Classification.label(Classification.BUMPS, f.category()) + " · Messung #" + f.id())
                .toArray(String[]::new);
    }

    public void deleteObservation(Finding f) {
        List<Finding> members = store.observations(f.bundleKey());
        if (members.isEmpty()) { changed.run(); return; }
        if (members.size() == 1) { confirmDelete(members.get(0)); return; }
        new AlertDialog.Builder(activity).setTitle("Welche Messung löschen?")
                .setItems(observationLabels(members), (d, index) -> confirmDelete(members.get(index)))
                .setNegativeButton("Abbrechen", null).show();
    }

    private void confirmDelete(Finding f) {
        String message = "Messung #" + f.id() + " vom " + DATE.format(Instant.ofEpochMilli(f.timeMillis()))
                + "\n" + Classification.label(Classification.BUMPS, f.category())
                + "\n\nDiese Messung und ihre Fotos werden aus der App gelöscht."
                + (f.bundleId() > 0 ? " Weitere Messungen dieser Fundstelle bleiben erhalten." : "")
                + " Bereits exportierte Kopien bleiben erhalten.";
        new AlertDialog.Builder(activity).setTitle("Messung löschen?").setMessage(message)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (d, w) -> { store.delete(f.id()); changed.run(); toast("Messung gelöscht."); }).show();
    }

    private void reviewObservation(Finding f) {
        FindingReviewForm form = new FindingReviewForm(activity, f);
        activePhotoPanel = new PhotoPanel(activity, store, f.id(), new PhotoPanel.Actions() {
            @Override public void pick(long id) { pickPhoto(id); }
            @Override public void capture(long id) { capturePhoto(id); }
        });
        PhotoPanel panel = activePhotoPanel;
        form.photos.addView(panel.root);
        ScrollView scroll = new ScrollView(activity); scroll.addView(form.root);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Messung #" + f.id() + " prüfen").setView(scroll)
                .setNegativeButton("Abbrechen", null)
                .setNeutralButton("Löschen", (d, w) -> confirmDelete(f))
                .setPositiveButton("Speichern", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                store.review(f.id(), form.category(), form.note.getText().toString().trim(), form.release.isChecked(), form.details(f));
                dialog.dismiss(); changed.run();
            } catch (IllegalArgumentException e) { form.height.setError(e.getMessage()); toast(e.getMessage()); }
        }));
        dialog.setOnDismissListener(d -> { if (activePhotoPanel == panel) activePhotoPanel = null; });
        dialog.show();
    }

    private void pickPhoto(long id) {
        pendingPhotoFinding=id;pendingCapture="";
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        try { activity.startActivityForResult(pick,PHOTO_PICK); } catch(android.content.ActivityNotFoundException e){pendingPhotoFinding=0;toast("Keine Bildauswahl gefunden.");}
    }
    private void capturePhoto(long id) {
        pendingPhotoFinding=id;pendingCapture=java.util.UUID.randomUUID().toString();Uri uri=PhotoFiles.captureUri(activity,pendingCapture);
        Intent capture=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT,uri);
        capture.setClipData(android.content.ClipData.newRawUri("Kamerafoto",uri));capture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { activity.startActivityForResult(capture,PHOTO_CAPTURE); } catch(android.content.ActivityNotFoundException e){PhotoFiles.captureFile(activity,pendingCapture).delete();pendingCapture="";pendingPhotoFinding=0;toast("Keine Kamera-App gefunden. Du kannst ein vorhandenes Foto auswählen.");}
    }

    public boolean onActivityResult(int request, int result, Intent data) {
        if(request==PHOTO_PICK || request==PHOTO_CAPTURE){
            long findingId=pendingPhotoFinding;String capture=pendingCapture;pendingPhotoFinding=0;pendingCapture="";
            if(!capture.isEmpty())activity.revokeUriPermission(PhotoFiles.captureUri(activity,capture),Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(result!=Activity.RESULT_OK){if(!capture.isEmpty())PhotoFiles.captureFile(activity,capture).delete();return true;}
            java.util.List<Uri> uris=new java.util.ArrayList<>();
            if(request==PHOTO_PICK && data!=null){if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());else if(data.getData()!=null)uris.add(data.getData());}
            if(request==PHOTO_PICK && uris.size()>PhotoFiles.MAX_PER_OBSERVATION-store.photos(findingId).size()){toast("Bitte höchstens fünf Fotos pro Beobachtung auswählen. Es wurde noch kein Foto übernommen.");return true;}
            android.content.Context app=activity.getApplicationContext();toast("Foto wird lokal vorbereitet …");
            PHOTOS.execute(()->{int count=0;try{
                if(request==PHOTO_CAPTURE){PhotoFiles.importCapture(app,capture,findingId);count++;}
                else for(Uri uri:uris){PhotoFiles.importPhoto(app,uri,findingId);count++;}
                int imported=count;handler.post(()->{if(!activity.isDestroyed()){toast(imported+" Foto(s) hinzugefügt.");if(activePhotoPanel!=null)activePhotoPanel.refresh();}});
            }catch(Exception e){int imported=count;handler.post(()->{if(!activity.isDestroyed()){toast(imported+" Foto(s) hinzugefügt. Ein Foto konnte nicht übernommen werden: "+e.getMessage());if(activePhotoPanel!=null)activePhotoPanel.refresh();}});}});return true;
        }
        return false;
    }
    private void toast(String text) { Toast.makeText(activity, text, Toast.LENGTH_LONG).show(); }
}
