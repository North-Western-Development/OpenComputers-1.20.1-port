#!/bin/bash
# Recorded feature walkthrough: tools/servertest/walkthrough.sh <servers-dir> <forge|fabric> <port> <display> <out.mp4>
# Like client.sh (same requirements, plus ffmpeg): starts the <servers-dir>/<loader> server on a
# fresh world with gameplay/walkthrough.txt, joins with a dev client (1280x720 under xvfb) and
# records the screen while it visits every station with on-screen captions (/title): OpenOS typed
# into through the screen GUI, screen re-merging after a chunk reload, robot, drone, hologram,
# network + relay, 3D printer (GUI, placing the print), assembler / disassembler / charger / rack /
# RAID / disk drive GUIs, microcontroller + redstone, manual (also opened on a block), OpenOS
# tablet and hover boots. Raw recording: <out>.mkv; final H.264 video: <out.mp4>.
G=$(cd "$1" && pwd); L=$2; PORT=$3; DISP=$4; OUT=$5
W=$(cd "$(dirname "$0")/../.." && pwd)
D=$G/$L
export DISPLAY=:$DISP XAUTHORITY=$G/xauth-$L
rm -rf "${D:?}/world"; : > "$D/cmd.txt"; : > "$D/out.log"
if [ "$L" = forge ]; then
  (cd "$D" && tail -n +1 -f cmd.txt | timeout 3600 bash run.sh nogui > out.log 2>&1 &)
else
  (cd "$D" && tail -n +1 -f cmd.txt | timeout 3600 java -Xmx1500M -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
fi
until grep -qE "Done \(" "$D/out.log"; do sleep 3; done
cmd() { echo "$1" >> "$D/cmd.txt"; }
cmd "gamerule sendCommandFeedback false"
cmd "gamerule doDaylightCycle false"
cmd "gamerule doWeatherCycle false"
cmd "gamerule doMobSpawning false"
cmd "time set 6000"
while IFS= read -r line; do
  case "$line" in WAIT*) sleep 3 ;; *) echo "$line" >> "$D/cmd.txt" ;; esac
done < "$W/tools/servertest/gameplay/walkthrough.txt"
(cd "$W" && timeout 3600 xvfb-run -f "$XAUTHORITY" -n "$DISP" -s "-screen 0 1280x720x24" ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1G ":$L:runClient" --args="--username OCDemo --width 1280 --height 720 --quickPlayMultiplayer localhost:$PORT" > "$G/client-$L.log" 2>&1 &)
until grep -qE "joined the game" "$D/out.log" || grep -qE "Game crashed|BUILD FAILED|FAILURE" "$G/client-$L.log"; do sleep 5; done
P=$(grep -o "[A-Za-z0-9_]* joined the game" "$D/out.log" | head -1 | cut -d' ' -f1)
echo "player $P"
cmd "op $P"; cmd "gamemode spectator $P"; cmd "kill @e[type=!player,type=!opencomputers:drone]"
cmd "title $P times 10 90 15"
x() { python3 "$W/tools/servertest/xin.py" "$@"; }
# cam x y z yaw pitch: put the camera (the player) there
cam() { cmd "tp $P $1 $2 $3 $4 $5"; }
# pan x1 y1 z1 yaw1 pitch1 x2 y2 z2 yaw2 pitch2 seconds: smooth camera move (10 steps per second)
pan() {
  python3 - "$D/cmd.txt" "$P" "$@" <<'EOF'
import sys, time
f, p = sys.argv[1], sys.argv[2]
a = list(map(float, sys.argv[3:8])); b = list(map(float, sys.argv[8:13])); secs = float(sys.argv[13])
n = max(1, int(secs * 10))
for i in range(n + 1):
    t = i / n; t = t * t * (3 - 2 * t)
    v = [a[k] + (b[k] - a[k]) * t for k in range(5)]
    with open(f, "a") as o: o.write("tp %s %.3f %.3f %.3f %.2f %.2f\n" % (p, *v))
    time.sleep(secs / n)
EOF
}
# say title subtitle: on-screen caption
say() { cmd "title $P subtitle {\"text\":\"$2\",\"color\":\"gray\"}"; cmd "title $P title {\"text\":\"$1\",\"color\":\"aqua\",\"bold\":true}"; }
# look at block (bx, -60, bz) from the north as a creative player, then right click it
gui() { # x z [seconds]
  cmd "tp $P $1.5 -60 $(($2 - 2)).5 0 36"; sleep 1.5; x rclick; sleep "${3:-4}"; x key:Escape; sleep 1
}
sleep 25  # let the client settle (chunks, OpenOS boot)

ffmpeg -loglevel error -y -f x11grab -framerate 25 -video_size 1280x720 -i ":$DISP" -c:v libx264 -preset ultrafast -crf 20 "${OUT%.mp4}.mkv" < /dev/null &
FF=$!
sleep 1

