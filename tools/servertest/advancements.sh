#!/bin/bash
# Advancement check: tools/servertest/advancements.sh <servers-dir> <forge|fabric> <port> <display>
# Starts the <servers-dir>/<loader> server (see run.sh) on a fresh world, joins with a dev client
# (:<loader>:runClient under xvfb on :<display>; advancements need a real player), then:
#   - revokes everything and grants opencomputers:root and all its children ("Granted the
#     criteria ... 37 advancements"), revokes them again;
#   - gives items (inventory_changed criteria, incl. the OpenOS floppy with its NBT) and drops a
#     robot / drone item at the player (picked up -> opencomputers:assembled criterion);
#   - prints the "has made the advancement" lines and any advancement loading errors.
# <port> must be the server-port in that server's server.properties.
G=$(cd "$1" && pwd); L=$2; PORT=$3; DISP=$4
W=$(cd "$(dirname "$0")/../.." && pwd)
D=$G/$L
export DISPLAY=:$DISP XAUTHORITY=$G/xauth-$L
rm -rf "${D:?}/world"; : > $D/cmd.txt; : > $D/out.log
if [ "$L" = forge ]; then
  (cd $D && tail -n +1 -f cmd.txt | timeout 1200 bash run.sh nogui > out.log 2>&1 &)
else
  (cd $D && tail -n +1 -f cmd.txt | timeout 1200 java -Xmx1500M -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
fi
until grep -qE "Done \(" $D/out.log; do sleep 3; done
cd $W
(timeout 1200 xvfb-run -f $XAUTHORITY -n $DISP -s "-screen 0 1280x720x24" ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1G :$L:runClient --args="--quickPlayMultiplayer localhost:$PORT" > $G/client-$L.log 2>&1 &)
until grep -qE "joined the game" $D/out.log || grep -qE "Game crashed|BUILD FAILED|FAILURE" $G/client-$L.log; do sleep 5; done
P=$(grep -o "[A-Za-z0-9_]* joined the game" $D/out.log | head -1 | cut -d' ' -f1)
echo "player $P"
cmd() { echo "$1" >> $D/cmd.txt; sleep ${2:-1}; }
cmd "op $P"
cmd "gamemode survival $P"
cmd "time set day"
cmd "clear $P"
cmd "advancement revoke $P everything" 2
cmd "advancement grant $P from opencomputers:root" 2
cmd "advancement revoke $P from opencomputers:root" 2
cmd "advancement grant $P only opencomputers:server"
cmd "advancement revoke $P only opencomputers:server" 2
for i in manual transistor chip2 cpu1 ram3 case1 screen2 hologram1 diskdrive floppy raid card redstonecard2 \
         graphicscard1 lancard wlancard1 cable powerdistributor relay adapter disassembler capacitor assembler \
         charger motionsensor geolyzer redstone eeprom hdd1 rack server2 keyboard; do
  cmd "give $P opencomputers:$i"
done
cmd 'give '$P' opencomputers:floppy{"oc:lootFactory":"opencomputers:openos","oc:data":{"oc:fs.label":"openos"}}'
cmd "clear $P" # 36 items so far: make room for the pickups
for i in robot drone tablet microcontroller; do
  cmd "execute at $P run summon item ~ ~ ~ {Item:{id:\"opencomputers:$i\",Count:1b},PickupDelay:0s}" 2
done
sleep 5
# The root has no toast / chat message; it must be granted already (the manual).
cmd "advancement grant $P only opencomputers:root" 2
cmd "stop"
sleep 15
for p in $(pgrep -f "runClient|KnotClient|forgeclient|net.minecraft.client.main|GradleWrapperMain"); do
  tr '\0' ' ' < /proc/$p/cmdline | grep -q "$W" && kill $p
done
grep -aE "Granted|Revoked|grant|made the advancement|reached the goal|dvancement|Exception|ERROR" $D/out.log | grep -vE "No data fixer|No key layers" | cut -c1-200
echo "made after the grant/revoke checks: $(awk '/Revoked the advancement/{f=1} f' $D/out.log | grep -ac 'has made the advancement\|has reached the goal')"
echo done
