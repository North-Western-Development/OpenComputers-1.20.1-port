# Porting guide: OpenComputers 1.16.5 (Scala/Forge) → 1.20.1 (Java/Architectury)

This document is the single source of truth for the conversion. Everyone working
on the port (humans and agents) follows it so that files converted independently
still fit together.

## Targets

| Thing | Version |
|---|---|
| Minecraft | 1.20.1 (official Mojang mappings) |
| Java | 17 |
| Architectury API | 9.2.x (`dev.architectury.*`) |
| Forge | 47.x (also loads on NeoForge 47.1 for 1.20.1) |
| Fabric Loader / API | 0.15.x / 0.92.x+1.20.1 |

## Project layout

```
common/   loader-independent code. Vanilla MC + Architectury API only.
          src/main/java/li/cil/oc/...            all mod code
          src/main/resources/assets|data/...     all assets & data
forge/    li.cil.oc.forge.*                      Forge entrypoint + platform impls
fabric/   li.cil.oc.fabric.*                     Fabric entrypoints + platform impls
src/main/scala/...                               UNCONVERTED legacy sources (the queue)
legacy/                                          third-party mod integrations parked for later
```

Conversion workflow per file: read `src/main/scala/li/cil/oc/<pkg>/X.scala`,
write `common/src/main/java/li/cil/oc/<pkg>/X.java`, then `git rm` the Scala
file. Java files that already live under `src/main/scala` (e.g.
`TileEntityTypes.java`) are ported and `git mv`'d the same way. **Package names
and simple class names never change**, so references across the codebase stay
valid while people work in parallel.

**Never import `net.minecraftforge.*`, `net.fabricmc.fabric.*` or Fabric Loader
APIs in `common`.** If something needs loader-specific code, use a platform hook
(see below).

## Scala → Java rules

These rules are mechanical on purpose. When you call into code converted by
someone else, apply the same rules to *their* Scala declaration (check it in git
history if the Scala file is already deleted: `git log --all -- path/X.scala`,
or look at the converted Java if it exists) to know how to call it.

1. **`object X`** (no parents) → `public final class X` with only `static`
   members and a private constructor. Call sites: `X.foo()`.
2. **`object X extends SomeClass/SomeTrait`** (e.g. drivers, providers passed
   around as values) → `public final class X extends/implements ...` with a
   `public static final X INSTANCE = new X();` and instance members. Call sites
   pass `X.INSTANCE`.
3. **Companion objects** → `static` members on the class of the same name. If
   a static member's name clashes with an instance member, suffix the static
   one with `Static`.
4. **`trait T`** → `public interface T` with the same name. Implementations
   go in `default` methods. Stateful traits: the interface declares abstract
   accessors and the implementing class holds the state (or the trait becomes
   an `abstract class` if *every* implementer uses it as its primary
   superclass – your call, but keep the type name). Stackable `abstract
   override` / `super.foo()` chains must be made explicit: implementing classes
   call each trait's hook in Scala linearization order.
5. **Members**
   - `def foo: T` / `def foo(): T` → method `foo()`.
   - `val/var x` in a `class`/`object` → field `x` (same visibility; `val` →
     `final` where possible). Call sites use `obj.x` / `obj.x = v`.
   - `val/var x` declared in a `trait` → interface methods `x()` and (for
     `var`) `setX(v)`.
   - `lazy val x` → method `x()` with lazy init.
   - Scala setter syntax `x_=` → `setX(v)`.
   - Keep Scala-style member names (`tier`, `node`, `host`, ...); don't
     rename to `getX` unless it implements a vanilla/API method.
6. **Types**
   - `Option[T]` → `java.util.Optional<T>` **everywhere** in signatures and fields.
   - `Unit` → `void`; `Any`/`AnyRef` → `Object`.
   - `Array[T]` → `T[]`. `Seq/List/Iterable/Vector/Buffer` → `java.util.List<T>`
     (`Iterable<T>` where Scala used Iterable). `Map` → `java.util.Map`, `Set` →
     `java.util.Set`. Use mutable `ArrayList`/`HashMap` internally.
   - Tuples → `org.apache.commons.lang3.tuple.Pair` / `Triple` (on the MC classpath),
     or a small `record` when it's local to one file.
   - `() => T` → `Supplier<T>`, `() => Unit` → `Runnable`, `A => B` →
     `Function<A,B>`, `A => Unit` → `Consumer<A>`, `A => Boolean` → `Predicate<A>`,
     `(A, B) => C` → `BiFunction`, etc.
   - Scala `Enumeration` (`util.ScalaEnum`) → Java `enum`.
   - `StackOption` (util) → `ItemStack` with `ItemStack.EMPTY` as "none".
