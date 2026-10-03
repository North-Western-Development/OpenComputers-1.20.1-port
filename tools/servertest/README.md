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
| `integrations/ae2.txt` | AE2 15.4.11: ME network (controller, creative cell, crafting storage, drive with item + fluid cell, interface, cable bus with interface part + export bus) and adapters; runs `integrations/ae2.lua` | `lastError=... RESULT OK comps=database,me_controller,me_exportbus,me_interface ...` |

Integration tests need the other mod in both servers' `mods/`: for AE2, `appliedenergistics2-forge-15.4.11.jar`
(the **release** jar from Modrinth/CurseForge; the modmaven one has a dev refmap and crashes production Forge)
plus `guideme-20.1.x.jar` on Forge, and `appliedenergistics2-fabric-15.4.11.jar` on Fabric.
`integrations/ae2.txt` is generated from `ae2.lua` by `integrations/ae2.py`.

The tests use `/oc_debug`, which only exists when the server runs with
`-Dopencomputers.debugCommands=true` (the script sets it).
