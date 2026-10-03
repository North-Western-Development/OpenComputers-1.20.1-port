#!/bin/bash
# Player check: tools/servertest/client-player.sh <servers-dir> <forge|fabric> <port> <display>
# Like client.sh (same requirements): starts the <servers-dir>/<loader> server on a fresh world
# with gameplay/player-scene.txt, joins with a dev client and, as a survival player:
#  - hover boots: walks into a 1 block step, jumps at a 2 block wall and falls 13 blocks, first
#    without and then with charged hover boots (prints the player's Y / Health after each:
#    STEP / JUMP / FALL markers), then prints the boots' remaining charge;
#  - nanomachines: eats nanomachines (holding right click), runs gameplay/nano.lua on the computer
#    next to the player (configures the nanomachines over its wireless card, overload damage) and
#    takes a screenshot of the HUD;
#  - tablet: right-clicks a tablet with the Lua BIOS and the OpenOS floppy, types `ls /` and
#    `echo hi` into its shell (oc_debug tablet ... type) and prints its screen;
#  - restarts the server and the client, checks the nanomachines are still configured (nano.lua
#    again) and lets the overload kill the player (death message).
# Screenshots go to <servers-dir>/shots/<loader>-player-*.png.
G=$(cd "$1" && pwd); L=$2; PORT=$3; DISP=$4
W=$(cd "$(dirname "$0")/../.." && pwd)
D=$G/$L
export DISPLAY=:$DISP XAUTHORITY=$G/xauth-$L
mkdir -p "$G/shots"
x() { python3 "$W/tools/servertest/xin.py" "$@"; }
shot() { import -window root "$G/shots/$L-player-$1.png"; }
cmd() { echo "$1" >> "$D/cmd.txt"; }
server() {
  : > "$D/cmd.txt"; : > "$D/out.log"
  if [ "$L" = forge ]; then
    (cd "$D" && tail -n +1 -f cmd.txt | timeout 2400 bash run.sh nogui > out.log 2>&1 &)
  else
    (cd "$D" && tail -n +1 -f cmd.txt | timeout 2400 java -Xmx1500M -Dopencomputers.debugCommands=true -jar fabric-server-launch.jar nogui > out.log 2>&1 &)
  fi
  until grep -qE "Done \(" "$D/out.log"; do sleep 3; done
}
client() {
  (cd "$W" && timeout 2400 xvfb-run -f "$XAUTHORITY" -n "$DISP" -s "-screen 0 1280x720x24" ./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx1G ":$L:runClient" --args="--username OCTester --quickPlayMultiplayer localhost:$PORT" > "$G/client-$L.log" 2>&1 &)
  until grep -qE "joined the game" "$D/out.log" || grep -qE "Game crashed|BUILD FAILED|FAILURE" "$G/client-$L.log"; do sleep 5; done
  P=$(grep -o "[A-Za-z0-9_]* joined the game" "$D/out.log" | head -1 | cut -d' ' -f1)
  echo "player $P"
  sleep 15
}
killclient() {
  for p in $(pgrep -f "runClient|KnotClient|forgeclient|net.minecraft.client.main|GradleWrapperMain|Xvfb"); do
    tr '\0' ' ' < "/proc/$p/cmdline" | grep -qE "$W|:$DISP " && kill "$p"
  done
  sleep 5
}
stopserver() {
  cmd "stop"
  for _ in $(seq 60); do grep -q "All dimensions are saved" "$D/out.log" && break; sleep 2; done
  sleep 5
  for p in $(pgrep -x tail); do [ "$(readlink "/proc/$p/cwd")" = "$(cd "$D" && pwd -P)" ] && kill "$p"; done
  cat "$D/out.log" >> "$D/out.all.log"
}
probe() { cmd "say $1"; cmd "data get entity $P Pos"; cmd "data get entity $P Health"; sleep 1; }
# player walks south onto the platform at x (step: 0, wall: 4), jumping if $2 is set
lane() {
  cmd "tp $P $1.5 -60 80.5 0 10"; sleep 2
  if [ -n "${2:-}" ]; then x down:w sleep:0.25 key:space sleep:1.5 up:w; else x down:w sleep:1.5 up:w; fi
  sleep 1
}
fall() { cmd "tp $P 8.5 -47 84.5 0 10"; sleep 5; }

