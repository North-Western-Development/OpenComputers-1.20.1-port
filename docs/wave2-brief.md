# Wave 2 brief (shared by all conversion agents)

You are one of ~11 agents converting OpenComputers from 1.16.5 Scala/Forge to 1.20.1 Java/Architectury,
working CONCURRENTLY in the same working tree (/home/user/OpenComputers-1.20.1-port).

1. Read PORTING.md fully first. It contains mandatory conventions AND the contracts of already
   converted code (util, Settings, API, resources). Converted Java lives in common/src/main/java —
   read it when you call into it (e.g. util/ExtendedNBT.java, util/InventoryUtils.java,
   api/**, common/platform/PlatformHooks.java, common/transfer/*).
2. For code not yet converted (owned by other agents), call it according to PORTING.md rules
   applied to its Scala declaration in src/main/scala (or `git show HEAD~N:path` if deleted already).
   Don't edit other agents' files. If you truly need a change in someone else's area, write it down
   in your final report instead.
3. Shared registration/lifecycle contracts for wave 2:
   - Registries use Architectury `DeferredRegister.create(OpenComputers.ID, Registries.X)` with static
     `RegistrySupplier<T>` fields and a static `init()`/`register()` that calls `REGISTRY.register()`:
     `common.init.Blocks` (blocks + their BlockItems), `common.init.Items`,
     `common.tileentity.TileEntityTypes`, `common.container.ContainerTypes`, `common.entity.EntityTypes`,
     `common.recipe.RecipeSerializers`, `common.Sound` (sound events). `common.Proxy.preInit()` calls
     each of these `init()`s except Blocks/Items (called by OpenComputers.init()).
   - Block entities: base class `li.cil.oc.common.tileentity.traits.TileEntity` (abstract, extends
     BlockEntity). It provides `public void updateEntity()` which is called every tick on both sides
     for block entities implementing `li.cil.oc.common.tileentity.traits.Tickable`. Blocks (common.block.*)
     implement `EntityBlock`; their `getTicker` returns `(level, pos, state, be) -> ((traits.TileEntity) be).updateEntity()`
     when the BE is Tickable. Block entities are created via `TileEntityTypes.X.get().create(pos, state)`;
     tile constructors are `(BlockEntityType<?> type, BlockPos pos, BlockState state[, extra args])`.
     Lifecycle hooks `initialize()` / `dispose()` are called by the tile base class (first tick /
     setRemoved + onChunkUnloaded); Fabric has no onChunkUnloaded on BEs, so the base class also
     registers with an Architectury chunk-unload hook — the tileentity agent owns that.
   - Menus: `MenuRegistry.ofExtended(...)` types in ContainerTypes; open with `MenuRegistry.openExtendedMenu`.
   - Networking: packets keep OC's custom byte format (PacketBuilder/PacketType); transport is
     Architectury `NetworkManager` with one channel id `opencomputers:main` per direction.
   - Events: Architectury events, OC API `EventBus.INSTANCE`, or new @ExpectPlatform hooks in your own
     hook class (see PORTING.md "Adding a platform hook"); implement BOTH forge and fabric Impl classes.
   - Mixins allowed (li.cil.oc.common.mixin / li.cil.oc.client.mixin); add each new mixin class name to
     common/src/main/resources/opencomputers-common.mixins.json — that file is shared, so edit it with a
     minimal, careful insertion (re-read right before editing).
4. Third-party mod integrations are out of scope (parked in legacy/). Drop references to them
   (AE2, CC, Mekanism, ProjectRed, TIS-3D, EnderStorage, JEI, WAILA, Immibis microblocks) with
   `// TODO(port): integration` where behaviour is lost.
5. Reference sources (raw GitHub is reachable via curl): Fabric API
   https://raw.githubusercontent.com/FabricMC/fabric/1.20.1/<module>/src/main/java/...,
   Architectury API https://raw.githubusercontent.com/architectury/architectury-api/1.20/common/src/main/java/dev/architectury/...,
   Forge https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.1/src/main/java/...
   Minecraft itself is not available — use your knowledge of 1.20.1 Mojang mappings carefully.
6. Per file: write the Java under common/src/main/java/<same package path> (or forge/ fabric/ for
   platform Impl classes), then `git rm -q` the Scala file (retry on index.lock). Java files already
   sitting in src/main/scala are ported and moved with `git mv` then edited. Do NOT commit, do NOT run
   other git commands that change state. Mark lost behaviour `// TODO(port): ...`.
7. Final report (< 60 lines): public shape decisions others must know (esp. traits → interface/abstract
   class decisions, renamed members, new hook classes), requests for other areas, TODOs.
