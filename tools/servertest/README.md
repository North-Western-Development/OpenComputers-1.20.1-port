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

The tests use `/oc_debug`, which only exists when the server runs with
`-Dopencomputers.debugCommands=true` (the script sets it).

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