7. **Implicit conversions / extension classes** (`util.ExtendedNBT`,
   `ExtendedWorld`, `ExtendedArguments`, `ExtendedInventory`, `ExtendedAABB`,
   `ExtendedBlock`, `ExtendedEnumFacing`, `ExtendedLuaState`, ...) become static
   helpers on the same-named class: `x.someExtension(a, b)` →
   `ExtendedFoo.someExtension(x, a, b)`. The implicit `toNbt(...)` conversions
   of `ExtendedNBT` become explicit `ExtendedNBT.toNbt(...)` overloads (or
   direct `IntTag.valueOf` etc.).
8. **Pattern matching** → `instanceof` patterns / `switch`. Java 17: pattern
   `instanceof` is fine; pattern `switch` is **not** available.
9. **Closures over `var`s** → use one-element arrays or `AtomicX`, or restructure.
10. Scala `@throws`, `@Deprecated` etc. map 1:1. Preserve comments and license headers.
11. `li.cil.oc.Settings.get.foo` → `Settings.get().foo` (Settings fields stay
    fields). `OpenComputers.log` → `OpenComputers.log()` is **not** used; it is a
    static field `OpenComputers.log`.

## Minecraft 1.16.5 → 1.20.1 cheat sheet (Mojang names)

| 1.16.5 | 1.20.1 |
|---|---|
| `net.minecraft.tileentity.TileEntity` | `net.minecraft.world.level.block.entity.BlockEntity` (ctor `(type, pos, state)`) |
| `TileEntityType` | `BlockEntityType` |
| `ITickableTileEntity.tick()` | `EntityBlock#getTicker` returning a `BlockEntityTicker` |
| `TileEntity.read(state, nbt)` / `write(nbt)` | `load(CompoundTag)` / `saveAdditional(CompoundTag)` |
| `getUpdatePacket` / `onDataPacket` | `ClientboundBlockEntityDataPacket.create(this)` + `getUpdateTag()`; client receives via `load()` (no `onDataPacket` in vanilla/Fabric) |
| `World` / `IWorld` / `IWorldReader` / `IBlockReader` | `Level` / `LevelAccessor` / `LevelReader` / `BlockGetter` |
| `ServerWorld` / `ClientWorld` | `ServerLevel` / `ClientLevel` |
| `entity.world` | `entity.level()` |
| `PlayerEntity` / `ServerPlayerEntity` | `Player` / `ServerPlayer` |
| `CompoundNBT` / `ListNBT` / `INBT` / `StringNBT` / `IntNBT` ... | `CompoundTag` / `ListTag` / `Tag` / `StringTag` / `IntTag` ... |
| `Constants.NBT.TAG_COMPOUND` (Forge) | `Tag.TAG_COMPOUND` |
| `ITextComponent` / `StringTextComponent` / `TranslationTextComponent` | `Component` / `Component.literal` / `Component.translatable` |
| `TextFormatting` | `ChatFormatting` |
| `ActionResultType` / `ActionResult<T>` | `InteractionResult` / `InteractionResultHolder<T>` |
| `Hand` | `InteractionHand` |
| `ItemUseContext` / `BlockItemUseContext` | `UseOnContext` / `BlockPlaceContext` |
| `IInventory` / `ISidedInventory` | `Container` / `WorldlyContainer` |
| `Container` (GUI) / `ContainerType` | `AbstractContainerMenu` / `MenuType` |
| `INamedContainerProvider` | `MenuProvider` |
| `ContainerScreen` / `Screen.render(MatrixStack,...)` | `AbstractContainerScreen` / `render(GuiGraphics, int, int, float)` |
| `MatrixStack` / `IRenderTypeBuffer` / `IVertexBuilder` | `PoseStack` / `MultiBufferSource` / `VertexConsumer` |
| `TileEntityRenderer` | `BlockEntityRenderer` (ctor takes `BlockEntityRendererProvider.Context`) |
| `Vector3f.YP.rotationDegrees` / `Quaternion` / `Matrix4f` | `com.mojang.math.Axis.YP.rotationDegrees` / `org.joml.Quaternionf` / `org.joml.Matrix4f` |
| `FontRenderer` | `Font` |
| `AxisAlignedBB` / `Vector3d` / `MathHelper` | `AABB` / `Vec3` / `Mth` |
| `net.minecraft.util.ResourceLocation` | `net.minecraft.resources.ResourceLocation` |
| `ItemGroup` | `CreativeModeTab` (registered via Architectury `CreativeTabRegistry`; items use `new Item.Properties().arch$tab(...)`) |
| `Material` / `ToolType` / `harvestTool` | gone: `BlockBehaviour.Properties.of().mapColor(...).strength(...)`, mining via `data/minecraft/tags/blocks/mineable/*.json` |
| `Registry.ITEM` / `ForgeRegistries.ITEMS` | `BuiltInRegistries.ITEM` |
| `DamageSource.GENERIC` | `level.damageSources().generic()` |
| `IRecipe` / `ICraftingRecipe` | `Recipe<Container>` / `CraftingRecipe` (`assemble(c, RegistryAccess)`, `getResultItem(RegistryAccess)`, `CraftingBookCategory`) |
| `new SoundEvent(rl)` | `SoundEvent.createVariableRangeEvent(rl)` |
| `ItemStack.areItemStackTagsEqual` / `isItemEqual` | `ItemStack.isSameItemSameTags` / `ItemStack.isSameItem` |
| `@OnlyIn(Dist.CLIENT)` | omit; keep client-only code in `li.cil.oc.client` and only reach it through client init (`EnvExecutor` / `Platform.getEnvironment()`) |