# Intro: overview of the stations
cam -6 -50 40 -60 30
say "OpenComputers" "Minecraft 1.20.1 port: Forge, NeoForge and Fabric"
sleep 3
pan -6 -50 40 -60 30  100 -50 40 60 30  14
# A: OpenOS
cam 0.5 -59 55.5 0 8
say "OpenOS" "A creative computer with a 3x2 tier 3 screen and a keyboard"
sleep 5
cmd "gamemode creative $P"
cmd "tp $P 0.5 -60 57.5 0 -2"; sleep 2
x rclick; sleep 2
x type:"ls /" key:Return sleep:1.5 type:components key:Return sleep:2.5
cmd "oc_debug type 0 -60 61 echo \"local g=require('component').gpu local w,h=g.getResolution() for i=0,w-1 do local f=function(o) return math.floor(127+127*math.sin(i/8+o)) end g.setBackground(f(0)*65536+f(2)*256+f(4)) g.fill(i+1,h-5,1,6,' ') end g.setBackground(0) g.set(2,h-7,'Hello from OpenComputers on Minecraft 1.20.1!')\" > /tmp/demo.lua"
sleep 1.5
x type:/tmp/demo.lua key:Return sleep:4
x key:Escape; sleep 1
cmd "gamemode spectator $P"
pan 0.5 -60 57.5 0 -2  0.5 -59 55 0 5  3
sleep 3
# Screen re-merge after a chunk reload
say "Multi-block screens" "Leaving and coming back: the screen re-merges right away"
sleep 4
cam 4000 120 4000 0 0; sleep 8
cam 0.5 -59 55 0 5; sleep 6
# B: robot
pan 0.5 -59 55 0 5  12.5 -56.5 54 0 32  3
say "Robots" "A tier 3 robot running a program from its EEPROM"
sleep 12
# C: drone
pan 12.5 -56.5 54 0 32  24.5 -57.5 51 0 0  3
say "Drones" "Flying a loop, changing light colour and status text"
sleep 15
# D: hologram
pan 24.5 -57.5 51 0 0  36.5 -57.6 55.5 0 18  3
say "Hologram projector" "Tier 2: three colours, drawn by Lua and rotating"
sleep 14
# E: network
pan 36.5 -57.6 55.5 0 18  46.5 -59.4 58 0 0  3
say "Networking" "Network cards, cables and a relay: A broadcasts, B receives"
sleep 5
pan 46.5 -59.4 58 0 0  54.5 -59.4 58 0 0  5
sleep 4
# F: 3D printer
say "3D printer" "A computer printed two lamps; the print is placed in the world"
cmd "gamemode creative $P"
gui 61 61 5
cmd "item replace entity $P weapon.mainhand from block 61 -60 61 container.2"
cmd "tp $P 61.5 -60 58.6 0 50"; sleep 2; x rclick; sleep 1
cmd "item replace entity $P weapon.mainhand with minecraft:air"
cmd "gamemode spectator $P"
pan 61.5 -60 58.6 0 50  62.5 -59 57.5 20 20  3
sleep 3
# G: machines
cmd "gamemode creative $P"
cmd "oc_debug start 66 -60 61"
say "Robot assembler" "A computer starts assembling a robot from a tier 1 case"
gui 67 61 6
say "Disassembler" "Taking a CPU apart, ingredient by ingredient"
gui 71 61 4
say "Charger" "Charging the robot in front of it (redstone enabled)"
gui 75 60 4
say "Server rack" "Servers, rack-mounted, with network sides"
gui 79 61 4
say "RAID and disk drive" "Three hard drives in a RAID; OpenOS floppy in a disk drive"
gui 81 61 3
gui 83 61 3
say "Relay" "Bridges networks, with upgradable buffers"
gui 85 61 3
say "Case" "The computer case of the OpenOS station"
cmd "tp $P 0.5 -60 63.5 180 36"; sleep 1.5; x rclick; sleep 4; x key:Escape; sleep 1
# H: microcontroller
cmd "gamemode spectator $P"
cam 93.5 -58.8 57 0 10
say "Microcontroller" "Redstone card blinking three lamps"
sleep 8
# Manual
cmd "gamemode creative $P"
cmd "tp $P 75.5 -60 58.5 0 36"; sleep 2
say "The manual" "Right click opens it (no arm swing); on a block, its page"
cmd "$(sed -n 1p "$W/tools/servertest/gameplay/walkthrough-items.txt")"; sleep 3
cmd "tp $P 80 -59 50 0 -20"; sleep 1.5
x rclick; sleep 3; x scroll:4; sleep 2; x key:Escape; sleep 1
cmd "tp $P 75.5 -60 58.5 0 36"; sleep 1.5
x rclick; sleep 4; x key:Escape; sleep 1
# Tablet
say "Tablet" "A tablet booting OpenOS from its floppy"
cmd "$(sed -n 2p "$W/tools/servertest/gameplay/walkthrough-items.txt")"; sleep 2
cmd "tp $P 80 -59 50 0 -20"; sleep 1.5
x rclick; sleep 14
cmd "oc_debug tablet $P type ls /"; sleep 3
cmd "oc_debug tablet $P type echo Hello from a tablet!"; sleep 3
x key:Escape; sleep 1
cmd "item replace entity $P weapon.mainhand with minecraft:air"
# I: hover boots
cmd "item replace entity $P armor.feet with opencomputers:hoverboots{\"oc:charge\":15000.0d}"
cmd "tp $P 105.5 -60 61.5 0 15"; sleep 2
say "Hover boots" "Step up full blocks and jump higher"
sleep 3
x down:w sleep:7 up:w
sleep 1
x key:space sleep:2
cmd "gamemode spectator $P"
# Outro
cam 50 -45 30 0 35
say "OpenComputers 1.20.1" "Forge / NeoForge / Fabric via Architectury"
pan 50 -45 30 0 35  50 -48 40 0 40  8
sleep 2

kill -INT $FF; wait $FF
ffmpeg -loglevel error -y -i "${OUT%.mp4}.mkv" -c:v libx264 -preset slow -crf 26 -pix_fmt yuv420p -movflags +faststart "$OUT"
cmd "oc_debug status 0 -60 61"; cmd "oc_debug status 66 -60 61"; cmd "stop"
sleep 15
for p in $(pgrep -f "runClient|forgeclient|KnotClient|net.minecraft.client.main|GradleWrapperMain"); do
  tr '\0' ' ' < /proc/$p/cmdline | grep -q "$W" && kill $p
done
grep -E "oc_debug|Exception|ERROR" "$D/out.log" | grep -vE "No data fixer|No key layers|api.github.com" | cut -c1-300
echo done
