package de.fahrradsucht.bodenwelle;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Human-confirmed place grouping; manual selection also works when GPS is too poor for suggestions. */
public final class BundleActivity extends Activity {
    private FindingStore store;
    private LinearLayout content;
    @Override public void onCreate(Bundle state) { super.onCreate(state);store=new FindingStore(this);if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false); }
    @Override public void onResume() { super.onResume();render(); }
    private void render() {
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xfff4f7f5);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20),dp(16),dp(20),dp(24));scroll.addView(content);
        if(Build.VERSION.SDK_INT>=30)content.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(20)+b.left,dp(16)+b.top,dp(20)+b.right,dp(24)+b.bottom);return insets;});
        setContentView(scroll);button("Zurück",this::finish);text("Mehrfachtreffer bündeln",24);
        text("Bestätige nur Beobachtungen derselben tatsächlichen Unebenheit. Einzelne Messungen, Freigaben und Fotos bleiben erhalten. Du kannst eine Bündelung wieder auflösen.",16);
        List<FindingBundle> all=store.bundles();
        List<List<FindingBundle>> suggestions=BundleSuggestions.suggest(all);
        if(!suggestions.isEmpty())text("Nahe Treffer gleicher Art – bitte vor Ort zuordnen",18);
        for(List<FindingBundle> suggestion:suggestions) {
            text(suggestion.get(0).title()+" · "+suggestion.stream().mapToInt(b->b.observations().size()).sum()+" Beobachtungen",16);
            for(FindingBundle b:suggestion)text(describe(b),14);
            button("Als dieselbe Fundstelle bestätigen",()->confirm(suggestion.stream().map(FindingBundle::key).collect(Collectors.toList())));
        }
        text("Fundstellen selbst auswählen",18);
        List<Long> keys=new ArrayList<>();List<CheckBox> boxes=new ArrayList<>();
        for(FindingBundle b:all) {
            CheckBox chosen=new CheckBox(this);chosen.setText(describe(b));content.addView(chosen);keys.add(b.key());boxes.add(chosen);
            if(b.observations().size()>1)button("Diese Bündelung auflösen",()->new AlertDialog.Builder(this).setTitle("Beobachtungen wieder einzeln anzeigen?")
                    .setMessage("Alle Messungen und Fotos bleiben erhalten.").setNegativeButton("Abbrechen",null).setPositiveButton("Auflösen",(d,w)->{store.separateBundle(b.key());render();}).show());
        }
        button("Ausgewählte Fundstellen bündeln",()->{
            List<Long> selected=new ArrayList<>();for(int i=0;i<keys.size();i++)if(boxes.get(i).isChecked())selected.add(keys.get(i));confirm(selected);
        }).setId(R.id.bundle_merge);
    }
    private String describe(FindingBundle b) {
        Finding f=b.representative();
        return b.title()+" · "+b.observations().size()+" Beobachtung(en) · "+b.reviewedCount()+" freigegeben\n"
                +String.format(java.util.Locale.ROOT,"%.5f, %.5f",f.latitude(),f.longitude())+" · "+b.observations().stream().map(x->MailReports.calendarWeek(x.timeMillis())).distinct().collect(Collectors.joining(", "));
    }
    private void confirm(List<Long> keys) {
        if(keys.size()<2){android.widget.Toast.makeText(this,"Bitte mindestens zwei Fundstellen auswählen.",android.widget.Toast.LENGTH_LONG).show();return;}
        List<Finding> members=new ArrayList<>();for(FindingBundle b:store.bundles())if(keys.contains(b.key()))members.addAll(b.observations());
        double span=0;for(Finding a:members)for(Finding b:members)span=Math.max(span,BundleSuggestions.distance(a,b));
        new AlertDialog.Builder(this).setTitle("Dieselbe Unebenheit?")
                .setMessage(members.size()+" Beobachtungen zusammenfassen? Größter Abstand der Messpunkte: "+Math.round(span)+" m. Prüfe insbesondere Straßenseite und benachbarte Schäden. Die Bündelung gilt nur nach deiner Bestätigung.")
                .setNegativeButton("Abbrechen",null).setPositiveButton("Zusammenfassen",(d,w)->{
                    try{store.mergeBundles(keys);}catch(IllegalArgumentException e){android.widget.Toast.makeText(this,e.getMessage(),android.widget.Toast.LENGTH_LONG).show();}render();
                }).show();
    }
    private void text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(0xff1b302e);t.setPadding(0,dp(6),0,dp(6));content.addView(t);}
    private Button button(String value,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setMinHeight(dp(48));b.setTextColor(0xff087f73);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffe8f2ee));content.addView(b);b.setOnClickListener(v->action.run());return b;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    @Override public void onDestroy(){store.close();super.onDestroy();}
}
