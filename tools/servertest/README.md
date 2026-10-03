# Dedicated server smoke tests

`run.sh <servers-dir> <commands-file>` boots a real Forge and a real Fabric 1.20.1 server with
the built OpenComputers jars, sends console commands and prints the results. See the header of
`run.sh` for the expected server layout.

| Commands file | What it checks | Expected |
|---|---|---|
| `blocks.txt` | places every OC block; block entities tick and save | no exceptions |
| `boot.txt` | creative case + CPU/RAM/redstone card + EEPROM with Lua code | `lastError=... OC-TEST-OK Lua 5.3 rs=<address>`, `LAMP-LIT` |
| `live.txt` | timing (20 x `pullSignal(0.1)`), GPU + screen | `LIVE ~2.3s gpu=true text=he` |
| `openos.txt` | Lua BIOS + OpenOS loot floppy, GPU + screen | `running=true ... lastError=null` |

Integration tests need the other mod in both servers' `mods/`: for AE2, `appliedenergistics2-forge-15.4.11.jar`
(the **release** jar from Modrinth/CurseForge; the modmaven one has a dev refmap and crashes production Forge)
plus `guideme-20.1.x.jar` on Forge, and `appliedenergistics2-fabric-15.4.11.jar` on Fabric.
`integrations/ae2.txt` / `ae2_crafting.txt` are generated from `ae2.lua` / `ae2_crafting.lua` by `integrations/ae2.py` / `ae2_crafting.py` (they use separate coordinates, so they can be concatenated into one run).

The tests use `/oc_debug`, which only exists when the server runs with
`-Dopencomputers.debugCommands=true` (the script sets it): `start|stop|status <pos>` (a rack:
its first server), `place <pos> <item>` (a fake player uses the item on the block below, e.g.
to place a robot, microcontroller or drone from item NBT), `drones start|stop|status`,
`use <pos> <player>`, `useitem <player>`.

In command files, `WAIT <n>` pauses n seconds and `RESTART` stops both servers (waiting until
they exited; a server that does not exit gets a thread dump in the log) and starts them again on
the same world. `LOADERS=forge` (or `fabric`) runs only one server; `CUT` / `HEAD` widen the
printed log excerpt.

## Gameplay (`gameplay/`)

`gameplay/gen.py` generates each `<name>.txt` from `<name>.cmds`, replacing `@EEPROM(x.lua)` by
an EEPROM item holding that Lua file (`@BIOS`: the Lua BIOS). Each EEPROM ends in
`error("RESULT ...")`, shown by `oc_debug status`. All pass on Forge and Fabric:

| Commands file | What it checks | Expected |
|---|---|---|
| `persistence.txt` | HDD counter program (`persist.lua`), OpenOS, a robot running `persist.lua` and a hovering drone keep running across `RESTART` (Lua state incl. a coroutine, HDD/tmpfs contents, EEPROM data, HDD label) | `RESULT n=80 co=80 hdd=80 tmp=80 starts=1 eeprom=true label=ptest` (computer and robot), OpenOS `running=true lastError=null`, `RESULT drone n=80 ...` |
| `robot.txt` | robot placed from item NBT, pickaxe in tool slot: detect/swing/move/turn/place/drop/suck/count/select | `RESULT inv=16.0 det=true,solid ... swing=true,block cnt=1.0 ... fwd=true back=true ... place=true ... drop=true ... suck=1.0 cnt5=1.0`, `ROBOT-MINED-SOUTH`, `ROBOT-PLACED-WEST`, `ROBOT-HOME` |
| `upgrades.txt` | robot with inventory controller, crafting, generator, piston, tank and experience upgrades | `RESULT icSize=27.0 icStack=minecraft:oak_logx4.0 ... craft=true,4.0 planks=minecraft:oak_planksx4.0 ... genCount=1.0,Coal ... push=true tanks=1.0 ...`, `PISTON-PUSHED` |
| `drone.txt` | drone placed from item NBT flies up 3 / east 2 | status `pos=22.50,-56.50,20.50 running=true`, `RESULT ... off1=0.0 off2=0.1 ...`, `DRONE-LANDED-EAST` |
| `network.txt` | network cards through cable + relay, wireless cards, linked cards; replies | `RESULT A ... lan:1.0:0:re:wired tun:0.0:0:re:linked wlan:2.0:0:re:air ... wlan:2.0:4:re:air` |
| `filesystem.txt` | HDD, floppy in a disk drive, RAID (3 HDDs), tmpfs: mkdir/write/append/seek/read/list/rename/remove/label; EEPROM data/label | `RESULT drive=true` + `[mk=true,rd=world!,...]` for raid/floppy/tmp/hdd, `fsCount=4 eeprom=data!,FS test,...` |
| `components.txt` | geolyzer, transposer, redstone I/O, hologram, data card (tier 3), internet card (HTTPS to example.com), motion sensor, 3D printer | eight `RESULT` lines without `ERR`/`nil,` values, chest with 4 cobblestone, `LAMP-LIT`, printer with 2 prints in slot 2 |
| `machines.txt` | assembler builds a robot, disassembler, charger charging a robot, rack + server blade, microcontroller placed from item NBT lighting a lamp, capacitor | `done=idle,false` and `opencomputers:robot` in the assembler, `charged=true`, `RESULT server comps=computer,eeprom,filesystem,modem`, `MC-LAMP-LIT`, `RESULT mc comps=...redstone`, iron nuggets in the disassembler's chest |