rm -rf "${D:?}/world"; : > "$D/out.all.log"
server
while IFS= read -r line; do
  case "$line" in WAIT*) sleep 3 ;; *) cmd "$line" ;; esac
done < "$W/tools/servertest/gameplay/player-scene.txt"
client
printf 'op %s\ngamemode survival %s\ndifficulty peaceful\ntime set day\ngamerule doDaylightCycle false\ngamerule doMobSpawning false\ngamerule naturalRegeneration false\ngamerule doImmediateRespawn true\nkill @e[type=!player]\nclear %s\n' "$P" "$P" "$P" >> "$D/cmd.txt"
sleep 3
shot world
# hover boots
lane 0; probe STEP-NOBOOTS
lane 4 jump; probe JUMP-NOBOOTS
fall; probe FALL-NOBOOTS
cmd "effect give $P minecraft:instant_health 1 10 true"; sleep 2
cmd "item replace entity $P armor.feet with opencomputers:hoverboots{\"oc:charge\":15000.0d}"; sleep 1
lane 0; probe STEP-BOOTS
lane 4 jump; probe JUMP-BOOTS
fall; probe FALL-BOOTS
cmd "data get entity $P Inventory[{Slot:100b}].tag"
cmd "effect give $P minecraft:instant_health 1 10 true"
# nanomachines
cmd "tp $P 11.5 -60 81.5 0 30"; sleep 2
cmd "item replace entity $P weapon.mainhand with opencomputers:nanomachines"; sleep 1
x rhold:3; sleep 2
cmd "data get entity $P OpenComputersPersisted"
cmd "item replace entity $P weapon.mainhand with minecraft:air"
cmd "effect clear $P"
cmd "oc_debug start 10 -60 80"; sleep 35
cmd "oc_debug status 10 -60 80"
shot nano-hud
cmd "effect give $P minecraft:instant_health 1 10 true"
# tablet with OpenOS
cmd "$(grep -v '^#' "$W/tools/servertest/gameplay/player-items.txt" | sed -n 1p)"; sleep 2
cmd "tp $P 11.5 -60 81.5 0 -30"; sleep 2
x rclick; sleep 15
cmd "oc_debug tablet $P status"
cmd "oc_debug tablet $P type ls /"; sleep 3
cmd "oc_debug tablet $P type echo hi"; sleep 3
shot tablet
x key:Escape; sleep 1
cmd "oc_debug tablet $P screen"
# OC's damage types exist (armor / effects bypassing via the damage type tags)
cmd "effect give $P minecraft:instant_health 1 10 true"; sleep 1
cmd "item replace entity $P armor.chest with minecraft:diamond_chestplate"
cmd "damage $P 3 opencomputers:nanomachines_hungry"; sleep 1; cmd "damage $P 4 opencomputers:nanomachines_overload"; sleep 1
probe DAMAGE-TYPES
sleep 2
# restart: nanomachines must still be configured
cmd "save-all"; sleep 2
stopserver; killclient
ls "$D"/world/playerdata/
server
client
cmd "tp $P 11.5 -60 81.5 0 30"; sleep 2
cmd "data get entity $P OpenComputersPersisted"
cmd "oc_debug start 10 -60 80"; sleep 45
cmd "oc_debug status 10 -60 80"
shot nano-after-restart
cmd "data get entity $P OpenComputersPersisted"
sleep 3
stopserver; killclient
grep -aE "oc_debug|\[Server\]|has the following|Exception|ERROR|greedy|breakdown|out of control|nanomachines|died|was " "$D/out.all.log" \
  | grep -vE "No data fixer|No key layers|Loaded [0-9]+ |UUID of player" | cut -c1-${CUT:-400}
echo done
