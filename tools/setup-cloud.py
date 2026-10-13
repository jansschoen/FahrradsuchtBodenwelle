#!/usr/bin/env python3
"""Pinned Linux x86_64 toolchain with authoritative artifact checksums.
No source, manifest or lockfile is changed; existing SDK configuration is preserved.
"""
import hashlib
from pathlib import Path
import platform
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = Path('/workspace/.bodenwelle-tools')
DOWNLOADS = TOOLS / 'downloads'

def verified_download(name, url, algorithm, checksum):
    dest = DOWNLOADS / name
    if not dest.exists() or hashlib.new(algorithm, dest.read_bytes()).hexdigest() != checksum:
        temporary = dest.with_suffix(dest.suffix + '.partial')
        try:
            subprocess.run(['curl', '--fail', '--location', '--retry', '2', '--max-time', '240',
                            '--silent', '--show-error', url, '-o', str(temporary)], check=True)
            if hashlib.new(algorithm, temporary.read_bytes()).hexdigest() != checksum:
                raise RuntimeError('Checksum mismatch for ' + name)
            temporary.replace(dest)
        finally:
            temporary.unlink(missing_ok=True)
    return dest

def extract_zip(archive, destination):
    if destination.exists():
        return
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=TOOLS) as staging:
        with zipfile.ZipFile(archive) as content:
            content.extractall(staging)
            for entry in content.infolist():
                mode = entry.external_attr >> 16
                if mode & 0o111:
                    (Path(staging) / entry.filename).chmod(mode & 0o777)
        entries = list(Path(staging).iterdir())
        if len(entries) != 1:
            raise RuntimeError('Unexpected archive layout: ' + str(archive))
        shutil.move(str(entries[0]), str(destination))

def main():
    if platform.system() != 'Linux' or platform.machine() != 'x86_64':
        raise SystemExit('Cloud bootstrap supports Linux x86_64; use Android Studio on other platforms.')
    DOWNLOADS.mkdir(parents=True, exist_ok=True)
    jdk = verified_download('jdk.deb',
            'https://packages.microsoft.com/debian/12/prod/pool/main/m/msopenjdk-21/msopenjdk-21_21.0.12.1-1_amd64.deb',
            'sha256', '80a1d94617caa47fd4b7efef61d5ad19423cb1a6a2ffcbd170b1f39ebe230c8e')
    jdk_java = TOOLS / 'jdk-package/usr/lib/jvm/msopenjdk-21-amd64/bin/javac'
    if not jdk_java.exists():
        subprocess.run(['dpkg-deb', '--extract', str(jdk), str(TOOLS / 'jdk-package')], check=True)
    gradle = verified_download('gradle.zip', 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip',
            'sha256', 'f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6')
    extract_zip(gradle, TOOLS / 'gradle-8.11.1')
    (TOOLS / 'gradle-8.11.1/bin/gradle').chmod(0o755)
    sdk = TOOLS / 'android-sdk'
    sdk_platform = verified_download('platform.zip', 'https://dl.google.com/android/repository/platform-35_r02.zip',
            'sha1', '0bb560a90a7a2cbd0dd8348224d518b638fe7949')
    extract_zip(sdk_platform, sdk / 'platforms/android-35')
    build_tools = verified_download('build-tools.zip', 'https://dl.google.com/android/repository/build-tools_r35_linux.zip',
            'sha1', '2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc')
    extract_zip(build_tools, sdk / 'build-tools/35.0.0')
    (TOOLS / 'android-user').mkdir(exist_ok=True)
    local = ROOT / 'local.properties'
    if not local.exists():
        local.write_text('sdk.dir=' + str(sdk) + '\n')
    print('Verified cloud toolchain available; existing local.properties preserved.')

if __name__ == '__main__':
    main()
