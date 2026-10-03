#!/bin/bash
# Client GUI check: tools/servertest/client.sh <servers-dir> <forge|fabric> <port> <display>
# Starts the <servers-dir>/<loader> server (see run.sh) on a fresh world with
# gameplay/client-scene.txt, connects a dev client (:<loader>:runClient under xvfb on :<display>)
# and right-clicks every OC block / item with a GUI, taking screenshots into
# <servers-dir>/shots/<loader>-<name>.png; types "hello" into a screen and checks that the
# computer behind it got it (oc_debug status prints "RESULT typed=hello"). <port> must be the
# server-port in that server's server.properties. Needs python3-xlib and ImageMagick.
G=$(cd "$1" && pwd); L=$2; PORT=$3; DISP=$4
W=$(cd "$(dirname "$0")/../.." && pwd)
D=$G/$L
export DISPLAY=:$DISP XAUTHORITY=$G/xauth-$L
mkdir -p $G/shots
rm -rf "${D:?}/world"; : > $D/cmd.txt; : > $D/out.log
if [ "$L" = forge ]; then
  (cd $D && tail -n +1 -f cmd.txt | timeout 2400 bash run.sh nogui > out.log 2>&1 &)
else
  (cd $D && tail -n +1 -f cmd.txt | timeout 2400 java -Xmx1500M -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
fi
until grep -qE "Done \(" $D/out.log; do sleep 3; done
while IFS= read -r line; do
  case "$line" in WAIT*) sleep 3 ;; *) echo "$line" >> $D/cmd.txt ;; esac
done < $W/tools/servertest/gameplay/client-scene.txt
cd $W
(timeout 2400 xvfb-run -f $XAUTHORITY -n $DISP -s "-screen 0 1280x720x24" ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1G :$L:runClient --args="--quickPlayMultiplayer localhost:$PORT" > $G/client-$L.log 2>&1 &)
until grep -qE "joined the game" $D/out.log || grep -qE "Game crashed|BUILD FAILED|FAILURE" $G/client-$L.log; do sleep 5; done
P=$(grep -o "[A-Za-z0-9_]* joined the game" $D/out.log | head -1 | cut -d' ' -f1)
echo "player $P"
printf 'op %s\ngamemode creative %s\ntime set day\ngamerule doDaylightCycle false\ngamerule doMobSpawning false\nkill @e[type=!player,type=!opencomputers:drone]\n' $P $P >> $D/cmd.txt
sleep 20
x() { python3 $W/tools/servertest/xin.py "$@"; }
shot() { import -window root $G/shots/$L-$1.png; }
cmd() { echo "$1" >> $D/cmd.txt; }
# look at block (bx, -60, 60) from the north, then right click it
look() { cmd "tp $P $1.5 -60 58.5 0 ${2:-36}"; sleep 2; }
gui() { # name x [pitch] [sneak]
  look $2 $3; x rclick; sleep 3; shot $1; x key:Escape; sleep 1
}
shot world
cmd "clear $P"; sleep 1
gui case 0
gui robot 3
gui drone 6 30
gui diskdrive 9
gui raid 12
gui rack 15
gui assembler 18
gui disassembler 21
gui printer 24
gui charger 27
gui relay 30
gui adapter 33
# keyboard -> screen
look 40; x rclick; sleep 3; x type:hello sleep:1; shot screen; x key:Return; sleep 2; x key:Escape; sleep 1
cmd "oc_debug status 40 -60 61"
# items used in hand
IT=$W/tools/servertest/gameplay/client-items.txt
item() { cmd "$(sed -n "${2}p" $IT)"; sleep 2; cmd "tp $P 0.5 -60 50.5 0 -30"; sleep 2; x rclick; sleep 3; shot $1; x key:Escape; sleep 1; }
item manual 1
item database 2
item server 3
item tablet 4
cmd "item replace entity $P weapon.mainhand with minecraft:air"
cmd "stop"
sleep 15
for p in $(pgrep -f "runClient|KnotClient|forgeclient|net.minecraft.client.main|GradleWrapperMain"); do
  tr '\0' ' ' < /proc/$p/cmdline | grep -q "$W" && kill $p
done
grep -E "oc_debug|Exception|ERROR" $D/out.log | grep -vE "No data fixer|No key layers" | cut -c1-300
echo done
