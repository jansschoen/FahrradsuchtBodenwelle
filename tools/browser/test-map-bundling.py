#!/usr/bin/env python3
"""Click real canvas points to select whole places, merge, save and reopen them."""
from pathlib import Path
import argparse, copy, datetime, json, runpy
from playwright.sync_api import sync_playwright

parser=argparse.ArgumentParser();parser.add_argument('fixture',type=Path);parser.add_argument('--screenshots',type=Path);args=parser.parse_args()
prefix='<script id="bodenwelle-project" type="application/json">'
data=json.loads((args.fixture/'original.html').read_text().split(prefix,1)[1].split('</script>',1)[0])
template=runpy.run_path('tools/browser/build-editor.py')['document']()
html=template.replace(prefix+'null</script>',prefix+json.dumps(data,ensure_ascii=False).replace('<','\\u003c')+'</script>',1)
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
    context=browser.new_context(viewport={'width':1600,'height':1000},accept_downloads=True);context.set_default_timeout(8000)
    context.route('https://tiles.openfreemap.org/**',lambda route:route.abort());context.route('https://assets.openfreemap.com/**',lambda route:route.abort())
    errors=[]
    page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)));page.set_content(html,wait_until='domcontentloaded')
    page.wait_for_selector('#apply-place')
    def click(id):
        point=page.evaluate('(id)=>BodenwelleEditor.mapPoints.find(p=>p.members.includes(id))',id)
        assert point,id
        page.locator('#measurement-layer').click(position={'x':point['x'],'y':point['y']})
    page.locator('#map-bundle-toggle').click();assert page.locator('#map-merge').is_disabled()
    # Close measurements are chosen explicitly, rather than one covering another.
    click('1');dialog=page.get_by_role('dialog');assert dialog.get_by_role('checkbox').count()==2
    dialog.get_by_label('Fundstelle #1 auswählen',exact=True).check();dialog.get_by_label('Fundstelle #2 auswählen',exact=True).check()
    dialog.get_by_role('button',name='Auswahl übernehmen',exact=True).click()
    assert page.locator('#map-selection-count').inner_text()=='2 Fundstelle(n) ausgewählt'
    page.locator('#map-merge').click();dialog=page.get_by_role('dialog');dialog.get_by_role('button',name='Bündeln & übernehmen',exact=True).click()
    assert page.locator('#rows .row').count()==3
    page.locator('#fit').click()
    assert len(page.evaluate('BodenwelleEditor.mapPoints'))==3
    assert page.locator('#inspector .refs').inner_text()=='Fundstelle #1'
    assert page.locator('#inspector .evidence-info summary').inner_text()=='Messbasis 2/5 · mehrfach erfasst'
    # Clear/stop do not dissolve any saved bundle. A whole existing place is selected with one click.
    page.locator('#map-bundle-toggle').click();page.locator('#map-clear').click();click('1')
    assert page.locator('#map-selection-count').inner_text()=='1 Fundstelle(n) ausgewählt'
    click('1');assert page.locator('#map-merge').is_disabled();click('1');click('3')
    assert page.locator('#map-selection-count').inner_text()=='2 Fundstelle(n) ausgewählt'
    page.locator('#map-merge').click();page.get_by_role('dialog').get_by_role('button',name='Abbrechen',exact=True).click()
    assert page.locator('#rows .row').count()==3
    page.locator('#map-merge').click();dialog=page.get_by_role('dialog');assert dialog.locator('select').count()>0
    dialog.get_by_label('Unebenheitsart',exact=True).select_option('pothole')
    if args.screenshots:
        args.screenshots.mkdir(parents=True,exist_ok=True);page.screenshot(path=str(args.screenshots/'browser-kartenbuendelung.png'),full_page=True)
    dialog.get_by_role('button',name='Bündeln & übernehmen',exact=True).click()
    page.get_by_label('Beschreibung / Notiz',exact=True).fill('Eine gemeinsame Fundstelle aus Kartenklicks')
    page.locator('#apply-place').click()
    assert page.evaluate('["1","2","3"].every(id=>BodenwelleEditor.project.edits[id].note==="Eine gemeinsame Fundstelle aus Kartenklicks")')
    with page.expect_download() as offered:page.locator('#save').click()
    saved=args.fixture/'map-bundle-saved.html';offered.value.save_as(saved)
    reopened=context.new_page();reopened.on('pageerror',lambda e:errors.append(str(e)));reopened.set_content(saved.read_text(),wait_until='domcontentloaded')
    assert reopened.locator('#rows .row').count()==2
    assert len(reopened.evaluate('BodenwelleEditor.mapPoints'))==2
    assert reopened.evaluate('new Set(["1","2","3"].map(id=>BodenwelleEditor.project.edits[id].group)).size')==1
    assert reopened.locator('#inspector .refs').inner_text()=='Fundstelle #1'
    assert 'Messbasis 2/5' in reopened.locator('#inspector .evidence-info').inner_text()
    assert reopened.evaluate('BodenwelleEditor.project.base')==data['base']
    if args.screenshots:reopened.screenshot(path=str(args.screenshots/'browser-bestaetigtes-buendel.png'),full_page=True)
    # Same derived scale as Android: five separate phases over three Berlin days.
    support=copy.deepcopy(data);stamps=['2026-11-01T12:00:00+00:00','2026-11-01T13:00:00+00:00','2026-11-02T12:00:00+00:00','2026-11-02T13:00:00+00:00','2026-11-03T12:00:00+00:00']
    for index,stamp in enumerate(stamps,1):
        id=str(index);support['base']['rows'][id]=copy.deepcopy(data['base']['rows']['1']);support['base']['rows'][id]['raw']['id']=index
        support['base']['rows'][id]['raw']['time_ms']=int(datetime.datetime.fromisoformat(stamp).timestamp()*1000)
        support['edits'][id]=copy.deepcopy(data['edits']['1']);support['edits'][id]['group']='g-score'
    score=context.new_page();score.on('pageerror',lambda e:errors.append(str(e)));score.set_content(template.replace(prefix+'null</script>',prefix+json.dumps(support).replace('<','\\u003c')+'</script>',1),wait_until='domcontentloaded')
    assert score.locator('#inspector .evidence-info summary').inner_text()=='Messbasis 5/5 · mehrfach erfasst'
    assert not errors,errors
    print(json.dumps({'status':'OK','canvas_selection':True,'overlap_chooser':True,'whole_bundle_selection':True,'save_and_reopen':True,'score':True,'file_scheme_tested':False,'live_provider_tested':False}))
    browser.close()