## Forge → Architectury / platform hooks

| Forge thing | Use instead |
|---|---|
| `DeferredRegister` / `RegistryEvent.Register` / `@ObjectHolder` | `dev.architectury.registry.registries.DeferredRegister` + `RegistrySupplier` |
| `MinecraftForge.EVENT_BUS` events | Architectury events (`dev.architectury.event.events.common.*`: `TickEvent`, `LifecycleEvent`, `BlockEvent`, `PlayerEvent`, `InteractionEvent`, `EntityEvent`, `ChunkEvent`, `LootEvent`; client: `ClientTickEvent`, `ClientGuiEvent`, `ClientLifecycleEvent`, `ClientPlayerEvent`, `ClientRawInputEvent`), else a platform hook or a common mixin |
| `ServerLifecycleHooks.getCurrentServer()` | `dev.architectury.utils.GameInstance.getServer()` |
| `FMLPaths.CONFIGDIR` / `ModList.isLoaded` | `dev.architectury.platform.Platform.getConfigFolder()` / `Platform.isModLoaded(id)` |
| `SimpleChannel` networking | `dev.architectury.networking.NetworkManager` (`registerReceiver(Side.S2C/C2S, id, (buf, ctx) -> ...)`, `sendToPlayer`, `sendToServer`) |
| `NetworkHooks.openGui` / `IForgeContainerType.create` | `dev.architectury.registry.menu.MenuRegistry.openExtendedMenu` / `MenuRegistry.ofExtended` |
| `ClientRegistry.bindTileEntityRenderer` / `RenderingRegistry` | `dev.architectury.registry.client.rendering.BlockEntityRendererRegistry` / `EntityRendererRegistry` |
| key bindings | `dev.architectury.registry.client.keymappings.KeyMappingRegistry` |
| item/block colours | `dev.architectury.registry.client.rendering.ColorHandlerRegistry` |
| `RenderTypeLookup.setRenderLayer` | `dev.architectury.registry.client.rendering.RenderTypeRegistry` |
| `TextureStitchEvent` | add sprites to `assets/minecraft/atlases/blocks.json` (directory/single sources) |
| Forge `FluidStack` | `dev.architectury.fluid.FluidStack` |
| `IItemHandler` / `CapabilityItemHandler` | `li.cil.oc.common.transfer.ItemHandler` + `PlatformHooks.getItemHandler(...)`; wrap vanilla containers with `ContainerItemHandler` |
| `IFluidHandler` / `IFluidHandlerItem` | `li.cil.oc.common.transfer.FluidHandler` / `ItemFluidHandler` + `PlatformHooks.getFluidHandler(...)` (amounts in mB) |
| `IEnergyStorage` / `CapabilityEnergy` | `li.cil.oc.common.transfer.EnergyHandler` + `PlatformHooks.getEnergyHandler(...)` |
| OC's own capabilities (`CapabilityEnvironment`, `CapabilitySidedEnvironment`, `CapabilityColored`, ...) | plain `instanceof` checks on the `BlockEntity` against the API interfaces (`Environment`, `SidedEnvironment`, `internal.Colored`, ...) |
| `FakePlayer` / `FakePlayerFactory` | OC's agent player extends `ServerPlayer` directly; `PlatformHooks.isFakePlayer` |
| `ForgeChunkManager` | vanilla `ServerLevel.setChunkForced` + OC-side bookkeeping |
| `ForgeHooks.getBurnTime` | `PlatformHooks.getBurnTime` |
| `BlockEvent.BreakEvent` firing | `PlatformHooks.canBreakBlock` |
| `IModelData` / `IDynamicBakedModel` / `ModelBakeEvent` | prefer blockstate properties + multipart JSON models or block entity renderers; truly custom models need a platform hook (Forge `BakedModel` + `ModelEvent`, Fabric `FabricBakedModel`/`ModelLoadingPlugin`) |
| Forge `Event` subclasses in the API | OC's own event bus, see `li.cil.oc.api.event` |
| `InterModComms` (IMC) | `li.cil.oc.api.IMC` is kept as a direct Java API (call methods instead of sending messages) |