`client.sh <servers-dir> <loader> <port> <display>` is the client check: it builds
`gameplay/client-scene.txt` on the server, joins with a dev client under xvfb, opens the GUIs of
the case, robot, drone, disk drive, RAID, rack, assembler, disassembler, printer, charger, relay,
adapter, manual, database upgrade, server and tablet (screenshots in `<servers-dir>/shots`) and
types into a screen through a keyboard (`RESULT typed=hello keyboards=1`).

## Advancements

`advancements.sh <servers-dir> <loader> <port> <display>` (like `client.sh`, one loader per run) checks
the advancements generated by `tools/advancements.py` (`data/opencomputers/advancements`, the 1.12
achievement tree): it joins with a dev client, grants/revokes `from opencomputers:root`
(`Granted 37 advancements`), gives one item per advancement (`minecraft:inventory_changed`, incl. the
OpenOS floppy NBT) and drops a robot, drone, tablet and microcontroller at the player (picked up ->
`opencomputers:assembled`). Expected: `made after the grant/revoke checks: 36` plus "Couldn't grant advancement [OpenComputers] ... already have it" (the root, which is silent), no advancement errors.

## Mod integrations (`integrations/`)

Run these with the other mod's jars added to both servers' `mods/` (and make sure `boot.txt`
still passes without them).

