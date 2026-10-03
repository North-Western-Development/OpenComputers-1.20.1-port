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
# Each line of the commands file is sent to both server consoles. Special lines:
#   WAIT        pause 8s
#   WAIT <n>    pause n seconds
#   RESTART     stop both servers, wait until they exited and start them again (same world)
# Debug commands (/oc_debug ...) are enabled for the run. Prints the OC debug output, LAMP-*
# markers and any errors from both logs. LOADERS="forge" (or "fabric") runs only one server.
set -u
DIR=$(cd "$1" && pwd); CMDS=$(cd "$(dirname "$2")" && pwd)/$(basename "$2")
LOADERS=${LOADERS:-forge fabric}
XMX=${XMX:-1500M}
grep -q debugCommands "$DIR/forge/user_jvm_args.txt" || printf '\n-Dopencomputers.debugCommands=true\n' >> "$DIR/forge/user_jvm_args.txt"
grep -q -- '-Xmx' "$DIR/forge/user_jvm_args.txt" || printf '\n-Xmx%s\n' "$XMX" >> "$DIR/forge/user_jvm_args.txt"
for d in $LOADERS; do
  rm -rf "${DIR:?}/$d/world" "${DIR:?}/$d/crash-reports"
  : > "$DIR/$d/out.all.log"
done

start() {
  for d in $LOADERS; do
    : > "$DIR/$d/out.log"; : > "$DIR/$d/cmd.txt"
    if [ "$d" = forge ]; then
      (cd "$DIR/forge" && tail -n +1 -f cmd.txt | timeout 900 bash run.sh nogui > out.log 2>&1 &)
    else
      (cd "$DIR/fabric" && tail -n +1 -f cmd.txt | timeout 900 java -Xmx$XMX -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
    fi
  done
  sleep 5
  for d in $LOADERS; do
    until grep -qE 'Done \(|Crash|Failed to start|Exception in thread "main"|Failed to initialize' "$DIR/$d/out.log" || [ -z "$(jvm $d)" ]; do sleep 3; done
  done
}

send() {
  for d in $LOADERS; do echo "$1" >> "$DIR/$d/cmd.txt"; done
}

# PIDs of the processes named $2 (default: java, the server JVM) running in $DIR/$1.
jvm() {
  for p in $(pgrep -x "${2:-java}"); do
    [ "$(readlink "/proc/$p/cwd")" = "$(cd "$DIR/$1" && pwd -P)" ] && echo "$p"
  done
}

stop() {
  send stop
  for d in $LOADERS; do
    for _ in $(seq 90); do [ -z "$(jvm $d)" ] && break; sleep 1; done
    p=$(jvm $d)
    if [ -n "$p" ]; then
      echo "ERROR [run.sh] $d server did not exit after stop, thread dump:" >> "$DIR/$d/out.log"
      jstack "$p" >> "$DIR/$d/out.log" 2>&1; kill -9 "$p"; sleep 1
    fi
    t=$(jvm $d tail); [ -n "$t" ] && kill $t
    cat "$DIR/$d/out.log" >> "$DIR/$d/out.all.log"
  done
}

start
while IFS= read -r line; do
  case "$line" in
    WAIT) sleep 8 ;;
    "WAIT "*) sleep "${line#WAIT }" ;;
    RESTART) stop; echo "[run.sh] restarting servers"; start ;;
    *) send "$line" ;;
  esac
done < "$CMDS"
sleep 5
stop
for d in $LOADERS; do
  echo "===== $d"
  grep -anE "oc_debug|LAMP|\[Server\]|has the following|No drones|Exception|ERROR|Caused by|^\s+at li\.cil|Crash|Unknown or incomplete|No machine|Incorrect argument|force load|not loaded" "$DIR/$d/out.all.log" \
    | grep -vE "No data fixer registered|No key layers" | cut -c1-${CUT:-300} | head -${HEAD:-40}
done
