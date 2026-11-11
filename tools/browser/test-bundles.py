#!/usr/bin/env python3
"""Durable bundle edits, source reconciliation, and the short path for matching properties."""
from pathlib import Path
import argparse, copy, json, re, runpy, uuid
from playwright.sync_api import sync_playwright

parser=argparse.ArgumentParser();parser.add_argument('fixture',type=Path);parser.add_argument('--screenshots',type=Path);args=parser.parse_args()
prefix='<script id="bodenwelle-project" type="application/json">'
original=json.loads((args.fixture/'original.html').read_text().split(prefix,1)[1].split('</script>',1)[0])
template=runpy.run_path('tools/browser/build-editor.py')['document']()
def html(data):
    raw=json.dumps(data,ensure_ascii=False).replace('<','\\u003c').replace('>','\\u003e').replace('&','\\u0026')
    return template.replace(prefix+'null</script>',prefix+raw+'</script>',1)

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
    context=browser.new_context(viewport={'width':1600,'height':1000},accept_downloads=True)
    context.set_default_timeout(8000);errors=[]
    context.route('https://tiles.openfreemap.org/**',lambda route:route.abort());context.route('https://assets.openfreemap.com/**',lambda route:route.abort())
    def page(data):
        result=context.new_page();result.on('pageerror',lambda e:errors.append(str(e)));result.set_content(html(data),wait_until='domcontentloaded');result.wait_for_selector('#inspector h1');return result
    matching=copy.deepcopy(original)
    a=matching['edits']['1'];b=matching['edits']['2']
    for key in ['category','note','hazards','traffic','surface','photos']:b[key]=copy.deepcopy(a[key])
    a['note']='G'*20001;b['note']=a['note']
    for id in ['1','2']:matching['base']['rows'][id]['edit']['note']=a['note'];matching['base']['rows'][id]['raw']['note']=a['note']
    a['heightMm']=4;b['heightMm']=9;a['reviewed']=False;b['reviewed']=True
    same=page(matching)
    same.get_by_label('#1 auswählen',exact=True).check();same.get_by_label('#2 auswählen',exact=True).check();same.get_by_role('button',name='Auswahl bündeln',exact=True).click()
    dialog=same.get_by_role('dialog');assert dialog.locator('select').count()==0
    assert dialog.get_by_text('Die gemeinsamen Angaben und Fotos stimmen überein und werden unverändert übernommen.',exact=True).is_visible()
    dialog.get_by_role('button',name='Bündeln & übernehmen',exact=True).click()
    same.get_by_label('Art der Unebenheit',exact=True).select_option('pothole')
    same.get_by_label('Verkehrsfläche',exact=True).select_option('cycleway');same.get_by_label('Oberfläche',exact=True).select_option('concrete')
    same.get_by_label('Beschreibung / Notiz',exact=True).fill('Gemeinsam ausdrücklich übernommen')
    assert same.evaluate('BodenwelleEditor.project.edits["1"].note')!= 'Gemeinsam ausdrücklich übernommen'
    same.get_by_role('button',name='Änderungen übernehmen',exact=True).click()
    assert same.evaluate('["1","2"].every(id=>{const e=BodenwelleEditor.project.edits[id];return e.note==="Gemeinsam ausdrücklich übernommen"&&e.category==="pothole"&&e.surface==="concrete"&&e.traffic==="cycleway"})')
    # Saving while the last text field still has focus must include that text for every member.
    same.get_by_label('Beschreibung / Notiz',exact=True).fill('Ohne Fokuswechsel dauerhaft gespeichert')
    with same.expect_download() as offered:same.locator('#save').click()
    first=offered.value;assert re.fullmatch(r'Bodenwelle-Bearbeitet-\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}-[a-f0-9-]{36}\.html',first.suggested_filename)
    saved=args.fixture/'bundle-saved.html';first.save_as(saved)
    with same.expect_download() as again:same.locator('#save').click()
    assert first.suggested_filename!=again.value.suggested_filename
    reopened=context.new_page();reopened.on('pageerror',lambda e:errors.append(str(e)));reopened.set_content(saved.read_text(),wait_until='domcontentloaded');reopened.wait_for_selector('#apply-place')
    assert reopened.evaluate('["1","2"].every(id=>BodenwelleEditor.project.edits[id].note==="Ohne Fokuswechsel dauerhaft gespeichert")')
    assert reopened.evaluate('BodenwelleEditor.project.edits["1"].heightMm===4&&BodenwelleEditor.project.edits["2"].heightMm===9&&!BodenwelleEditor.project.edits["1"].reviewed&&BodenwelleEditor.project.edits["2"].reviewed')
    assert reopened.evaluate('BodenwelleEditor.project.base')==matching['base']
    # Navigation commits one draft as one undo step, without losing its fields during list rebuilding.
    reopened.get_by_label('Beschreibung / Notiz',exact=True).fill('Beim Wechsel erhalten')
    reopened.locator('#rows').get_by_text('#3',exact=True).click()
    assert reopened.locator('#inspector .refs').inner_text()=='#3'
    assert reopened.evaluate('["1","2"].every(id=>BodenwelleEditor.project.edits[id].note==="Beim Wechsel erhalten")')
    reopened.locator('#undo').click();assert reopened.evaluate('BodenwelleEditor.project.edits["1"].note')=='Ohne Fokuswechsel dauerhaft gespeichert'
    reopened.locator('#redo').click();assert reopened.evaluate('BodenwelleEditor.project.edits["2"].note')=='Beim Wechsel erhalten'
    # Different reviewed descriptions expose a full comparison and editable final result.
    differing=copy.deepcopy(original);a=differing['edits']['1'];c=differing['edits']['3']
    a.update(traffic='cycleway',surface='asphalt',hazards=1,note='Erste bestätigte Beschreibung',heightMm=4,reviewed=True)
    c.update(traffic='shared_path',surface='concrete',hazards=16,note='Zweite bestätigte Beschreibung',heightMm=9,reviewed=False)
    new='new-'+str(uuid.uuid4())+'.jpg';differing['blobs'][new]=next(iter(differing['blobs'].values()));c['photos']=[new]
    different=page(differing)
    different.get_by_label('#1 auswählen',exact=True).check();different.get_by_label('#3 auswählen',exact=True).check()
    def open_merge():
        different.get_by_role('button',name='Auswahl bündeln',exact=True).click();return different.get_by_role('dialog')
    dialog=open_merge();dialog.get_by_role('button',name='Bündeln & übernehmen',exact=True).click()
    assert dialog.is_visible();assert different.evaluate('BodenwelleEditor.project.edits["1"].group!==BodenwelleEditor.project.edits["3"].group')
    assert dialog.get_by_text('Bisherige Angaben vergleichen',exact=True).is_visible()
    dialog.get_by_label('Ausgangspunkt für die gemeinsamen Angaben',exact=True).select_option('3')
    assert dialog.get_by_label('Gemeinsame Beschreibung',exact=True).input_value()=='Zweite bestätigte Beschreibung'
    dialog.get_by_label('Unebenheitsart',exact=True).select_option('pothole');dialog.get_by_label('Gemeinsame Beschreibung',exact=True).fill('Bewusst vereinheitlicht')
    dialog.get_by_role('button',name='Abbrechen',exact=True).click()
    assert different.evaluate('BodenwelleEditor.project')==differing
    dialog=open_merge();dialog.get_by_label('Ausgangspunkt für die gemeinsamen Angaben',exact=True).select_option('3')
    dialog.get_by_label('Unebenheitsart',exact=True).select_option('pothole');dialog.get_by_label('Gemeinsame Beschreibung',exact=True).fill('Bewusst vereinheitlicht')
    dialog.get_by_label('Schlechte Sichtbarkeit der Unebenheit',exact=True).check()
    if args.screenshots:
        args.screenshots.mkdir(parents=True,exist_ok=True);different.locator('#toast').wait_for(state='hidden');different.screenshot(path=str(args.screenshots/'browser-buendel-vergleichen.png'),full_page=True)
    dialog.get_by_role('button',name='Bündeln & übernehmen',exact=True).click()
    result=different.evaluate('BodenwelleEditor.project.edits')
    for id in ['1','3']:
        assert result[id]['category']=='pothole' and result[id]['note']=='Bewusst vereinheitlicht' and result[id]['hazards']==17
        assert result[id]['surface']=='concrete' and result[id]['traffic']=='shared_path' and len(result[id]['photos'])==2
    assert result['1']['group']==result['3']['group'];assert result['1']['heightMm']==4 and result['3']['heightMm']==9 and result['1']['reviewed'] and not result['3']['reviewed']
    assert different.evaluate('BodenwelleEditor.project.base')==original['base']
    if args.screenshots:different.screenshot(path=str(args.screenshots/'browser-buendel-uebernehmen.png'),full_page=True)
    # Older inconsistent bundles must be reconciled explicitly instead of silently taking one row.
    legacy=copy.deepcopy(differing);legacy['edits']['1']['group']='legacy-group';legacy['edits']['3']['group']='legacy-group'
    old=page(legacy);old.get_by_role('button',name='Gemeinsame Angaben abgleichen',exact=True).click()
    dialog=old.get_by_role('dialog');dialog.get_by_label('Ausgangspunkt für die gemeinsamen Angaben',exact=True).select_option('3');dialog.get_by_role('button',name='Für alle übernehmen',exact=True).click()
    assert old.evaluate('["1","3"].every(id=>{const e=BodenwelleEditor.project.edits[id];return e.group==="legacy-group"&&e.note==="Zweite bestätigte Beschreibung"&&e.surface==="concrete"&&e.photos.length===2})')
    assert not errors,errors
    print(json.dumps({'status':'OK','matching_properties_short_dialog':True,'differing_properties_full_comparison':True,'source_preset_and_cancel':True,'apply_to_all':True,'focused_field_download':True,'reopened_values_preserved':True,'navigation_undo_redo':True,'individual_evidence_preserved':True,'browser_errors':errors}))
    browser.close()
