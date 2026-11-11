# Local HTML editor checks

The Android export embeds `app/src/main/assets/browser/editor.html`, MapLibre GL JS 5.24.0 and its data in one HTML file. The map library, CSS and license are vendored locally; no CDN scripts are loaded at runtime. MapLibre requires WebGL for the street background. Editing and the independent measurement overlay work without it. The automation below uses Python Playwright and a separately installed Chromium (`/usr/bin/chromium`). Neither is a dependency of the distributed editor.

## Android → browser → Android

Run these phases in order from the repository root. The fixture contains four artificial Berlin measurements, one generated photo and thirty route points. No real user data is used.

```sh
python3 tools/cloud-gradle.py :app:testDebugUnitTest --tests '*BrowserRoundtripTest.exportFixture' -Dbodenwelle.browser.fixture=/tmp/bodenwelle-browser-fixture
python3 tools/browser/test-editor.py /tmp/bodenwelle-browser-fixture --screenshots /tmp/bodenwelle-browser-fixture/screenshots
python3 tools/cloud-gradle.py :app:testDebugUnitTest --tests '*BrowserRoundtripTest.importEditedFixture' -Dbodenwelle.browser.fixture=/tmp/bodenwelle-browser-fixture
```

The script interacts with controls, imports a new photo, reconciles a merge conflict, deletes and undoes a deletion, selects a map marker, searches, saves through the actual download event and reopens the saved result. The final Android phase verifies fields, photos, memberships, deletion and retention of raw measurements, newer phone-only data and the route. The two fixture methods are deliberately skipped during the normal test suite unless the fixture system property is provided.

The cloud Chromium enterprise policy blocks `file://` navigation with `ERR_BLOCKED_BY_ADMINISTRATOR`. Do not disable or bypass that policy. In that environment use the explicit fallback:

```sh
python3 tools/browser/test-editor.py /tmp/bodenwelle-browser-fixture --screenshots /tmp/bodenwelle-browser-fixture/screenshots --render-content
```

This renders the same authorized HTML with Playwright `set_content`. The report explicitly states `file_scheme_tested: false`; it does not establish that opening a local file works on a user's computer. The editor's restrictive CSP remains enabled; no `unsafe-eval` is required. Real file navigation remains a manual desktop check.

OpenFreeMap requests are intentionally aborted in the editing test. The script verifies that all HTTP requests go only to the two permitted OpenFreeMap origins, as GETs without application payloads. Screenshots therefore show the labeled offline grid. A separate real network/device check is still required for live street tiles. Firefox, Safari and real Android hardware have not been exercised by this script.

## Map regression and legacy files (0.15.1)

```sh
python3 tools/browser/test-map.py /tmp/bodenwelle-browser-fixture --legacy /path/to/older-project.html
python3 tools/browser/build-editor.py /tmp/Bodenwelle-Browser.html
```

The map regression runs the real WebGL renderer with a deliberately artificial style and road geometry supplied by Playwright routes. It checks map/measurement coordinate alignment, zoom, legacy JSON preservation, HTTP 403, manual retry and the no-WebGL fallback. It sends no live map requests and reports `live_provider_tested: false` and `file_scheme_tested: false`. It does not spoof User-Agent/Referer or proxy the old OSM server. The public OpenFreeMap configuration explicitly permits CORS `*`; service documentation and the old OSM Referer requirement are linked in [the correction note](../../docs/BROWSER-KARTENFEHLER.md).

The cloud HTTPS proxy rejects the live OpenFreeMap endpoint at CONNECT with 403. Do not circumvent this restriction; a live test remains necessary on the user's computer. Provider URL and CSP are restricted to `tiles.openfreemap.org` and `assets.openfreemap.com`. No project data are added to map requests.

Other exchange, migration, photo-metadata and native UI checks live in `BrowserExchangeTest`, `BrowserPhotoMetadataTest` and `BrowserExchangeUiTest`. Run the full Android suite/build/lint with:

```sh
python3 tools/cloud-gradle.py :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```
