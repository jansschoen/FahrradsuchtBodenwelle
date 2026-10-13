#!/usr/bin/env python3
"""Activate the cloud toolchain, system TLS trust and existing proxy without logging credentials."""
import os
from pathlib import Path
import subprocess
import sys
import urllib.parse

ROOT = Path(__file__).resolve().parents[1]
TOOLS = Path('/workspace/.bodenwelle-tools')
environment = dict(os.environ, JAVA_HOME=str(TOOLS / 'jdk-package/usr/lib/jvm/msopenjdk-21-amd64'),
                   GRADLE_USER_HOME=str(TOOLS / 'gradle-home'),
                   ANDROID_HOME=str(TOOLS / 'android-sdk'),
                   ANDROID_USER_HOME=str(TOOLS / 'android-user'))
java_user = TOOLS / 'java-user'
java_user.mkdir(parents=True, exist_ok=True)
arguments = [str(TOOLS / 'gradle-8.11.1/bin/gradle'), '--no-daemon', '--console=plain',
             '-Duser.home=' + str(java_user)]
trust = Path('/etc/ssl/certs/java/cacerts')
if trust.exists():
    arguments.append('-Djavax.net.ssl.trustStore=' + str(trust))
for scheme in ['http', 'https']:
    proxy = os.environ.get(scheme.upper() + '_PROXY') or os.environ.get('HTTPS_PROXY')
    if proxy:
        parsed = urllib.parse.urlparse(proxy)
        if not parsed.hostname:
            raise SystemExit('The configured proxy has no hostname')
        arguments.extend(['-D' + scheme + '.proxyHost=' + parsed.hostname,
                          '-D' + scheme + '.proxyPort=' + str(parsed.port or 80)])
raise SystemExit(subprocess.call(arguments + sys.argv[1:], env=environment, cwd=ROOT))
