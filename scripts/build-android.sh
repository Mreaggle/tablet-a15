#!/usr/bin/env bash
# Uso: scripts/build-android.sh <dollmaker|otimizador|icones|shorts|princesas|salao>
set -euo pipefail
base=$(cd "$(dirname "$0")/.." && pwd)
app=${1:?Informe o app}
sdk_root=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
sdk=${A15_BUILD_TOOLS:-${sdk_root:+$sdk_root/build-tools/35.0.0}}
jar=${A15_ANDROID_JAR:-${sdk_root:+$sdk_root/platforms/android-28/android.jar}}
key=${A15_KEYSTORE:?Informe A15_KEYSTORE}
password=${A15_KEY_PASS_FILE:?Informe A15_KEY_PASS_FILE}
dist=${A15_DIST:-$base/dist}

case "$app" in
  otimizador) output=Otimizar-A15.apk ;;
  icones) output=Icones-A15.apk ;;
  shorts) output=Shorts-A15.apk ;;
  dollmaker) output=DollMaker-offline.apk ;;
  princesas) output=Princesas-A15.apk; label=Princesas; orientation=unspecified ;;
  salao) output=Salao-A15.apk; label=Salão; orientation=sensorLandscape ;;
  *) printf 'App desconhecido: %s\n' "$app" >&2; exit 2 ;;
esac

test -x "$sdk/aapt2" && test -f "$jar" && test -f "$key" && test -f "$password"
mkdir -p "$dist"
dist=$(cd "$dist" && pwd)
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
mkdir -p "$build/classes" "$build/dex" "$build/gen"

component="$base/apps/$app"
min_api=26
if [[ "$app" == princesas || "$app" == salao ]]; then
  component="$base/apps/jogos-offline"
  assets="$component/web/$app"
  mkdir -p "$build/res/drawable"
  cp "$component/icons/$app.xml" "$build/res/drawable/game.xml"
  res="$build/res"
  manifest="$build/AndroidManifest.xml"
  cat > "$manifest" <<XML
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.tableta15.$app" android:versionCode="1" android:versionName="1.0">
  <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="28" />
  <application android:label="$label" android:icon="@drawable/game"
      android:theme="@android:style/Theme.Material.Light.NoActionBar"
      android:allowBackup="false" android:usesCleartextTraffic="false">
    <activity android:name="org.tableta15.offlinegame.MainActivity" android:exported="true"
        android:screenOrientation="$orientation" android:configChanges="keyboardHidden|orientation|screenSize">
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
  </application>
</manifest>
XML
else
  res="$component/res"
  manifest="$component/AndroidManifest.xml"
  assets="$component/assets"
  case "$app" in
    dollmaker) min_api=23 ;;
    shorts) min_api=24 ;;
  esac
fi

extra_assets=()
if [[ -d "$assets" ]]; then extra_assets=(-A "$assets"); fi
"$sdk/aapt2" compile --dir "$res" -o "$build/res.zip"
"$sdk/aapt2" link --manifest "$manifest" -I "$jar" "${extra_assets[@]}"     --java "$build/gen" -o "$build/unsigned.apk" "$build/res.zip"
mapfile -t sources < <(find "$component/src" "$build/gen" -name '*.java')
if [[ -n ${A15_ECJ_JAR:-} ]]; then
  java -jar "$A15_ECJ_JAR" -1.8 -proc:none -classpath "$jar" -d "$build/classes" "${sources[@]}"
else
  javac -source 8 -target 8 -proc:none -bootclasspath "$jar" -d "$build/classes" "${sources[@]}"
fi
mapfile -t classes < <(find "$build/classes" -name '*.class')
"$sdk/d8" --min-api "$min_api" --lib "$jar" --output "$build/dex" "${classes[@]}"
(cd "$build/dex" && zip -q "$build/unsigned.apk" classes.dex)
"$sdk/zipalign" -f -p 4 "$build/unsigned.apk" "$build/aligned.apk"
"$sdk/apksigner" sign --ks "$key" --ks-pass "file:$password"     --out "$build/$output" "$build/aligned.apk"
"$sdk/apksigner" verify "$build/$output"
cp "$build/$output" "$dist/$output"
printf '%s\n' "$dist/$output"
