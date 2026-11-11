package de.fahrradsucht.bodenwelle;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Local file workflow with a frozen preview, explicit conflict choices and backup before every write. */
public final class BrowserExchangeActivity extends Activity {
    private static final int OPEN=71,SAVE=72;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout content,preview,buttons;
    private TextView status;
    private volatile boolean busy,closed;
    private String source="";
    private File pendingFile;
    private BrowserProject.Loaded loaded;
    private BrowserMerge plan;
    private final Map<String,Spinner> choices=new HashMap<>();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);
        if(Build.VERSION.SDK_INT>=30)content.setOnApplyWindowInsetsListener((v,insets)->{var b=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(20)+b.left,dp(16)+b.top,dp(20)+b.right,dp(24)+b.bottom);return insets;});setContentView(scroll);
        text(content,"Am Computer bearbeiten",26);
        text(content,"1. Eine HTML-Projektdatei speichern und auf den Computer übertragen.\n2. Die Datei dort im Browser öffnen, bearbeiten und als neue Datei speichern.\n3. Die bearbeitete Datei hier zurückimportieren.",16);
        text(content,"Alle gespeicherten Messungen, Fotos, Beschreibungen, Bündelungen und die letzte Fahrtspur werden mitgenommen. Der Browser braucht keine Installation oder Anmeldung. Nur der OpenFreeMap-Straßenhintergrund auf OSM-Basis benötigt Internet und WebGL-Grafikunterstützung. Die Datei enthält auch genaue Zeitpunkte, Standorte und private Notizen.",14);
        status=text(content,"Bereit. Während der Bearbeitung am Computer kannst du auf dem Handy weiter aufzeichnen.",15);status.setId(R.id.browser_status);
        buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.VERTICAL);content.addView(buttons);
        button(buttons,"Projektdatei für den Browser speichern",R.id.browser_export,()->runTask(()->{File file=BrowserProject.create(this);handler.post(()->saveAs(file,BrowserProject.exportFilename("Projekt")));}));
        button(buttons,"Bearbeitete Projektdatei öffnen",R.id.browser_import,()->{if(RecordingService.running){message("Zum Rückimportieren zuerst die Fahrt beenden.");return;}startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),OPEN);});
        button(buttons,"Letzte Importsicherung speichern",R.id.browser_backup,()->runTask(()->{File file=BrowserProject.lastBackup(this);if(file==null)throw new IOException("Es ist noch keine Importsicherung vorhanden.");handler.post(()->saveAs(file,BrowserProject.exportFilename("Sicherung")));}));
        button(buttons,"Letzten Import rückgängig machen",R.id.browser_restore,this::confirmRestore);
        button(buttons,"Zurück",R.id.browser_back,this::finish);
        preview=new LinearLayout(this);preview.setOrientation(LinearLayout.VERTICAL);content.addView(preview);
        if(state!=null){source=state.getString("source","");String pending=state.getString("pending","");if(pending.matches("[a-f0-9-]{36}\\.html")){try{File out=new File(BrowserProject.directory(this,state.getBoolean("pendingBackup")?"browser-backups":"browser-out"),pending);if(out.isFile())pendingFile=out;}catch(IOException ignored){}}
            if(!source.isEmpty())load(Uri.parse(source),state.getBundle("choices"));}
    }
    private interface Job{void run()throws Exception;}
    private void runTask(Job job){if(busy)return;setBusy(true);worker.execute(()->{try{job.run();}catch(Exception e){handler.post(()->message(e.getMessage()==null?"Die Datei konnte nicht verarbeitet werden.":e.getMessage()));}finally{handler.post(()->setBusy(false));if(closed&&loaded!=null)loaded.close();}});}
    private void setBusy(boolean value){busy=value;if(closed)return;for(int i=0;i<buttons.getChildCount();i++)buttons.getChildAt(i).setEnabled(!value);for(Spinner s:choices.values())s.setEnabled(!value);View apply=findViewById(R.id.browser_apply);if(apply!=null)apply.setEnabled(!value);if(value)status.setText(R.string.browser_working);}
    private void message(String value){if(!closed){status.setText(value);Toast.makeText(this,value,Toast.LENGTH_LONG).show();}}
    private void saveAs(File file,String name){if(closed)return;pendingFile=file;message("Projektdatei vorbereitet. Bitte einen Speicherort wählen.");startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/html").putExtra(Intent.EXTRA_TITLE,name),SAVE);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null){message("Dateiauswahl abgebrochen. Die Handydaten wurden nicht verändert.");return;}Uri uri=data.getData();
        if(request==OPEN){source=uri.toString();try{if("content".equals(uri.getScheme()))getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException|IllegalArgumentException ignored){}load(uri,null);}
        if(request==SAVE&&pendingFile!=null){File file=pendingFile;runTask(()->{try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Speicherort nicht verfügbar.");byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}handler.post(()->message("HTML-Projektdatei gespeichert. Auf den Computer übertragen und dort im Browser öffnen."));});}
    }
    private void load(Uri uri,Bundle restored){runTask(()->{BrowserProject.Loaded next;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Die Datei konnte nicht geöffnet werden.");next=BrowserProject.load(this,in);}
        try(FindingStore store=new FindingStore(this)){BrowserMerge merge=new BrowserMerge(BrowserSnapshot.edits(next.base),next.edits,BrowserSnapshot.currentEdits(store));if(loaded!=null)loaded.close();loaded=next;plan=merge;handler.post(()->showPreview(restored));}catch(Exception e){next.close();throw e;}});}
    private void showPreview(Bundle restored){if(closed)return;preview.removeAllViews();choices.clear();
        text(preview,"Rückimport prüfen",22);text(preview,plan.parts.size()+" betroffene Stelle(n) / verbundene Gruppen · "+plan.conflicts()+" Konflikt(e). Neue Handymessungen und die aktuelle Fahrtspur bleiben erhalten.",15);
        text(preview,"Konflikte werden für zusammenhängende Bündel gemeinsam entschieden. „Browser“ kann auch Löschungen oder Wiederherstellungen umfassen. Bei reinen Beschreibungsänderungen bleiben die aktuelle Handygruppierung und neue Mitglieder erhalten; geänderte gemeinsame Angaben gelten für alle. Nur ausdrücklich aufgelöste Bündel werden getrennt. Bei einer sonst drohenden Teilabtrennung wird ein aktueller Export verlangt.",14);
        for(BrowserMerge.Part part:plan.parts)if(part.conflict()) {
            text(preview,"Messungen #"+String.join(", #",part.ids()),18);text(preview,"HANDY\n"+part.phone()+"\n\nBROWSER\n"+part.browser(),14);
            Map<String,File> phonePhotos=new LinkedHashMap<>(),browserPhotos=new LinkedHashMap<>();
            try{for(String id:part.ids()){JSONObject phone=plan.live.optJSONObject(id),browser=loaded.edits.optJSONObject(id);if(phone!=null){JSONArray a=phone.getJSONArray("photos");for(int i=0;i<a.length();i++){String key=a.getString(i);phonePhotos.put(key,PhotoFiles.localFile(this,key));}}if(browser!=null){JSONArray a=browser.getJSONArray("photos");for(int i=0;i<a.length();i++){String key=a.getString(i);browserPhotos.put(key,loaded.blobs.get(key));}}}}
            catch(JSONException e){message("Eine Fotoliste konnte nicht gelesen werden.");}
            if(!phonePhotos.isEmpty())photoButton("Handy-Fotos ansehen",phonePhotos);if(!browserPhotos.isEmpty())photoButton("Browser-Fotos ansehen",browserPhotos);
            Spinner choice=new Spinner(this);choice.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Bitte auswählen …","Handy beibehalten","Browser übernehmen"}));choice.setMinimumHeight(dp(48));preview.addView(choice);choices.put(part.key(),choice);if(restored!=null)choice.setSelection(restored.getInt(part.key(),0));
        }
        if(plan.parts.isEmpty()){message("Keine Änderungen aus dieser Datei zu übernehmen. Neuere Handydaten bleiben erhalten.");return;}
        button(preview,"Ausgewählte Änderungen übernehmen",R.id.browser_apply,()->{
            try{Map<String,Boolean> selected=new HashMap<>();for(var e:choices.entrySet()){int index=e.getValue().getSelectedItemPosition();if(index==0)throw new IOException("Bitte für jeden Konflikt Handy oder Browser auswählen.");selected.put(e.getKey(),index==2);}JSONObject chosen=plan.chosen(selected);if(chosen.length()==0){preview.removeAllViews();message("Handydaten beibehalten. Es wurden keine Browseränderungen übernommen.");return;}
                int deleted=0;for(String id:BrowserMerge.keys(chosen))if(chosen.isNull(id))deleted++;
                new AlertDialog.Builder(this).setTitle("Änderungen übernehmen?").setMessage(chosen.length()+" Messung(en) werden nach deiner Auswahl aktualisiert; davon "+deleted+" gelöscht. Zuvor wird automatisch eine vollständige lokale Sicherung angelegt.")
                        .setNegativeButton("Abbrechen",null).setPositiveButton("Sichern und importieren",(d,w)->runTask(()->{File backup=BrowserProject.backup(this);BrowserDatabase.apply(this,loaded,chosen,plan.fingerprint,backup);BrowserProject.rememberBackup(this,backup);handler.post(()->{preview.removeAllViews();source="";message("Rückimport abgeschlossen. Neue Handymessungen und die aktuelle Fahrtspur sind erhalten.");});})).show();
            }catch(Exception e){message(e.getMessage());}
        });
        status.setText(R.string.browser_preview_ready);
    }
    private void photoButton(String title,Map<String,File> files) {
        button(preview,title+" ("+files.size()+")",android.view.View.NO_ID,()->{
            List<File> photos=new ArrayList<>(files.values());int[] index={0};android.graphics.Bitmap[] current={null};
            LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(8),dp(16),dp(8));ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);root.addView(image,new LinearLayout.LayoutParams(-1,dp(280)));TextView page=text(root,"",14);
            Runnable show=()->{image.setImageDrawable(null);if(current[0]!=null){current[0].recycle();current[0]=null;}File file=photos.get(index[0]);if(file!=null){android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;android.graphics.BitmapFactory.decodeFile(file.getPath(),options);if(options.outWidth>0&&options.outHeight>0&&options.outWidth<=40000&&options.outHeight<=40000){int sample=1;while(Math.max(options.outWidth,options.outHeight)/sample>1024)sample*=2;options.inJustDecodeBounds=false;options.inSampleSize=sample;current[0]=android.graphics.BitmapFactory.decodeFile(file.getPath(),options);image.setImageBitmap(current[0]);}}image.setContentDescription(title+" · Foto "+(index[0]+1));page.setText(getString(current[0]==null?R.string.browser_photo_unreadable:R.string.browser_photo_page,index[0]+1,photos.size()));};
            LinearLayout controls=new LinearLayout(this);root.addView(controls);button(controls,"Vorheriges Foto",View.NO_ID,()->{index[0]=(index[0]+photos.size()-1)%photos.size();show.run();});button(controls,"Nächstes Foto",View.NO_ID,()->{index[0]=(index[0]+1)%photos.size();show.run();});show.run();AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(root).setPositiveButton("Schließen",null).create();dialog.setOnDismissListener(d->{image.setImageDrawable(null);if(current[0]!=null)current[0].recycle();});dialog.show();
        });
    }
    private void confirmRestore(){new AlertDialog.Builder(this).setTitle("Letzten Import rückgängig machen?").setMessage("Die Messungen, Beschreibungen, Fotos und Bündelungen der letzten Importsicherung werden wiederhergestellt. Später neu aufgezeichnete Messungen und die aktuelle Fahrtspur bleiben erhalten. Vorher wird der jetzige Stand erneut gesichert.")
        .setNegativeButton("Abbrechen",null).setPositiveButton("Wiederherstellen",(d,w)->runTask(()->{File backup=BrowserProject.lastBackup(this);if(backup==null)throw new IOException("Es ist noch keine Importsicherung vorhanden.");try(var original=BrowserProject.load(this,new FileInputStream(backup),true);FindingStore store=new FindingStore(this)){String fingerprint=BrowserJson.canonical(BrowserSnapshot.currentEdits(store));JSONObject desired=BrowserSnapshot.edits(original.base);File undoBackup=BrowserProject.backup(this);BrowserDatabase.apply(this,original,desired,fingerprint,undoBackup);BrowserProject.rememberBackup(this,undoBackup);}handler.post(()->{source="";preview.removeAllViews();message("Importsicherung wiederhergestellt. Neuere Messungen bleiben erhalten.");});})).show();}
    @Override public void onSaveInstanceState(Bundle state){state.putString("source",source);if(pendingFile!=null){state.putString("pending",pendingFile.getName());state.putBoolean("pendingBackup",pendingFile.getParentFile().getName().equals("browser-backups"));}Bundle selections=new Bundle();for(var e:choices.entrySet())selections.putInt(e.getKey(),e.getValue().getSelectedItemPosition());state.putBundle("choices",selections);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){closed=true;if(!busy&&loaded!=null)loaded.close();worker.shutdown();super.onDestroy();}
    private TextView text(LinearLayout parent,String label,int size){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(0xff1b302e);t.setPadding(0,dp(8),0,dp(8));parent.addView(t);return t;}
    private Button button(LinearLayout parent,String label,int id,Runnable action){Button b=new Button(this);b.setText(label);b.setId(id);b.setAllCaps(false);b.setMinHeight(dp(48));parent.addView(b);b.setOnClickListener(v->action.run());return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
