# Local HTML editor checks

The Android export embeds `app/src/main/assets/browser/editor.html` and its data in one HTML file. The browser has no runtime library dependencies. The automation below uses Python Playwright and a separately installed Chromium (`/usr/bin/chromium`). Neither is a dependency of the distributed editor.

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

OSM tile requests are intentionally aborted in automation. The script verifies that all HTTP requests are tile GETs without application payloads. Screenshots therefore show the labeled offline grid. A separate real network/device check is still required for loaded street tiles. Firefox, Safari and real Android hardware have not been exercised by this script.

Other exchange, migration, photo-metadata and native UI checks live in `BrowserExchangeTest`, `BrowserPhotoMetadataTest` and `BrowserExchangeUiTest`. Run the full Android suite/build/lint with:

```sh
python3 tools/cloud-gradle.py :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```
