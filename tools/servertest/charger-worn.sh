#!/bin/bash
# Player check: tools/servertest/charger-worn.sh <servers-dir> <forge|fabric> <port> <display>
# Like client-player.sh (same requirements): a player wearing drained hover boots stands next to a
# powered charger; the boots in the armour slot must gain charge (CHARGE-BEFORE / CHARGE-AFTER).
G=$(cd "$1" && pwd); L=$2; PORT=$3; DISP=$4
W=$(cd "$(dirname "$0")/../.." && pwd)
D=$G/$L
export DISPLAY=:$DISP XAUTHORITY=$G/xauth-$L
cmd() { echo "$1" >> "$D/cmd.txt"; }
rm -rf "${D:?}/world"; : > "$D/cmd.txt"; : > "$D/out.log"
if [ "$L" = forge ]; then
  (cd "$D" && tail -n +1 -f cmd.txt | timeout 1200 bash run.sh nogui > out.log 2>&1 &)
else
  (cd "$D" && tail -n +1 -f cmd.txt | timeout 1200 java -Xmx1500M -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
fi
until grep -qE "Done \(" "$D/out.log"; do sleep 3; done
(cd "$W" && timeout 1200 xvfb-run -f "$XAUTHORITY" -n "$DISP" -s "-screen 0 1280x720x24" ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1G ":$L:runClient" --args="--username OCTester --quickPlayMultiplayer localhost:$PORT" > "$G/client-$L.log" 2>&1 &)
until grep -qE "joined the game" "$D/out.log" || grep -qE "Game crashed|BUILD FAILED|FAILURE" "$G/client-$L.log"; do sleep 5; done
P=OCTester; sleep 15
printf 'op %s\ngamemode survival %s\ndifficulty peaceful\nforceload add 48 64 64 80\nsetblock 56 -60 70 opencomputers:casecreative\nsetblock 57 -60 70 opencomputers:charger\nsetblock 57 -59 70 minecraft:redstone_block\ntp %s 58.5 -60 70.5\n' $P $P $P >> "$D/cmd.txt"
cmd "item replace entity $P armor.feet with opencomputers:hoverboots{\"oc:charge\":0.0d}"; sleep 2
cmd "say CHARGE-BEFORE"; cmd "data get entity $P Inventory[{Slot:100b}].tag"; sleep 20
cmd "say CHARGE-AFTER"; cmd "data get entity $P Inventory[{Slot:100b}].tag"; sleep 2
cmd "stop"; sleep 15
for p in $(pgrep -x java) $(pgrep -x Xvfb); do tr '\0' ' ' < "/proc/$p/cmdline" | grep -qE "$W|:$DISP " && kill "$p"; done
for p in $(pgrep -x tail); do [ "$(readlink "/proc/$p/cwd")" = "$(cd "$D" && pwd -P)" ] && kill "$p"; done
grep -aE "CHARGE-|oc:charge|has the following" "$D/out.log" | cut -c1-200
