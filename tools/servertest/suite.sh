#!/bin/bash
# Full server test suite: tools/servertest/suite.sh <servers-dir> [test-name...]
#
# Runs the command files below with run.sh on <servers-dir> (forge/ and fabric/, see run.sh) and
# checks each loader's log for the expected markers (the "Expected" column of README.md).
# Prints PASS / FAIL per test and loader (with the missing markers) and a summary; logs are kept
# in <servers-dir>/suite-logs/<test>-<loader>.log. Without test names, runs every test whose
# requirements are met.
#
# Integration tests copy the other mods' jars into mods/ for their run and remove them again:
#   EXTRA_FORGE=<dir>   jars for Forge (AE2 + GuideME, CC:Tweaked, JEI, Jade, TIS-3D + Markdown
#                       Manual, Mekanism, EnderStorage + CodeChickenLib, ProjectRed + CBMultipart)
#   EXTRA_FABRIC=<dir>  jars for Fabric (AE2, CC:Tweaked, JEI, Jade, TIS-3D + Markdown Manual +
#                       Forge Config API Port); Fabric Loader >= 0.19.4 for JEI
# Without them the integration tests are skipped. LOADERS limits the loaders as for run.sh
# (e.g. LOADERS=forge with a NeoForge server installed in forge/).
set -u
DIR=$(cd "$1" && pwd); shift
T=$(cd "$(dirname "$0")" && pwd)
ALL_LOADERS=${LOADERS:-forge fabric}
LOGS=$DIR/suite-logs; mkdir -p "$LOGS"

