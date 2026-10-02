# common item / init / recipe / template — wave 2 report

- traits.SimpleItem abstract class extends Item; ItemTier marker; CPULike/GPULike/FileSystemLike interfaces with defaults; traits.Chargeable interface + Chargeable.Provider implements transfer.EnergyHandler.
- HoverBoots extends ArmorItem. doesSneakBypassUse → sneakBypassesUse(...); onItemUseFirst 9-arg; item.ItemEvents (RIGHT_CLICK_BLOCK) registered by Items.init().
- Analyzer.register(), Tablet.register() exist (ModOpenComputers calls; Proxy must not).
- TabletWrapper own file (stack, player, world, data, tablet, isDirty, timesChanged, autoSave, machine()); Tablet.Client.INSTANCE / Tablet.Server.INSTANCE (.cache); Tablet.currentlyAnalyzing Optional<AnalyzeContext>.
- Debugger.NetworkDebugger.INSTANCE, Debugger.node(), reconnect().
- init.Items implements ItemAPI (INSTANCE): get(name), get(stack), registerFloppy, registerEEPROM instance; statics init, decorateCreativeTab, registerItem(Supplier,id), registerBlock(Supplier,id,Supplier<Item.Properties>), registerBlockOnly, registerStack, createConfiguredX, createChargedHoverBoots. Items.ITEMS / Blocks.BLOCKS DeferredRegisters; Items.getItem(name), Blocks.get(name) → RegistrySupplier.
- CustomModel: getModelLocation(stack), modelLocations().
- New hook common.platform.ItemPlatform: createDrone(props), createHoverBoots(props). client.ItemClientHooks.
- Recipes: Extended* extend vanilla Shaped/Shapeless and read result.nbt; RecipeSerializers.X RegistrySuppliers; ItemSpecialSerializer factory (id, category, ItemLike).
- Templates: static IMC callbacks; INSTANCE for Drone/Microcontroller/Robot/TabletTemplate; validate → Triple<Boolean, Component, Component[]>; assemble → Pair<ItemStack, Double>; disassemble → Pair<Optional<ItemStack[]>, Optional<ItemStack[]>>.

## Requests
- copy_color registered by server.loot.LootFunctions only (confirmed: LootFunctions.COPY_COLOR exists).
- Platform: PlatformHooks.getEnergyHandler(ItemStack) / Forge cap / TR Energy should return new Chargeable.Provider(stack, item) for OC chargeable items.
- Client: set Drone.customRenderer and HoverBoots.armorModel; Fabric: drone renderer + hover boots ArmorRenderer.
