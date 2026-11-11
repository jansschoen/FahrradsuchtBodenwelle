package de.fahrradsucht.bodenwelle;

import android.content.Context;
import android.text.*;
import android.view.View;
import android.widget.*;
import java.util.*;

/** Shows every final shared property, its source observations, and explicit alternatives. */
final class BundleMergeForm {
    final LinearLayout root;
    private final Context context;
    private final List<Finding> members;
    private final SharedFindingDetails proposal;
    private final Spinner category,traffic,surface;
    private final EditText note;
    private final CheckBox[] hazards=new CheckBox[Classification.HAZARDS.length];
    private final TextView preview;
    BundleMergeForm(Context context,FindingStore store,List<Finding> members) {
        this.context=context;this.members=List.copyOf(members);proposal=SharedFindingDetails.from(members);
        root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),0,dp(20),dp(8));
        label("Gemeinsame Angaben nach dem Bündeln",19);
        label("Alle folgenden Angaben gelten anschließend für jede Messung. Prüfe die vorgeschlagenen Werte und passe sie bei Bedarf an.",14);
        Button compare=new Button(context);compare.setAllCaps(false);compare.setText(context.getString(R.string.bundle_compare_sources,members.size()));root.addView(compare);
        LinearLayout sources=new LinearLayout(context);sources.setOrientation(LinearLayout.VERTICAL);sources.setVisibility(View.GONE);root.addView(sources);
        for(Finding f:members){TextView row=new TextView(context);row.setText(source(store,f));row.setTextSize(14);row.setPadding(0,dp(8),0,dp(8));sources.addView(row);}
        compare.setOnClickListener(v->sources.setVisibility(sources.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));
        label("Ausgangspunkt für die gemeinsamen Angaben",15);
        List<String> presets=new ArrayList<>();presets.add("Vorschlag: bekannte Angaben, alle Hinweise und Notizen");
        for(Finding f:members)presets.add("Angaben von Messung #"+f.id()+" übernehmen");
        Spinner preset=new Spinner(context);preset.setId(R.id.bundle_preset);preset.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_spinner_dropdown_item,presets));root.addView(preset);
        category=field("Unebenheitsart",Classification.BUMPS,proposal.category(),null,R.id.bundle_category_choice);
        traffic=field("Verkehrsfläche",Classification.ROADS,proposal.traffic(),SharedFindingDetails.automatic(members,true).value(),R.id.bundle_traffic_choice);
        surface=field("Oberfläche",Classification.SURFACES,proposal.surface(),SharedFindingDetails.automatic(members,false).value(),R.id.bundle_surface_choice);
        label("Gemeinsame Beschreibung",15);note=new EditText(context);note.setId(R.id.bundle_note);note.setMinLines(2);note.setMaxLines(6);note.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);note.setText(proposal.note());root.addView(note);
        label("Zusätzliche Gefährdungsmerkmale",15);
        for(int i=0;i<hazards.length;i++){hazards[i]=new CheckBox(context);hazards[i].setText(Classification.HAZARDS[i].label());hazards[i].setId(View.generateViewId());hazards[i].setChecked((proposal.hazards()&(1<<i))!=0);root.addView(hazards[i]);hazards[i].setOnCheckedChangeListener((b,c)->updatePreview());}
        Set<Long> photos=new HashSet<>();for(Finding f:members)for(FindingPhoto photo:store.photos(f.id()))photos.add(photo.id());
        label("Fotos: alle "+photos.size()+" verschiedenen Fotos werden gemeinsam übernommen. Doppelte Zuordnungen zählen nur einmal.",14);
        label("Messwerte, Koordinaten, Zeitpunkte, einzeln gemessene Höhen, Bezirke und Versandfreigaben bleiben unverändert. Auflösen trennt später die Messungen; die gemeinsamen Angaben bleiben dabei erhalten.",14);
        preview=label("",14);preview.setId(R.id.bundle_result_preview);
        for(Spinner spinner:List.of(category,traffic,surface))spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){updatePreview();}});
        note.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){updatePreview();}public void afterTextChanged(Editable e){}});
        int[] last={0};preset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){if(last[0]==n)return;last[0]=n;applyPreset(n);}});updatePreview();
    }
    private String source(FindingStore store,Finding f) {
        var d=f.details();List<String> flags=new ArrayList<>();for(int i=0;i<hazards.length;i++)if((d.hazards()&(1<<i))!=0)flags.add(Classification.HAZARDS[i].label());
        return "#"+f.id()+" · "+Classification.label(Classification.BUMPS,f.category())+"\nVerkehrsfläche: "+Classification.label(Classification.ROADS,d.traffic())+(d.trafficManual()?" (manuell)":" (OSM)")
                +"\nOberfläche: "+Classification.label(Classification.SURFACES,d.surface())+(d.surfaceManual()?" (manuell)":" (OSM)")+"\nBeschreibung: "+(f.note().isEmpty()?"keine":f.note())
                +"\nGefährdungsmerkmale: "+(flags.isEmpty()?"keine":String.join(", ",flags))+"\nFotos: "+store.photos(f.id()).size()+" · "+(f.reviewed()?"freigegeben":"offen");
    }
    private Spinner field(String title,Classification.Choice[] choices,SharedFindingDetails.Field current,String automatic,int id) {
        label(title,15);List<Classification.Choice> values=new ArrayList<>();
        if(current.conflicting())values.add(new Classification.Choice("","Bitte gemeinsamen Wert auswählen …"));
        if(automatic!=null)values.add(new Classification.Choice("auto","Automatisch / OSM: "+Classification.label(choices,automatic)));
        // Conflicting source values lead the list, so their provenance stays easy to compare.
        for(String value:current.conflicts())values.add(new Classification.Choice(value,Classification.label(choices,value)+" · vorhanden"));
        for(var choice:choices)if(values.stream().noneMatch(v->v.id().equals(choice.id())))values.add(choice);
        Spinner spinner=new Spinner(context);spinner.setId(id);spinner.setMinimumHeight(dp(48));spinner.setAdapter(new ArrayAdapter<>(context,android.R.layout.simple_spinner_dropdown_item,values));root.addView(spinner);
        select(spinner,current.conflicting()?"":automatic!=null&&!current.manual()?"auto":current.value());return spinner;
    }
    private void applyPreset(int index) {
        Finding f=index==0?null:members.get(index-1);var d=f==null?null:f.details();
        select(category,f==null?(proposal.category().conflicting()?"":proposal.category().value()):f.category());
        select(traffic,f==null?(proposal.traffic().conflicting()?"":proposal.traffic().manual()?proposal.traffic().value():"auto"):d.trafficManual()?d.traffic():"auto");
        select(surface,f==null?(proposal.surface().conflicting()?"":proposal.surface().manual()?proposal.surface().value():"auto"):d.surfaceManual()?d.surface():"auto");
        note.setText(f==null?proposal.note():f.note());int flags=f==null?proposal.hazards():d.hazards();for(int i=0;i<hazards.length;i++)hazards[i].setChecked((flags&(1<<i))!=0);updatePreview();
    }
    SharedFindingDetails value() {
        String bump=selected(category),road=selected(traffic),ground=selected(surface);if(bump.isEmpty()||road.isEmpty()||ground.isEmpty())throw new IllegalArgumentException("Bitte alle abweichenden gemeinsamen Angaben auswählen.");
        int flags=0;for(int i=0;i<hazards.length;i++)if(hazards[i].isChecked())flags|=1<<i;
        SharedFindingDetails result=new SharedFindingDetails(new SharedFindingDetails.Field("auto".equals(road)?SharedFindingDetails.automatic(members,true).value():road,!"auto".equals(road),List.of()),
                new SharedFindingDetails.Field("auto".equals(ground)?SharedFindingDetails.automatic(members,false).value():ground,!"auto".equals(ground),List.of()),flags,new SharedFindingDetails.Field(bump,true,List.of()),note.getText().toString());
        BundleDescription.validate(result,members);return result;
    }
    private void updatePreview() {
        if(preview==null)return;
        try{var v=value();List<String> flags=new ArrayList<>();for(int i=0;i<hazards.length;i++)if((v.hazards()&(1<<i))!=0)flags.add(Classification.HAZARDS[i].label());preview.setText(context.getResources().getQuantityString(R.plurals.bundle_result_description,members.size(),members.size(),Classification.label(Classification.BUMPS,v.category().value()),Classification.label(Classification.ROADS,v.traffic().value())+(v.traffic().manual()?" (manuell)":" (OSM)"),Classification.label(Classification.SURFACES,v.surface().value())+(v.surface().manual()?" (manuell)":" (OSM)"),flags.isEmpty()?"keine":String.join(", ",flags),v.note().isEmpty()?"keine":v.note()));}
        catch(IllegalArgumentException e){preview.setText(e.getMessage());}
    }
    private static String selected(Spinner s){return ((Classification.Choice)s.getSelectedItem()).id();}
    private static void select(Spinner s,String id){for(int i=0;i<s.getCount();i++)if(((Classification.Choice)s.getItemAtPosition(i)).id().equals(id)){s.setSelection(i);return;}}
    private TextView label(String text,int size){TextView view=new TextView(context);view.setText(text);view.setTextSize(size);view.setTextColor(0xff1b302e);view.setPadding(0,dp(7),0,dp(7));root.addView(view);return view;}
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
}