### Adding a platform hook

Core hooks live in `li.cil.oc.common.platform.PlatformHooks` (item/fluid/energy
lookup, fake players, block protection, burn time). If you need another one,
**create a new class for your area** rather than editing a shared file (several
people work in parallel):

```java
// common/src/main/java/li/cil/oc/common/platform/RobotPlatform.java
public final class RobotPlatform {
    @ExpectPlatform
    public static boolean fooBar(Level level) { throw new AssertionError(); }
}
// forge/src/main/java/li/cil/oc/common/platform/forge/RobotPlatformImpl.java
public final class RobotPlatformImpl { public static boolean fooBar(Level level) { ... } }
// fabric/src/main/java/li/cil/oc/common/platform/fabric/RobotPlatformImpl.java
public final class RobotPlatformImpl { public static boolean fooBar(Level level) { ... } }
```

The implementation class lives in the same package as the stub plus `.forge` /
`.fabric`, named `<Stub>Impl`, with identical static signatures. Client-only
hooks go in `li.cil.oc.client.platform`.

Mixins: common mixins go in `li.cil.oc.common.mixin` (client ones in
`li.cil.oc.client.mixin`) and are listed in
`common/src/main/resources/opencomputers-common.mixins.json`.

## Entrypoints and lifecycle

- Forge: `li.cil.oc.forge.OpenComputersForge` (`@Mod("opencomputers")`).
- Fabric: `li.cil.oc.fabric.OpenComputersFabric` / `OpenComputersFabricClient`.
- Both call `li.cil.oc.OpenComputers.init()` during mod construction (registries
  are registered there via Architectury `DeferredRegister#register()`), and on
  the client additionally `li.cil.oc.OpenComputers.initClient()`.
- `common.Proxy` / `client.Proxy` keep their roles: `OpenComputers.proxy()`
  returns the right one; lifecycle methods are invoked from Architectury
  lifecycle events (`LifecycleEvent.SETUP`, `ClientLifecycleEvent.CLIENT_SETUP`,
  `LifecycleEvent.SERVER_STARTING`, ...).

## Scope notes

