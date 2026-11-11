#!/usr/bin/env python3
"""Exercise the editor and save edited.html for Android; report file navigation separately."""
from pathlib import Path
import argparse, json, re, runpy
from playwright.sync_api import sync_playwright

parser=argparse.ArgumentParser()
parser.add_argument('fixture',type=Path)
parser.add_argument('--screenshots',type=Path)
parser.add_argument('--render-content',action='store_true',help='Render authorized test markup when file URL navigation is unavailable; does not test file:// navigation.')
args=parser.parse_args();root=args.fixture.resolve()
if args.screenshots:args.screenshots.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
    context=browser.new_context(viewport={'width':1600,'height':1000},device_scale_factor=1,accept_downloads=True)
    context.set_default_timeout(8000)
    requests=[];errors=[]
    context.on('request',lambda r:requests.append((r.url,r.method,r.post_data)))
    context.route('https://tiles.openfreemap.org/**',lambda route:route.abort())
    context.route('https://assets.openfreemap.com/**',lambda route:route.abort())
    page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
    if args.render_content:page.set_content((root/'original.html').read_text(),wait_until='domcontentloaded')
    else:page.goto((root/'original.html').as_uri())
    page.wait_for_selector('#inspector h1')
    assert page.get_by_text('Beispieldaten · keine echte Schadensmeldung',exact=True).is_visible()
    assert page.locator('#measurement-layer').is_visible()
    assert page.locator('#rows .row').count()==4
    original=page.evaluate('structuredClone(BodenwelleEditor.project.base)')
    if args.screenshots:page.screenshot(path=str(args.screenshots/'browser-uebersicht.png'),full_page=True)
    # Select through the list, edit every important field using real form controls.
    page.locator('#rows').get_by_text('#1',exact=True).click()
    page.get_by_label('Art der Unebenheit',exact=True).select_option('pothole')
    page.get_by_label('Beschreibung / Notiz',exact=True).fill('Im Browser geprüft – sichere Rückfahrt 🌳')
    page.get_by_label('Beschreibung / Notiz',exact=True).press('Tab')
    page.locator('#inspector summary').filter(has_text='Zusätzliche Gefährdungsmerkmale').click()
    page.get_by_label('Schlechte Sichtbarkeit der Unebenheit',exact=True).check()
    page.get_by_label('Wasseransammlung',exact=True).check()
    page.get_by_label('Verkehrsfläche',exact=True).select_option('shared_path')
    page.get_by_label('Oberfläche',exact=True).select_option('concrete')
    page.get_by_label('Für den Export freigeben',exact=True).check()
    page.get_by_label('Höhenunterschied vor Ort gemessen',exact=True).check()
    page.get_by_label('Gemessene Höhe für #1',exact=True).fill('7.5')
    page.get_by_label('Gemessene Höhe für #1',exact=True).press('Tab')
    page.get_by_label('Bezirk / Anlaufstelle',exact=True).select_option('02')
    # An actual photo is selected via the browser file picker and normalized locally.
    b64=page.evaluate('Object.values(BodenwelleEditor.project.blobs)[0]')
    import base64
    (root/'new-photo.jpg').write_bytes(base64.b64decode(b64))
    page.locator('#inspector summary').filter(has_text=re.compile(r'^Fotos')).click()
    page.locator('#inspector input[type=file]').set_input_files(str(root/'new-photo.jpg'))
    import time
    for _ in range(160):
        if page.evaluate('BodenwelleEditor.project.edits["1"].photos.length===2'):break
        time.sleep(.05)
    assert page.evaluate('BodenwelleEditor.project.edits["1"].photos.length===2'),page.locator('#toast').inner_text()
    assert page.evaluate('BodenwelleEditor.project.edits["1"].hazards')==17
    assert page.evaluate('BodenwelleEditor.project.edits["1"].heightMm')==7.5
    # Merge two actual places; different causes must be explicitly reconciled.
    page.get_by_label('#2 auswählen',exact=True).check();page.get_by_label('#3 auswählen',exact=True).check()
    page.get_by_role('button',name='Auswahl bündeln',exact=True).click()
    dialog=page.get_by_role('dialog')
    dialog.get_by_label('Unebenheitsart',exact=True).select_option('pothole')
    dialog.get_by_role('button',name='Zusammenfassen',exact=True).click()
    page.get_by_label('Beschreibung / Notiz',exact=True).fill('Gemeinsame Browserbeschreibung')
    page.get_by_label('Beschreibung / Notiz',exact=True).press('Tab')
    assert page.evaluate('BodenwelleEditor.project.edits["2"].group===BodenwelleEditor.project.edits["3"].group')
    assert page.evaluate('BodenwelleEditor.project.edits["2"].reviewed&&!BodenwelleEditor.project.edits["3"].reviewed')
    # Delete, undo, redo. Deletion remains only a project change until Android confirmation.
    page.locator('#rows').get_by_text('#4',exact=True).click()
    page.get_by_role('button',name='Stelle löschen',exact=True).click()
    page.get_by_role('dialog').get_by_role('button',name='Bestätigen',exact=True).click()
    assert page.evaluate('BodenwelleEditor.project.edits["4"]===null')
    page.locator('#undo').click();assert page.evaluate('BodenwelleEditor.project.edits["4"]!==null')
    page.locator('#redo').click();assert page.evaluate('BodenwelleEditor.project.edits["4"]===null')
    page.locator('#list select').select_option('deleted')
    page.locator('#rows').get_by_text('#4',exact=True).click()
    page.get_by_role('button',name='Als Einzelmessung wiederherstellen',exact=True).click()
    assert page.evaluate('BodenwelleEditor.project.edits["4"].group==="s4"')
    page.locator('#undo').click();assert page.evaluate('BodenwelleEditor.project.edits["4"]===null')
    page.locator('#list select').select_option('all')
    assert page.evaluate('BodenwelleEditor.project.base')==original
    # Search is reflected in the list, and the map click opens the corresponding inspector.
    page.get_by_role('searchbox',name='Fundstellen suchen').fill('Gemeinsame')
    assert page.locator('#rows .row').count()==1
    page.get_by_role('searchbox',name='Fundstellen suchen').fill('')
    # Return to the edited place and capture the actual app, not a design mockup.
    page.locator('#rows').get_by_text('#1',exact=True).click()
    page.evaluate('BodenwelleEditor.select("2",false)')
    canvas=page.locator('#measurement-layer');box=canvas.bounding_box();canvas.click(position={'x':box['width']/2,'y':box['height']/2})
    assert page.locator('#inspector .refs').inner_text()=='#1'
    if args.screenshots:page.screenshot(path=str(args.screenshots/'browser-bearbeitung.png'),full_page=True)
    with page.expect_download() as offered:page.locator('#save').click()
    download=offered.value;download.save_as(root/'edited.html')
    assert (root/'edited.html').stat().st_size>10000
    assert not page.evaluate('BodenwelleEditor.dirty')
    reopened=context.new_page();reopened.on('pageerror',lambda e:errors.append(str(e)))
    if args.render_content:reopened.set_content((root/'edited.html').read_text(),wait_until='domcontentloaded')
    else:reopened.goto((root/'edited.html').as_uri())
    reopened.wait_for_selector('#rows .row')
    assert reopened.evaluate('BodenwelleEditor.project.edits["1"].note')=='Im Browser geprüft – sichere Rückfahrt 🌳'
    assert reopened.evaluate('BodenwelleEditor.project.edits["1"].photos.length')==2
    assert reopened.evaluate('BodenwelleEditor.project.edits["4"]===null')
    assert reopened.evaluate('BodenwelleEditor.project.base')==original
    # The separately downloadable empty editor opens an existing project through its real picker.
    viewer=context.new_page();viewer.on('pageerror',lambda e:errors.append(str(e)))
    empty=runpy.run_path('tools/browser/build-editor.py')['document']()
    (root/'empty-editor.html').write_text(empty)
    if args.render_content:viewer.set_content(empty,wait_until='domcontentloaded')
    else:viewer.goto((root/'empty-editor.html').as_uri())
    with viewer.expect_file_chooser() as chooser:viewer.get_by_role('button',name='Projektdatei öffnen',exact=True).click()
    chooser.value.set_files(str(root/'edited.html'))
    viewer.wait_for_selector('#rows .row')
    assert viewer.evaluate('BodenwelleEditor.project.base')==original
    assert viewer.evaluate('BodenwelleEditor.project.edits["1"].photos.length')==2
    viewer.locator('#rows').get_by_text('#1',exact=True).click()
    viewer.locator('#inspector summary').filter(has_text=re.compile(r'^Fotos')).click()
    assert viewer.locator('#inspector .photos img').count()==2
    if args.screenshots:
        viewer.locator('#inspector summary').filter(has_text='Verkehrsfläche und Oberfläche').click()
        viewer.screenshot(path=str(args.screenshots/'browser-fotos.png'),full_page=True)
    # Offline selection/map controls and local photo display still work.
    reopened.locator('#network').uncheck();reopened.locator('#zoom-in').click();reopened.locator('#fit').click()
    assert reopened.locator('#measurement-layer').is_visible()
    assert not errors, errors
    network=[r for r in requests if r[0].startswith(('http://','https://'))]
    assert network and all(url.startswith(('https://tiles.openfreemap.org/','https://assets.openfreemap.com/')) and method=='GET' and data is None for url,method,data in network),network
    print(json.dumps({'status':'OK','file_scheme_tested':not args.render_content,'edited_rows':4,'new_photo':True,'project_picker':True,'raw_evidence_unchanged':True,'only_openfreemap_requests':True,'browser_errors':errors},ensure_ascii=False))
    browser.close()
