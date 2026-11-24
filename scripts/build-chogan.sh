#!/usr/bin/env bash
# Uso: scripts/build-chogan.sh <APK oficial Chogan 0.38.0> <apktool_3.0.3.jar>
set -euo pipefail
base=$(cd "$(dirname "$0")/.." && pwd)
original=${1:?Informe o APK oficial do Chogan 0.38.0}
apktool=${2:?Informe o JAR do Apktool 3.0.3}
sdk_root=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
sdk=${A15_BUILD_TOOLS:-${sdk_root:+$sdk_root/build-tools/35.0.0}}
key=${A15_KEYSTORE:?Informe A15_KEYSTORE}
password=${A15_KEY_PASS_FILE:?Informe A15_KEY_PASS_FILE}
dist=${A15_DIST:-$base/dist}
expected=578d351c89dcd371475b7ac0adb4486fbcf71da50cd40d3c7f733e9ff9797cdf
printf '%s  %s\n' "$expected" "$original" | sha256sum -c
mkdir -p "$dist"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
java -jar "$apktool" d -s -o "$work/chogan" "$original"
patch -d "$work/chogan" -p1 < "$base/apps/chogan/webview-66.patch"
cp "$base/apps/chogan/LICENSE.txt" "$work/chogan/assets/public/LICENSE.txt"
python3 - "$work/chogan" <<'PY'
from pathlib import Path
import sys

root = Path(sys.argv[1])
old = 'io.github.choganhq.chogan'
new = 'org.tableta15.chogan'
manifest = root / 'AndroidManifest.xml'
content = manifest.read_text().replace('package="' + old + '"', 'package="' + new + '"')
for suffix in ('.share', '.androidx-startup', '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'):
    content = content.replace(old + suffix, new + suffix)
manifest.write_text(content)
shortcuts = root / 'res/xml/shortcuts.xml'
shortcuts.write_text(shortcuts.read_text().replace('android:targetPackage="' + old + '"',
    'android:targetPackage="' + new + '"'))
config = root / 'apktool.yml'
config.write_text(config.read_text().replace('versionCode: 60', 'versionCode: 62')
    .replace('versionName: 0.38.0', 'versionName: 0.38.0-a15.2'))
PY
java -jar "$apktool" b -f "$work/chogan" -o "$work/unsigned.apk"
"$sdk/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
"$sdk/apksigner" sign --ks "$key" --ks-pass "file:$password"     --out "$work/Chogan-A15.apk" "$work/aligned.apk"
"$sdk/apksigner" verify "$work/Chogan-A15.apk"
cp "$work/Chogan-A15.apk" "$dist/Chogan-A15.apk"
printf '%s\n' "$dist/Chogan-A15.apk"
