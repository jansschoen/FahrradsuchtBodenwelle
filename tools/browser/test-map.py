#!/usr/bin/env python3
"""Real WebGL renderer + controlled map responses; never pretends to verify the live service."""
from pathlib import Path
import argparse, json, runpy, time
from playwright.sync_api import sync_playwright

parser=argparse.ArgumentParser()
parser.add_argument('fixture',type=Path)
parser.add_argument('--legacy',type=Path)
args=parser.parse_args()
html=(args.fixture/'original.html').read_text()
empty=runpy.run_path('tools/browser/build-editor.py')['document']()

def wait(page,expression):
    for _ in range(240):
        if page.evaluate(expression):return
        time.sleep(.05)
    raise AssertionError((expression,page.locator('#map-state').inner_text()))

with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
    context=browser.new_context(viewport={'width':1600,'height':1000},device_scale_factor=1,accept_downloads=True)
    requests=[];errors=[];blocked=[False]
    context.on('request',lambda r:requests.append((r.url,r.method,r.post_data)))
    # Explicitly artificial road geometry. No OSM network server is accessed by this test.
    features={'type':'FeatureCollection','features':[
        {'type':'Feature','properties':{},'geometry':{'type':'LineString','coordinates':[[13.39,52.517],[13.42,52.525]]}},
        {'type':'Feature','properties':{},'geometry':{'type':'LineString','coordinates':[[13.398,52.515],[13.405,52.527]]}}]}
    style={'version':8,'sources':{'roads':{'type':'geojson','data':'https://tiles.openfreemap.org/bodenwelle-test.geojson'}},'layers':[
        {'id':'background','type':'background','paint':{'background-color':'#e8eee7'}},
        {'id':'roads','source':'roads','type':'line','paint':{'line-color':'#c955a1','line-width':12}}]}
    def respond(route):
        if blocked[0]:route.fulfill(status=403,content_type='application/json',headers={'Access-Control-Allow-Origin':'*'},body='{"error":"test access blocked"}');return
        route.fulfill(status=200,content_type='application/json',headers={'Access-Control-Allow-Origin':'*'},body=json.dumps(style if route.request.url.endswith('/styles/liberty') else features))
    context.route('https://tiles.openfreemap.org/**',respond)
    context.route('https://assets.openfreemap.com/**',respond)
    page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
    page.set_content(html,wait_until='domcontentloaded')
    wait(page,'Boolean(BodenwelleEditor.basemap?.loaded())')
    assert 'OpenFreeMap / OSM' in page.locator('#map-state').inner_text()
    assert page.evaluate('BodenwelleEditor.basemap.isSourceLoaded("roads")')
    # The real WebGL camera and the clickable measurement canvas must agree on screen position.
    for zoom in range(2):
        page.evaluate('BodenwelleEditor.select("3",false)')
        point=page.evaluate('''()=>{const r=BodenwelleEditor.project.base.rows["4"].raw;const p=BodenwelleEditor.basemap.project([r.longitude,r.latitude]);return{x:p.x,y:p.y};}''')
        page.locator('#measurement-layer').click(position=point)
        assert page.locator('#inspector .refs').inner_text()=='#4'
        if zoom==0:page.locator('#zoom-in').click()
    # An older project is opened as inert JSON by the corrected editor, with all changes intact.
    if args.legacy:
        legacy=context.new_page();legacy.on('pageerror',lambda e:errors.append(str(e)));legacy.set_content(empty,wait_until='domcontentloaded')
        with legacy.expect_file_chooser() as chooser:legacy.get_by_role('button',name='Projektdatei öffnen',exact=True).click()
        chooser.value.set_files(str(args.legacy))
        legacy.wait_for_selector('#measurement-layer');wait(legacy,'Boolean(BodenwelleEditor.basemap?.loaded())')
        prefix='<script id="bodenwelle-project" type="application/json">';raw=args.legacy.read_text().split(prefix,1)[1].split('</script>',1)[0]
        assert legacy.evaluate('BodenwelleEditor.project')==json.loads(raw)
        with legacy.expect_download() as offered:legacy.get_by_role('button',name='Bearbeitete Datei speichern',exact=True).click()
        repaired=args.fixture/'legacy-fixed.html';offered.value.save_as(repaired)
        repaired_html=repaired.read_text();assert 'tile.openstreetmap.org' not in repaired_html
        reopened=context.new_page();reopened.on('pageerror',lambda e:errors.append(str(e)));reopened.set_content(repaired_html,wait_until='domcontentloaded')
        wait(reopened,'Boolean(BodenwelleEditor.basemap?.loaded())')
        assert reopened.evaluate('BodenwelleEditor.project')==json.loads(raw)
    # A denied map is removed and explained; it never becomes a repeating error-picture backdrop.
    blocked[0]=True
    page.locator('#network').uncheck();page.locator('#network').check()
    wait(page,'document.querySelector("#map-state").textContent.includes("derzeit nicht erreichbar")')
    assert page.locator('#basemap canvas').count()==0
    assert page.locator('#measurement-layer').is_visible()
    page.get_by_label('Beschreibung / Notiz',exact=True).fill('Bearbeitung trotz Kartenfehler')
    page.get_by_label('Beschreibung / Notiz',exact=True).press('Tab')
    assert page.evaluate('BodenwelleEditor.project.edits["4"].note')=='Bearbeitung trotz Kartenfehler'
    blocked[0]=False
    page.locator('#network').uncheck();page.locator('#network').check()
    wait(page,'Boolean(BodenwelleEditor.basemap?.loaded())')
    # No WebGL is a readable fallback; editing and local evidence stay available.
    unsupported=context.new_page();unsupported.on('pageerror',lambda e:errors.append(str(e)))
    unsupported.evaluate('''()=>{const get=HTMLCanvasElement.prototype.getContext;HTMLCanvasElement.prototype.getContext=function(type,...args){return type.includes('webgl')?null:get.call(this,type,...args);};}''')
    unsupported.set_content(html,wait_until='domcontentloaded')
    assert 'WebGL' in unsupported.locator('#map-state').inner_text()
    assert unsupported.locator('#measurement-layer').is_visible()
    assert unsupported.locator('#rows .row').count()==4
    assert not errors,errors
    network=[r for r in requests if r[0].startswith(('http://','https://'))]
    assert network and all(url.startswith(('https://tiles.openfreemap.org/','https://assets.openfreemap.com/')) and method=='GET' and data is None for url,method,data in network),network
    print(json.dumps({'status':'OK','real_webgl_renderer':True,'aligned_map_clicks':True,'legacy_file_preserved':bool(args.legacy),'http_403_fallback':True,'retry':True,'no_webgl_fallback':True,'file_scheme_tested':False,'live_provider_tested':False,'browser_errors':errors}))
    browser.close()
