# Integration brief (third-party mod integrations for 1.20.1)

You are porting one or more of OpenComputers' third-party mod integrations from the parked
1.16.5 Scala code in `legacy/integration-scala/<mod>/` to Java for 1.20.1, against the current
versions of those mods (or their modern replacements). Read `PORTING.md` first (conventions,
platform hooks, "Dedicated server safety" rules), then `legacy/integration-scala/README.md`.

## Where code goes

- Mods available on both loaders with a loader-independent API (AE2, CC:Tweaked, JEI, Jade,
  TIS-3D): `common/src/main/java/li/cil/oc/integration/<mod>/`. Common compiles against the
  Fabric variant (AE2, Jade, TIS-3D) or the common API jar (CC:Tweaked, JEI). If something truly
  differs per loader, use an `@ExpectPlatform` hook (see PORTING.md) with Impls in forge/ and fabric/.
- Forge-only mods (Mekanism, EnderStorage, ProjectRed): `forge/src/main/java/li/cil/oc/integration/<mod>/`,
  registered from `li.cil.oc.forge.OpenComputersForge` via
  `Mods.registerOptional(Mods.IDs.X, "li.cil.oc.integration.<mod>.ModX")` **before** `OpenComputers.init()`.
- Entry point per mod: a `ModProxy` class `li.cil.oc.integration.<mod>.Mod<Name>` with
  `public static final Mod<Name> INSTANCE`. `common/.../integration/Mods.java` already lists the
  common ones in `OPTIONAL` and loads them reflectively only when the mod is present, so the proxy
  class may reference the other mod's classes, but **nothing outside your integration package may
  reference the other mod's classes** (they are absent at runtime when the mod isn't installed).
  Register drivers/converters in `initialize()` via `li.cil.oc.api.Driver.add(...)` like
  `integration/minecraft/ModMinecraft.java` does.
- Client-only integrations (JEI, Jade) use their mods' own plugin discovery (Forge annotations /
  Fabric entrypoints in `fabric/src/main/resources/fabric.mod.json`); keep client classes in a
  client-only package and never let a dedicated server load them.
- Dependencies are already declared (gradle.properties versions; `common/build.gradle`,
  `forge/build.gradle`, `fabric/build.gradle`). Build with `-Pintegrations=true` to put them into
  dev runs. If you need another artifact (e.g. a mod's transitive API), add it the same way.
- If the 1.20.1 version of a mod lacks a feature the old integration used, port what makes sense,
  and note dropped parts in your report and with `// TODO(port): ...`.

## Building and testing

- Your worktree is separate; the first build sets up Minecraft (a few minutes). Keep memory low:
  `./gradlew --no-daemon -Dorg.gradle.jvmargs=-Xmx2G build -x javadoc` (other agents build in
  parallel on a 4-core / 15 GB machine). Run servers with `-Xmx1500M`.
- Real dedicated servers are installed at
  `/tmp/claude-0/-home-user-OpenComputers-1-20-1-port/d1a80ebc-5249-5e03-92eb-fab5e04037ad/scratchpad/srv-forge`
  and `.../srv-fabric` (Forge 47.4.26 / Fabric Loader 0.16.14 + Fabric API 0.92.12 + Architectury).
  **Do not use those directories directly** (other agents use them). Copy them to your own
  directory under the scratchpad (`cp -r`, skip `world/`, `logs/`, `crash-reports/`), set a unique
  `server-port` in `server.properties`, put your freshly built OC jar plus the integrated mod's
  jars (download from the mavens in `build.gradle`; mind their own dependencies) into `mods/`.
- `tools/servertest/run.sh <dir-with-forge-and-fabric-subdirs> <commands-file>` runs a command
  file on both servers (see `tools/servertest/README.md`; `WAIT` lines pause). `/oc_debug
  start|stop|status <pos>` starts a computer and shows `lastError`. The pattern used by the
  existing tests: a creative case with CPU/RAM/(cards) and an EEPROM whose Lua code ends in
  `error("RESULT " .. something)`, so the result shows up in `oc_debug status`. Build EEPROM
  item NBT with a small Python script (see how `tools/servertest/boot.txt` encodes Lua as a byte
  array `{"oc:data":{"oc:eeprom":[B;...]}}`). Use `forceload add` so chunks stay loaded.
  Adapters (`opencomputers:adapter`) expose adjacent blocks as components.
- Test every integration you port on every loader it supports, with the other mod installed, and
  also make sure both servers still start cleanly **without** the other mod.
- Kill only processes you started (match your own directory in `/proc/<pid>/cmdline`); never use
  `pkill -f` with a pattern that matches your own shell command line.

## Finishing

Commit your work in your worktree with a clear message (end it with the two attribution lines
used in this repo's recent commits). Add your test command files under
`tools/servertest/integrations/` and update `tools/servertest/README.md`. Final report (< 40
lines): what's ported, what's tested on which loader with which mod version, what was dropped and
why, and the branch/commit to merge.