- Third-party integrations (AE2, ComputerCraft, Mekanism, ProjectRed, TIS-3D,
  EnderStorage, JEI, WAILA) are parked in `legacy/integration-scala` and are not
  part of the first pass. `integration.Mods` keeps only what is needed for the
  built-in `opencomputers` / `minecraft` integration.
- Keep behaviour identical where possible; mark anything you could not port
  faithfully with `// TODO(port): ...` so it can be found later.

## Resource migration notes (done; Java side must follow)

- Recipes use OC-owned item tags (`opencomputers:ingots/iron`, `opencomputers:dyes/<color>`,
  `opencomputers:beacon_base_blocks`, ...) in `data/opencomputers/tags/items/`, which include
  the matching `#forge:` and `#c:` tags. Java code must reference these instead of `forge:` tags
  (e.g. `ExtendedRecipe.beaconBlocks` → `opencomputers:beacon_base_blocks`).
- `opencomputers:crafting_shaped_extended` / `crafting_shapeless_extended` serializers must read
  `result.nbt` themselves (SNBT string or JSON object) — vanilla drops it.
- Register loot function `opencomputers:copy_color`; dynamic drops `opencomputers:item_data` and
  `opencomputers:volatile_contents` are still used by loot tables.
- Damage types `opencomputers:nanomachines_overload` / `nanomachines_hungry` exist as data; build
  sources with `new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key))`.
  Keep `.1/.2/.3` random death message suffixes by overriding `getLocalizedDeathMessage`.
- Mining: all OC blocks are in `minecraft:mineable/pickaxe`; blocks must NOT set `requiresCorrectToolForDrops`.
- Atlas: `assets/minecraft/atlases/blocks.json` stitches `blocks/` and `items/` dirs, so every
  `opencomputers:blocks/*` / `opencomputers:items/*` texture is an atlas sprite.
- Code-baked models (cable, netsplitter, print, robot, robotafterimage use `block/air`; screen and
  rack were replaced at bake time) need either BERs or platform model hooks
  (Forge `ModelEvent.ModifyBakingResult`, Fabric `ModelLoadingPlugin`). The drone item uses
  `builtin/entity` → needs a BEWLR (Forge `IClientItemExtensions`, Fabric `BuiltinItemRendererRegistry`).
- Lang: `block.opencomputers.<id>`, `itemGroup.opencomputers`, `entity.opencomputers.drone` added.

## API changes (done; callers must follow)

- Type swaps only; API class/method names unchanged. `internal.Case/Rack/Adapter` extend `Container`,
  `internal.Robot` extends `WorldlyContainer`; `Agent.mainInventory()/equipmentInventory()` and
  `InventoryProvider.getInventory()` return `Container`. Vector3f/Vector4f are `org.joml` (`.x()`).
- Manual rendering uses `GuiGraphics`: `TabIconRenderer.render(GuiGraphics)`,
  `ImageRenderer.render(GuiGraphics, int, int)`. `TextBuffer.renderText(PoseStack)` and
  `UpgradeRenderer.render(PoseStack, MultiBufferSource, ...)` stay on PoseStack.
- `api.CreativeTab.instance` (CreativeModeTab) is null until OC sets it after registering its tab.
- `prefab.TileEntityEnvironment` / `TileEntitySidedEnvironment`: ctor `(type, pos, state, ...)`,
  `load`/`saveAdditional`; ticking via the block's `getTicker`; `onLoad()`/`onChunkUnloaded()` must be
  called by OC itself on Fabric.
- **Events:** API events extend `li.cil.oc.api.event.OCEvent`; post with
  `EventBus.INSTANCE.post(event)` (returns true if canceled), listen with
  `EventBus.INSTANCE.register(Type.class, listener)`. Replaces `MinecraftForge.EVENT_BUS`.
- **IMC:** `li.cil.oc.api.detail.IMCAPI.handle(String method, Object payload)`; OC init sets `API.imc`
  (implemented by `common.IMC`) and then calls `li.cil.oc.api.IMC.processPending()`.
- `Driver.itemHandlerFor` / `DriverAPI.itemHandlerFor` return `li.cil.oc.common.transfer.ItemHandler`.
  `MultiTank.getFluidTank(int)` returns `li.cil.oc.common.transfer.FluidHandler` (single-tank view, mB).