| Commands file | Mod jars | What it checks | Expected |
|---|---|---|---|
| `integrations/computercraft.txt` (generated by `computercraft.py`) | `cc-tweaked-1.20.1-forge-1.120.2.jar` / `cc-tweaked-1.20.1-fabric-1.120.2.jar` (maven.squiddev.cc) | OC adapter → CC monitor + disk drive (incl. a main-thread method and the drive mounting its disk into OC); CC floppy in an OC disk drive (write/read); a CC computer booting from that floppy sees an OC relay as `modem`, lists OC components via `getNamesRemote`, exchanges messages with the OC computer | `RESULT mon=7.0x5.0,scale=1.0 drive=true,label=occc,id=7.0,mount=disk floppy=ro:false,total=125000.0,... ccDriveMounts=1 ccmsg=hi:right:ap=false:remote=computer,...,from=cc1_right,reply=98.0 cclog=names=back,right;ack=98/98/pong;` and `LAMP-LIT` |
| `integrations/jei-jade.txt` | JEI 15.62.0.217 + Jade 11.13.3 in `mods/` (and again without them): servers start, Jade loads `li.cil.oc.integration.jade.JadePlugin` server-side; leaves a running computer, capacitor, relay, charger, disk drive, screen, assembler and hologram next to spawn for a client check | `running=true ... lastError=null`, no errors |
| `integrations/ae2.txt` | AE2 15.4.11: ME network (controller, creative cell, crafting storage, drive with item + fluid cell, interface, cable bus with interface part + export bus) and adapters; runs `integrations/ae2.lua` | `lastError=... RESULT OK comps=database,me_controller,me_exportbus,me_interface ...` |
| `integrations/ae2_crafting.txt` | AE2 15.4.11: crafting CPU, pattern provider with an encoded crafting pattern (oak log -> 4 planks, item NBT) next to a molecular assembler, 64 logs in a cell, cable bus with an import bus under a chest (dirt + stone); runs `integrations/ae2_crafting.lua`: `getCraftables`, `request(8)` until `isDone()`, a request with missing resources, import bus get/set configuration (only dirt is imported) | `RESULT OK craftables=1 planksCraftable=true ... cpuWhileCrafting=true:minecraft:oak_planks done=true,nil ... after logs=62.0 planks=8.0 ... missing=false,request failed (missing resources: 188xminecraft:oak_log) cfg0=minecraft:clay_ball ... cfg=minecraft:dirt ... dirt=1.0->5.0 stone=0 ...` |
| `integrations/tis3d.txt` (generated by `tis3d_forge_mods.py`) | `tis3d-MC1.20.1-{forge,fabric}-1.7.7.jar` + `markdown_manual-MC1.20.1-{forge,fabric}-1.2.5` (+ `ForgeConfigAPIPort-v8.0.3-1.20.1-Fabric` on Fabric) (Modrinth) | TIS-3D casing (lever-powered controller) with two serial port modules facing two OC adapters; the computer writes to one adapter's `serial_port`, TIS-3D moves the value to the other module/adapter and back | `RESULT ports=2 write=true a->b=42.0 b->a=-7.0` |
| `integrations/mekanism.txt` (Forge only) | `Mekanism-1.20.1-10.4.16.80.jar` (modmaven.dev) | adapter next to a basic chemical tank holding 1234 mB hydrogen: `getChemicalTanks` | `RESULT name=block_mekanism_basic_chemical_tank tank1=gas,mekanism:hydrogen,Hydrogen,1234.0/64000.0 tank2=infuse_type,...` |
| `integrations/enderstorage.txt` (Forge only) | `EnderStorage-1.20.1-2.11.0.188-universal` + `CodeChickenLib-1.20.1-4.4.0.528-universal` (maven.covers1624.net) | adapter next to an ender chest: get/set frequency (3 numbers and packed form), colors, owner, invalid frequency rejected; `data get` shows the chest's new frequency | `RESULT freq0=0.0,0.0,0.0 freq1=1.0,2.0,3.0 colors=orange,magenta,light_blue freq2=4.0,10.0,15.0 owner=nil c14=red bad=false`, `{middle: 10, left: 4, right: 15}` |
| `integrations/projectred.txt` (Forge only) | `ProjectRed-1.20.1-4.21.0-{core,transmission}` + `CBMultipart-1.20.1-3.3.0.159-universal` + CodeChickenLib (maven.covers1624.net) | two OC redstone I/O blocks joined by a ProjectRed bundled cable: bundled output of one is read as bundled input by the other (both directions); `data get` shows the cable's signal | `RESULT n=2 before=0.0 on=200.0 off=0.0 back=255.0`, cable `signal: [B; ..., -1B, 0B]` |

### JEI / Jade client check

JEI 15.62 on Fabric needs Fabric Loader >= 0.19.4 (`fabric-installer server -loader 0.19.5` for the
server; the dev environment uses 0.19.5). Keep the
servers running after `integrations/jei-jade.txt`, then join with a dev client, e.g.
`xvfb-run -a -n <display> -s "-screen 0 1280x720x24" ./gradlew :fabric:runClient -Pintegrations=true
--args="--quickPlayMultiplayer localhost:<port>"`, `tp` the player next to the computer at `2 -60 2`
and check the Jade overlay (address / stored energy / component name), then `e` and the JEI list
(`@opencomputers`, `r` / `u` on an item: crafting recipes, "OpenComputers Manual", "OpenComputers API").
`-Pintegrations=true` dev runs (server and client, both loaders) start with all integrated mods. Two
of those mods need dev-only patched copies, built by `devPatchedJar` in `forge/build.gradle`: Jade
(its `StringRenderOutputMixin` shadows a synthetic field Mojang's mappings don't name) and
CodeChickenLib (an override of a vanilla private method that Loom can't remap); production is unaffected.

The Forge-only command files can be run together (they use separate coordinates); on Fabric they
only print unknown-block errors.
