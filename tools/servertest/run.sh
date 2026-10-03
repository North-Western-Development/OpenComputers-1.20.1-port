#!/bin/bash
# Smoke-test the built jars on real dedicated servers.
#
# Usage: tools/servertest/run.sh <servers-dir> <commands-file>
#
# <servers-dir> must contain two installed servers:
#   forge/   Forge 1.20.1 server (installer --installServer), mods/: architectury-forge + OC forge jar
#   fabric/  Fabric 1.20.1 server (fabric-installer server ... -downloadMinecraft),
#            mods/: fabric-api + architectury-fabric + OC fabric jar
# Both need eula.txt (eula=true) and server.properties (online-mode=false, flat world).
#
# Each line of the commands file is sent to both server consoles; a line "WAIT" pauses 8s.
# Debug commands (/oc_debug start|stop|status <pos>) are enabled for the run. Prints the
# OC debug output, LAMP-* markers and any errors from both logs.
set -u
DIR=$(cd "$1" && pwd); CMDS=$(cd "$(dirname "$2")" && pwd)/$(basename "$2")
grep -q debugCommands "$DIR/forge/user_jvm_args.txt" || printf '\n-Dopencomputers.debugCommands=true\n' >> "$DIR/forge/user_jvm_args.txt"
for d in forge fabric; do
  rm -rf "${DIR:?}/$d/world" "${DIR:?}/$d/crash-reports"
  : > "$DIR/$d/out.log"; : > "$DIR/$d/cmd.txt"
done
(cd "$DIR/forge" && tail -n +1 -f cmd.txt | timeout 900 bash run.sh nogui > out.log 2>&1 &)
(cd "$DIR/fabric" && tail -n +1 -f cmd.txt | timeout 900 java -Xmx1500M -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
until grep -qE 'Done \(|Crash|Failed to start|Exception in thread "main"|Failed to initialize' "$DIR/forge/out.log" && grep -qE 'Done \(|Crash|Failed to start|Exception in thread "main"|Failed to initialize' "$DIR/fabric/out.log"; do sleep 3; done
while IFS= read -r line; do
  if [ "$line" = "WAIT" ]; then sleep 8; else echo "$line" | tee -a "$DIR/forge/cmd.txt" >> "$DIR/fabric/cmd.txt"; fi
done < "$CMDS"
sleep 5
echo stop | tee -a "$DIR/forge/cmd.txt" >> "$DIR/fabric/cmd.txt"
sleep 15
for d in forge fabric; do
  echo "===== $d"
  grep -nE "oc_debug|LAMP|Exception|ERROR|Caused by|^\s+at li\.cil|Crash|Unknown or incomplete|No machine" "$DIR/$d/out.log" \
    | grep -vE "No data fixer registered|No key layers" | cut -c1-300 | head -40
done