- `SideTracker.addServerThread()` registers extra server threads.

## util / top-level contracts (done; callers must follow)

- **Proxy:** `common.Proxy` has no-arg `preInit()`, `init()`, `postInit()`, `initClient()`;
  `client.Proxy` overrides. `OpenComputers.init()` loads Settings, creates the proxy reflectively,
  calls `preInit()`, `ThreadPoolFactory.init()`, `Mods.preInit()` (static), `CreativeTab.register()`,
  `Blocks.init()`, `Items.init()`; `LifecycleEvent.SETUP` → `proxy().init()`,
  `SERVER_BEFORE_START` → `proxy().postInit()`. Client proxy must call `Audio.init()` and set
  `api.CreativeTab.instance = CreativeTab.TAB.get()`. `Items.decorateCreativeTab(NonNullList<ItemStack>)` must exist.
  `DebugCard.AccessContext` needs public fields `player`, `nonce`.
- **Settings:** `Settings.get().field`; tuples are `Pair`; `Settings.basicScreenPixels()` static;
  `Settings.DebugCardAccess` (`Forbidden.INSTANCE`, `Allowed.INSTANCE`, `Whitelist`).
- **Extension helpers:** `ExtendedWorld.getBlock(world, pos)` (BlockGetter/Level variants),
  `ExtendedArguments.checkSlot(args, ItemHandler|Container, n)`, `checkSideForAction(args, i)`,
  `ExtendedArguments.BUCKET_VOLUME = 1000`, `TankProperties(long capacity, FluidStack contents)`;
  `ExtendedInventory.asList(container)` (live List view); `ExtendedLuaState.pushValue(lua, v)`;
  `ExtendedEnumFacing.getRotation(facing, axis)`.
- **ExtendedNBT:** `toNbt(...)` overloads; `xxxIterableToNbt(Iterable)` → `List<XTag>`;
  `setNewCompoundTag(nbt, name, Consumer<CompoundTag>)`, `setNewTagList(nbt, name, Iterable|Tag...)`,
  `getDirection/setDirection` (Optional<Direction>), `get/setBooleanArray`,
  `appendNewCompoundTag(list, Consumer)`, `append`, `foreach`, `map(list, Function)` → List,
  `toTagArray(list, Class<T>)`, `toTypedMap(tag)`, `typedMapToNbt(map)`.
- **BlockPosition:** public final `x,y,z`, `Optional<Level> world`; static `BlockPosition.apply(...)`
  overloads (ints/doubles/Vec3/BlockPos [+Level], EnvironmentHost, Entity); `bounds()`, `toBlockPos()`, `toVec3()`.
- **InventoryUtils / FluidUtils:** operate on `ItemHandler`/`FluidHandler`/`ItemFluidHandler`;
  `InventoryUtils.asItemHandler(Container[, side])`; Scala default args → overloads; Extractor = `IntSupplier`;
  fluid amounts `long` mB; `FluidUtils.fluidHandlerOf(stack)` works on a copy (`getContainer()`).
- **Lua:** `li.cil.repack.org.luaj.vm2.*`, `li.cil.repack.com.naef.jnlua.*`. `new ScalaClosure(Function<Varargs,Varargs>)`.
- **ResultWrapper.result(Object...)** (varargs; pass `(Object) arr` for a single array); `ResultWrapper.unit` sentinel.
- `MovingAverage.apply()` / `add(v)`. `util.TextBuffer`: `foreground()/setForeground`, `background()/setBackground`,
  `format()/setFormat`, `size()` → Pair, `setSize(w,h)`; fields `color`, `buffer`, `width`, `height`.
- `PackedColor`: `Color(value[, isPalette])`, `ColorFormat`, `SingleBitFormat.INSTANCE`, `PackedColor.Depth.format(depth)`.
- `RTree<Data>(int M, Function<Data, Triple<Double,Double,Double>>)`; `GameTimeFormatter.mktime` → `Optional<Integer>`.
- `PlayerUtils.persistedData(player)` (backed by mixins replacing Forge persistent data).
- `ScalaEnum` gone → Java enums. `StackOption` gone → `ItemStack`/`ItemStack.EMPTY`.