# name | command file (relative to tools/servertest) | loaders (all|forge) | extra mods (0|1) | markers
# Markers are extended regexes separated by " && "; "N*regex" needs at least N matching lines.
TESTS=$(cat <<'EOF'
blocks|blocks.txt|all|0|Done \(
boot|boot.txt|all|0|OC-TEST-OK Lua 5\.3 && LAMP-LIT
live|live.txt|all|0|LIVE [0-9.]+s gpu=true text=he
openos|openos.txt|all|0|running=true .*lastError=null
persistence|gameplay/persistence.txt|all|0|2*RESULT n=80 co=80 hdd=80 tmp=80 starts=1 eeprom=true label=ptest && RESULT drone n=80 && running=true .*lastError=null
robot|gameplay/robot.txt|all|0|RESULT inv=16\.0 && swing=true,block && ROBOT-MINED-SOUTH && ROBOT-PLACED-WEST && ROBOT-HOME
upgrades|gameplay/upgrades.txt|all|0|RESULT icSize=27\.0 && craft=true,4\.0 && genCount=1\.0 && PISTON-PUSHED
drone|gameplay/drone.txt|all|0|pos=22\.50,-56\.50,20\.50 && DRONE-LANDED-EAST
network|gameplay/network.txt|all|0|lan:1\.0:0:re:wired && tun:0\.0:0:re:linked && wlan:2\.0:0:re:air
filesystem|gameplay/filesystem.txt|all|0|RESULT drive=true && fsCount=4 && mv=true
components|gameplay/components.txt|all|0|8*RESULT && LAMP-LIT
machines|gameplay/machines.txt|all|0|done=idle,false && opencomputers:robot && charged=true && RESULT server comps=computer,eeprom,filesystem,modem && MC-LAMP-LIT && RESULT mc comps=.*redstone && iron_nugget
navsign|gameplay/navsign.txt|all|0|makeMap=true && facing=3\.0 && signSet=robot\|was here && item4=minecraft:diamondx3\.0
chunkloader|gameplay/chunkloader.txt|all|0|CL-R1-LOADED && CL-R1-MOVED && CL-R2-UNLOADED && CL-R3-LOADED && CL-R3-MOVED && CL-R1-RELEASED && CL-R2-DID-NOT-MOVE && CL-R3-RESTORED && CL-R3-RELEASED
shell|gameplay/shell.txt|all|0|SHELL-ROBOT-MOVED && fwd +true && f\.lua +x
disassembler|gameplay/disassembler.txt|all|0|3*iron_nugget
hoverboots|gameplay/hoverboots.txt|all|0|"oc:charge": 400\.0d && "oc:charge": 2400\.0d
computercraft|integrations/computercraft.txt|all|1|RESULT mon=7\.0x5\.0 && drive=true,label=occc && ccmsg=hi
jei-jade|integrations/jei-jade.txt|all|1|running=true .*lastError=null
ae2|integrations/ae2.txt|all|1|RESULT OK comps=database,me_controller,me_exportbus,me_interface
ae2-crafting|integrations/ae2_crafting.txt|all|1|RESULT OK craftables=1 && done=true
tis3d|integrations/tis3d.txt|all|1|RESULT ports=2 write=true a->b=42\.0 b->a=-7\.0
mekanism|integrations/mekanism.txt|forge|1|tank1=gas,mekanism:hydrogen,Hydrogen,1234\.0/64000\.0
enderstorage|integrations/enderstorage.txt|forge|1|freq1=1\.0,2\.0,3\.0 && colors=orange,magenta,light_blue && bad=false
projectred|integrations/projectred.txt|forge|1|RESULT n=2 before=0\.0 on=200\.0 off=0\.0 back=255\.0
EOF
)
# Log lines that fail any test (besides missing markers).
FAILS='Could not persist|Crash|Exception in server tick loop|ERROR \[run.sh\]|^\s+at li\.cil'

extra() { # add|remove loader
  local src; [ "$2" = forge ] && src=${EXTRA_FORGE:-} || src=${EXTRA_FABRIC:-}
  [ -n "$src" ] || return 0
  for j in "$src"/*.jar; do
    if [ "$1" = add ]; then cp "$j" "$DIR/$2/mods/"; else rm -f "$DIR/$2/mods/$(basename "$j")"; fi
  done
}

pass=0; fail=0; skipped=""; SUM=$LOGS/summary.txt; : > "$SUM"
while IFS='|' read -r name file loaders needsExtra markers; do
  [ -n "$name" ] || continue
  if [ $# -gt 0 ] && ! printf '%s\n' "$@" | grep -qx "$name"; then continue; fi
  run=""
  for l in $ALL_LOADERS; do
    [ "$loaders" = forge ] && [ "$l" != forge ] && continue
    if [ "$needsExtra" = 1 ]; then
      { [ "$l" = forge ] && [ -n "${EXTRA_FORGE:-}" ]; } || { [ "$l" = fabric ] && [ -n "${EXTRA_FABRIC:-}" ]; } || continue
    fi
    run="$run $l"
  done
  run=${run# }
  if [ -z "$run" ]; then skipped="$skipped $name"; continue; fi
  echo "### $name ($run)"
  [ "$needsExtra" = 1 ] && for l in $run; do extra add $l; done
  LOADERS="$run" CUT=${CUT:-400} bash "$T/run.sh" "$DIR" "$T/$file" > "$LOGS/$name.out" 2>&1 < /dev/null
  [ "$needsExtra" = 1 ] && for l in $run; do extra remove $l; done
  for l in $run; do
    log=$LOGS/$name-$l.log
    cp "$DIR/$l/out.all.log" "$log"
    missing=""
    IFS='&' read -ra parts <<< "${markers// && /&}"
    for m in "${parts[@]}"; do
      n=1; re=$m
      if [[ "$m" =~ ^([0-9]+)\*(.*)$ ]]; then n=${BASH_REMATCH[1]}; re=${BASH_REMATCH[2]}; fi
      c=$(grep -acE -- "$re" "$log")
      [ "$c" -ge "$n" ] || missing="$missing [$m]"
    done
    bad=$(grep -aE -- "$FAILS" "$log" | grep -vE "api\.github\.com" | head -3)
    if [ -z "$missing" ] && [ -z "$bad" ]; then
      echo "PASS  $name ($l)" | tee -a "$SUM"; pass=$((pass + 1))
    else
      echo "FAIL  $name ($l)${missing:+ missing:$missing}${bad:+ errors: $(echo "$bad" | cut -c1-200 | tr '\n' ' ')}" | tee -a "$SUM"; fail=$((fail + 1))
    fi
  done
done <<< "$TESTS"
echo "===== summary"
cat "$SUM"
[ -n "$skipped" ] && echo "skipped (requirements not met):$skipped"
echo "passed: $pass, failed: $fail"
[ "$fail" = 0 ]
