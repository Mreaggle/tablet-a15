#!/usr/bin/env bash
# Leitura do aparelho selecionado pelo adb.
set -euo pipefail
a15_adb=${ADB:-adb}
"$a15_adb" get-state >/dev/null
"$a15_adb" shell '
for prop in ro.product.model ro.product.board ro.board.platform ro.hardware \
             ro.product.cpu.abilist ro.treble.enabled ro.build.version.release \
             ro.build.version.sdk ro.build.version.security_patch \
             ro.boot.flash.locked sys.usb.state; do
  printf "%s=" "$prop"
  getprop "$prop"
done
uname -rm
wm size
wm density
df -k /data
awk "\$4 == \"mmcblk0\" {print \"eMMC em KiB:\", \$3}" /proc/partitions
for file in possible present online; do
  printf "CPU %s=" "$file"
  cat "/sys/devices/system/cpu/$file"
done
awk "/^MemTotal:|^MemAvailable:|^SwapTotal:/ {print}" /proc/meminfo
awk "\$1 == \"present\" {n += \$2; found=1}
     END {if(found) printf \"Paginas fisicas presentes: %d\\n\", n}" /proc/zoneinfo
dumpsys webviewupdate | grep "Current WebView package" || true
'
