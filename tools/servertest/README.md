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
| `integrations/jei-jade.txt` | JEI 15.62.0.217 + Jade 11.13.3 in `mods/` (and again without them): servers start, Jade loads `li.cil.oc.integration.jade.JadePlugin` server-side; leaves a running computer, capacitor, relay, charger, disk drive, screen, assembler and hologram next to spawn for a client check | `running=true ... lastError=null`, no errors |
| `integrations/ae2.txt` | AE2 15.4.11: ME network (controller, creative cell, crafting storage, drive with item + fluid cell, interface, cable bus with interface part + export bus) and adapters; runs `integrations/ae2.lua` | `lastError=... RESULT OK comps=database,me_controller,me_exportbus,me_interface ...` |

Integration tests need the other mod in both servers' `mods/`: for AE2, `appliedenergistics2-forge-15.4.11.jar`
(the **release** jar from Modrinth/CurseForge; the modmaven one has a dev refmap and crashes production Forge)
plus `guideme-20.1.x.jar` on Forge, and `appliedenergistics2-fabric-15.4.11.jar` on Fabric.
`integrations/ae2.txt` is generated from `ae2.lua` by `integrations/ae2.py`.

The tests use `/oc_debug`, which only exists when the server runs with
`-Dopencomputers.debugCommands=true` (the script sets it): `start|stop|status <pos>` (a rack:
its first server), `place <pos> <item>` (a fake player uses the item on the block below, e.g.
to place a robot, microcontroller or drone from item NBT), `drones start|stop|status`,
`use <pos> <player>`, `useitem <player> [release]`, `type <pos> <text>` (pastes the line plus
Enter into the machine's screen through its keyboard, e.g. into the OpenOS shell),
`screen <pos>` (prints the non-blank screen lines) and `tablet <player> start|stop|status|screen|type <text>`
(the tablet in the player's main hand).

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
| `navsign.txt` | robot creates map #0 with an empty map, then navigation upgrade (holding map #0: position, facing, range, waypoint "home"), sign upgrade (read / write the sign in front), tractor beam (picks up 3 diamonds) | `RESULT posNoMap=nil,invalid map makeMap=true,item_used pos=30.5,-59.5,60.5 facing=3.0 range=64.0 wp={1:{...label:home;position:{1:3.0;2:-1.0;3:2.0...}...}} signGet=hello\|world\|\| signSet=robot\|was here\|\| ... suck=true suck2=false count4=3.0 item4=minecraft:diamondx3.0`, sign block data `"robot", "was here"` |
| `chunkloader.txt` | robots with / without chunkloader upgrade far from spawn, no players, after `forceload remove all`; a chunk forced with `/forceload` in the loader's area; robot moves into the next chunk; restart; robot removed | `CL-R1-LOADED`, `CL-R1-MOVED`, `running=true`, `CL-R2-UNLOADED`, `CL-R3-LOADED`, `CL-R3-MOVED`, `CL-R1-RELEASED`, `CL-R2-DID-NOT-MOVE`, `CL-R3-RESTORED`, `CL-R3-RELEASED` and every `forceload query` saying chunk [125, 124] `is marked for force loading` |
| `shell.txt` (use `CUT=2000`) | OpenOS (Lua BIOS + OpenOS floppy) on a robot with screen/keyboard/GPU; `oc_debug type` runs `ls /`, `echo hi > /tmp/x`, `cat /tmp/x` and a script calling `require('robot').forward()` | `SHELL-ROBOT-MOVED`; the last `oc_debug screen` shows the `ls /` listing, `hi`, `fwd     true` and `f.lua  x` (`ls /tmp` after the move) |
| `disassembler.txt` | tier 1 CPU (9 ingredients) in a disassembler: one ingredient per 2000 energy at 25 energy per 10 ticks = 40 s each, like 1.12 / 1.16.5 (a whole CPU takes ~6 min) | chest holds 1 / 2 / 3 iron nuggets at ~45 / 85 / 125 s (the disassembler's `oc:buffer` at 225.0 / 100.0) |
| `hoverboots.txt` | hover boots charging in a charger (100 energy per 10 ticks) | `"oc:charge": 400.0d` after 2 s, `2400.0d` after 12 s |

`client.sh <servers-dir> <loader> <port> <display>` is the client check: it builds
`gameplay/client-scene.txt` on the server, joins with a dev client under xvfb, opens the GUIs of
the case, robot, drone, disk drive, RAID, rack, assembler, disassembler, printer, charger, relay,
adapter, manual, database upgrade, server and tablet (screenshots in `<servers-dir>/shots`) and
types into a screen through a keyboard (`RESULT typed=hello keyboards=1`).

`client-player.sh <servers-dir> <loader> <port> <display>` (same requirements; the client joins
as `OCTester`) checks what needs a real player, in survival mode (see the script header):

| Check | Expected (Forge and Fabric) |
|---|---|
| hover boots: walk onto a 1 block platform, jump onto a 2 block one, fall 13 blocks; without, then with charged boots | `STEP-NOBOOTS` / `JUMP-NOBOOTS` at y `-60.0d` (blocked), `FALL-NOBOOTS` health `10.0f`; `STEP-BOOTS` y `-59.0d`, `JUMP-BOOTS` y `-58.0d`, `FALL-BOOTS` health `19.0f`; boots `oc:charge` below 15000 |
| nanomachines: eat (hold right click), `nano.lua` over a wireless card | `oc:hasNanomachines: 1b`; `RESULT nano first port=port,7.0 power=power,19974.8,100000.0 name=name,OCTester inputs=totalInputCount,17.0 safe=...,2.0 max=...,4.0 set1=input,1.0,true ... health=health,H,20.0 set3=input,3.0,true overloaded=health,H-5 or less,...`; HUD bar left of the hotbar in `shots/<loader>-player-nano-hud.png` |
| tablet with Lua BIOS + OpenOS floppy, right click, `oc_debug tablet ... type` | tablet GUI with OpenOS in `shots/<loader>-player-tablet.png`, screen shows the `ls /` listing and `hi` |
| damage types | `DAMAGE-TYPES` health `13.0f` (7 damage through a diamond chestplate: armor bypassed) |
| restart server + client: `<uuid>.ocnm` in `playerdata/`, `nano.lua` again, overload until death | `oc:hasNanomachines: 1b`, `RESULT nano again ... inputs=totalInputCount,17.0 ... in1=input,1.0,true in2=input,2.0,true in3=input,3.0,false ...` (same configuration), a nanomachines overload death message (e.g. `The nanomachines of OCTester went out of control.`) |

## Mod integrations (`integrations/`)

Run these with the other mod's jars added to both servers' `mods/` (and make sure `boot.txt`
still passes without them).

| Commands file | Mod jars | What it checks | Expected |
|---|---|---|---|
| `integrations/computercraft.txt` (generated by `computercraft.py`) | `cc-tweaked-1.20.1-forge-1.120.2.jar` / `cc-tweaked-1.20.1-fabric-1.120.2.jar` (maven.squiddev.cc) | OC adapter → CC monitor + disk drive (incl. a main-thread method and the drive mounting its disk into OC); CC floppy in an OC disk drive (write/read); a CC computer booting from that floppy sees an OC relay as `modem`, lists OC components via `getNamesRemote`, exchanges messages with the OC computer | `RESULT mon=7.0x5.0,scale=1.0 drive=true,label=occc,id=7.0,mount=disk floppy=ro:false,total=125000.0,... ccDriveMounts=1 ccmsg=hi:right:ap=false:remote=computer,...,from=cc1_right,reply=98.0 cclog=names=back,right;ack=98/98/pong;` and `LAMP-LIT` |

### JEI / Jade client check

JEI 15.62 on Fabric needs Fabric Loader >= 0.19.4 (`fabric-installer server -loader 0.19.5` for the
server; dev runs with `-Pintegrations=true` switch to `fabric_loader_integrations_version`). Keep the
servers running after `integrations/jei-jade.txt`, then join with a dev client, e.g.
`xvfb-run -a -n <display> -s "-screen 0 1280x720x24" ./gradlew :fabric:runClient -Pintegrations=true
--args="--quickPlayMultiplayer localhost:<port>"`, `tp` the player next to the computer at `2 -60 2`
and check the Jade overlay (address / stored energy / component name), then `e` and the JEI list
(`@opencomputers`, `r` / `u` on an item: crafting recipes, "OpenComputers Manual", "OpenComputers API").
Known dev-environment issues: Jade 11.13.3 Forge's `StringRenderOutputMixin` fails in the Loom Forge
dev environment (`@Shadow this$0`), so the Forge dev client crashes unless that mixin is removed from
the Loom-remapped Jade jar; production is fine. With all integrations enabled, the AE2 / TIS-3D
(Fabric) and CB Multipart (Forge) dev runtimes miss dependencies.
| `integrations/tis3d.txt` (generated by `tis3d_forge_mods.py`) | `tis3d-MC1.20.1-{forge,fabric}-1.7.7.jar` + `markdown_manual-MC1.20.1-{forge,fabric}-1.2.5` (+ `ForgeConfigAPIPort-v8.0.3-1.20.1-Fabric` on Fabric) (Modrinth) | TIS-3D casing (lever-powered controller) with two serial port modules facing two OC adapters; the computer writes to one adapter's `serial_port`, TIS-3D moves the value to the other module/adapter and back | `RESULT ports=2 write=true a->b=42.0 b->a=-7.0` |
| `integrations/mekanism.txt` (Forge only) | `Mekanism-1.20.1-10.4.16.80.jar` (modmaven.dev) | adapter next to a basic chemical tank holding 1234 mB hydrogen: `getChemicalTanks` | `RESULT name=block_mekanism_basic_chemical_tank tank1=gas,mekanism:hydrogen,Hydrogen,1234.0/64000.0 tank2=infuse_type,...` |
| `integrations/enderstorage.txt` (Forge only) | `EnderStorage-1.20.1-2.11.0.188-universal` + `CodeChickenLib-1.20.1-4.4.0.528-universal` (maven.covers1624.net) | adapter next to an ender chest: get/set frequency (3 numbers and packed form), colors, owner, invalid frequency rejected; `data get` shows the chest's new frequency | `RESULT freq0=0.0,0.0,0.0 freq1=1.0,2.0,3.0 colors=orange,magenta,light_blue freq2=4.0,10.0,15.0 owner=nil c14=red bad=false`, `{middle: 10, left: 4, right: 15}` |
| `integrations/projectred.txt` (Forge only) | `ProjectRed-1.20.1-4.21.0-{core,transmission}` + `CBMultipart-1.20.1-3.3.0.159-universal` + CodeChickenLib (maven.covers1624.net) | two OC redstone I/O blocks joined by a ProjectRed bundled cable: bundled output of one is read as bundled input by the other (both directions); `data get` shows the cable's signal | `RESULT n=2 before=0.0 on=200.0 off=0.0 back=255.0`, cable `signal: [B; ..., -1B, 0B]` |

The Forge-only command files can be run together (they use separate coordinates); on Fabric they
only print unknown-block errors.
