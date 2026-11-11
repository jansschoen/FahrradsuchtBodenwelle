#!/usr/bin/env python3
"""Assemble the single-file desktop editor, using the same assets as Android's export."""
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[2]

def document():
    assets = ROOT / 'app/src/main/assets/browser'
    template = (assets / 'editor.html').read_text()
    for token, filename in [('LICENSE', 'LICENSE.txt'), ('STYLE', 'maplibre-gl.css'), ('LIBRARY', 'maplibre-gl.js')]:
        content = (assets / 'vendor' / filename).read_text()
        if token != 'STYLE':
            content = content.replace('</script', '<\\/script')
        template = template.replace('__BODENWELLE_MAP_' + token + '__', content)
    return template.replace('__BODENWELLE_PROJECT_DATA__', 'null')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    args.output.write_text(document())
