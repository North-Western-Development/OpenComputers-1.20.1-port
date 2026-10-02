package li.cil.oc.common.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.Constants;
import li.cil.oc.CreativeTab;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.common.Loot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.item.data.HoverBootsData;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.common.platform.ItemPlatform;
import li.cil.oc.integration.opencomputers.ModOpenComputers;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Item registration and OC's item lookup ({@code api.Items} delegates here via {@link #INSTANCE}).
 * <p>
 * Port notes: registration goes through Architectury's {@link #ITEMS} DeferredRegister, so
 * {@link ItemInfo#item()} / {@link ItemInfo#block()} resolve lazily and must not be called before
 * the registries were populated. {@code get(name)} / {@code get(stack)} and the {@link ItemAPI}
 * methods are instance methods (use {@code Items.INSTANCE}); everything else is static (callable
 * through {@code INSTANCE} as well).
 */
public final class Items implements ItemAPI {
    public static final Items INSTANCE = new Items();

    private Items() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(OpenComputers.ID, Registries.ITEM);

    public static final Map<String, ItemInfo> descriptors = new LinkedHashMap<>();

    public static final Map<String, String> aliases = Map.of(
        "datacard", Constants.ItemName.DataCardTier1,
        "wlancard", Constants.ItemName.WirelessNetworkCardTier2
    );

    private static final Map<String, RegistrySupplier<Item>> itemsByName = new HashMap<>();

    @Override
    @Nullable
    public ItemInfo get(String name) {
        return descriptors.get(name);
    }

    @Override
    @Nullable
    public ItemInfo get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        final ResourceLocation key;
        if (stack.getItem() instanceof BlockItem blockItem) {
            key = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        } else {
            key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        }
        if (key == null || !OpenComputers.ID.equals(key.getNamespace())) return null;
        final ItemInfo info = descriptors.get(key.getPath());
        if (info == null) return null;
        // Make sure this really is the registered block / item (not some registerStack entry).
        if (info instanceof RegisteredInfo registered && registered.matches(stack)) return info;
        return null;
    }

    /** Registry supplier of the item registered under the given OC name, or null. */
    @Nullable
    public static RegistrySupplier<Item> getItem(String name) {
        return itemsByName.get(name);
    }

    // ----------------------------------------------------------------------- //

    private abstract static class RegisteredInfo implements ItemInfo {
        private final String id;

        RegisteredInfo(String id) {
            this.id = id;
        }

        @Override
        public String name() {
            return id;
        }

        abstract boolean matches(ItemStack stack);
    }

    public static RegistrySupplier<Block> registerBlockOnly(Supplier<? extends Block> instance, String id) {
        if (descriptors.containsKey(id)) return Blocks.get(id);
        final RegistrySupplier<Block> block = Blocks.registerBlock(id, instance);
        descriptors.put(id, new RegisteredInfo(id) {
            @Override
            public Block block() {
                return block.get();
            }

            @Override
            public Item item() {
                return null;
            }

            @Override
            public ItemStack createItemStack(int size) {
                OpenComputers.log.warn("Attempt to get ItemStack for block " + block.get() + " without item form");
                return ItemStack.EMPTY;
            }

            @Override
            boolean matches(ItemStack stack) {
                return false;
            }
        });
        return block;
    }

    public static RegistrySupplier<Block> registerBlock(Supplier<? extends Block> instance, String id, Supplier<Properties> itemProps) {
        if (descriptors.containsKey(id)) return Blocks.get(id);
        final RegistrySupplier<Block> block = Blocks.registerBlock(id, instance);
        final RegistrySupplier<Item> itemInst = ITEMS.register(id, () -> new li.cil.oc.common.block.Item(block.get(), itemProps.get()));
        itemsByName.put(id, itemInst);
        descriptors.put(id, new RegisteredInfo(id) {
            @Override
            public Block block() {
                return block.get();
            }

            @Override
            public Item item() {
                return itemInst.get();
            }

            @Override
            public ItemStack createItemStack(int size) {
                if (block.get() instanceof SimpleBlock simple) return simple.createItemStack(size);
                return new ItemStack(block.get(), size);
            }

            @Override
            boolean matches(ItemStack stack) {
                return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block.get();
            }
        });
        return block;
    }

    public static <T extends Item> RegistrySupplier<T> registerItem(Supplier<T> instance, String id) {
        @SuppressWarnings("unchecked")
        final RegistrySupplier<T> existing = (RegistrySupplier<T>) (RegistrySupplier<?>) itemsByName.get(id);
        if (descriptors.containsKey(id)) return existing;
        final RegistrySupplier<T> item = ITEMS.register(id, instance);
        @SuppressWarnings("unchecked")
        final RegistrySupplier<Item> asItem = (RegistrySupplier<Item>) (RegistrySupplier<?>) item;
        itemsByName.put(id, asItem);
        descriptors.put(id, new RegisteredInfo(id) {
            @Override
            public Block block() {
                return null;
            }

            @Override
            public Item item() {
                return item.get();
            }

            @Override
            public ItemStack createItemStack(int size) {
                final Item instance = item.get();
                if (instance instanceof SimpleItem simple) return simple.createItemStack(size);
                return new ItemStack(instance, size);
            }

            @Override
            boolean matches(ItemStack stack) {
                return stack.getItem() == item.get();
            }
        });
        return item;
    }

    public static ItemStack registerStack(ItemStack stack, String id) {
        final ItemStack immutableStack = stack.copy();
        registerStack(() -> immutableStack, id);
        return stack;
    }

    /** Like {@link #registerStack(ItemStack, String)}, but the stack is created on first use (after registration). */
    public static void registerStack(Supplier<ItemStack> stack, String id) {
        descriptors.put(id, new ItemInfo() {
            private ItemStack immutableStack;

            private ItemStack stack() {
                if (immutableStack == null) immutableStack = stack.get().copy();
                return immutableStack;
            }

            @Override
            public String name() {
                return id;
            }

            @Override
            public Block block() {
                return null;
            }

            @Override
            public ItemStack createItemStack(int size) {
                final ItemStack copy = stack().copy();
                copy.setCount(size);
                return copy;
            }

            @Override
            public Item item() {
                return stack().getItem();
            }
        });
    }

    // ----------------------------------------------------------------------- //

    public static final List<Supplier<ItemStack>> registeredItems = new ArrayList<>();

    @Override
    public ItemStack registerFloppy(String name, ResourceLocation loc, DyeColor color, Callable<FileSystem> factory, boolean doRecipeCycling) {
        final ItemStack stack = Loot.registerLootDisk(name, loc, color, factory, doRecipeCycling);

        registeredItems.add(() -> stack);

        return stack.copy();
    }

    @Override
    public ItemStack registerEEPROM(String name, byte[] code, byte[] data, boolean readonly) {
        final ItemStack stack = createEEPROM(name, code, data, readonly);

        registeredItems.add(() -> stack);

        return stack.copy();
    }

    private static ItemStack createEEPROM(String name, byte[] code, byte[] data, boolean readonly) {
        final ItemStack stack = INSTANCE.get(Constants.ItemName.EEPROM).createItemStack(1);
        final var nbt = stack.getOrCreateTagElement(Settings.namespace + "data");
        if (name != null) {
            final String trimmed = name.trim();
            nbt.putString(Settings.namespace + "label", trimmed.substring(0, Math.min(24, trimmed.length())));
        }
        if (code != null) {
            nbt.putByteArray(Settings.namespace + "eeprom", Arrays.copyOf(code, Math.min(code.length, Settings.get().eepromSize)));
        }
        if (data != null) {
            nbt.putByteArray(Settings.namespace + "userdata", Arrays.copyOf(data, Math.min(data.length, Settings.get().eepromDataSize)));
        }
        nbt.putBoolean(Settings.namespace + "readonly", readonly);
        return stack;
    }

    // ----------------------------------------------------------------------- //

    private static ItemStack safeGetStack(String name) {
        final ItemInfo info = INSTANCE.get(name);
        return info != null ? info.createItemStack(1) : ItemStack.EMPTY;
    }

    private static ItemStack[] nonEmpty(ItemStack... stacks) {
        return Arrays.stream(stacks).filter(s -> !s.isEmpty()).toArray(ItemStack[]::new);
    }

    private static ItemStack[] padTo(int size, ItemStack... stacks) {
        final ItemStack[] result = Arrays.copyOf(stacks, Math.max(size, stacks.length));
        for (int i = stacks.length; i < result.length; i++) result[i] = ItemStack.EMPTY;
        return result;
    }

    public static ItemStack createConfiguredDrone() {
        final var data = new DroneData();

        data.name = "Crecopter";
        data.tier = Tier.Four;
        data.storedEnergy = (int) Settings.get().bufferDrone;
        data.components = nonEmpty(
            safeGetStack(Constants.ItemName.InventoryUpgrade),
            safeGetStack(Constants.ItemName.InventoryUpgrade),
            safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
            safeGetStack(Constants.ItemName.TankUpgrade),
            safeGetStack(Constants.ItemName.TankControllerUpgrade),
            safeGetStack(Constants.ItemName.LeashUpgrade),
            safeGetStack(Constants.ItemName.AngelUpgrade),

            safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

            LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
            safeGetStack(Constants.ItemName.RAMTier6),
            safeGetStack(Constants.ItemName.RAMTier6)
        );

        return data.createItemStack();
    }

    public static ItemStack createConfiguredMicrocontroller() {
        final var data = new MicrocontrollerData();

        data.tier = Tier.Four;
        data.storedEnergy = (int) Settings.get().bufferMicrocontroller;
        data.components = nonEmpty(
            safeGetStack(Constants.ItemName.SignUpgrade),
            safeGetStack(Constants.ItemName.PistonUpgrade),

            safeGetStack(Constants.ItemName.RedstoneCardTier2),
            safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

            LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
            safeGetStack(Constants.ItemName.RAMTier6),
            safeGetStack(Constants.ItemName.RAMTier6)
        );

        return data.createItemStack();
    }

    public static ItemStack createConfiguredRobot() {
        final var data = new RobotData();

        data.name = "Creatix";
        data.tier = Tier.Four;
        data.robotEnergy = (int) Settings.get().bufferRobot;
        data.totalEnergy = data.robotEnergy;
        data.components = nonEmpty(
            safeGetStack(Constants.BlockName.ScreenTier1),
            safeGetStack(Constants.BlockName.Keyboard),
            safeGetStack(Constants.BlockName.Geolyzer),
            safeGetStack(Constants.ItemName.InventoryUpgrade),
            safeGetStack(Constants.ItemName.InventoryUpgrade),
            safeGetStack(Constants.ItemName.InventoryUpgrade),
            safeGetStack(Constants.ItemName.InventoryUpgrade),
            safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
            safeGetStack(Constants.ItemName.TankUpgrade),
            safeGetStack(Constants.ItemName.TankControllerUpgrade),
            safeGetStack(Constants.ItemName.CraftingUpgrade),
            safeGetStack(Constants.ItemName.HoverUpgradeTier2),
            safeGetStack(Constants.ItemName.AngelUpgrade),
            safeGetStack(Constants.ItemName.TradingUpgrade),
            safeGetStack(Constants.ItemName.ExperienceUpgrade),

            safeGetStack(Constants.ItemName.GraphicsCardTier3),
            safeGetStack(Constants.ItemName.RedstoneCardTier2),
            safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
            safeGetStack(Constants.ItemName.InternetCard),

            LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
            safeGetStack(Constants.ItemName.RAMTier6),
            safeGetStack(Constants.ItemName.RAMTier6),

            safeGetStack(Constants.ItemName.LuaBios),
            safeGetStack(Constants.ItemName.OpenOS),
            safeGetStack(Constants.ItemName.HDDTier3)
        );
        data.containers = nonEmpty(
            safeGetStack(Constants.ItemName.CardContainerTier3),
            safeGetStack(Constants.ItemName.UpgradeContainerTier3),
            safeGetStack(Constants.BlockName.DiskDrive)
        );

        return data.createItemStack();
    }

    public static ItemStack createConfiguredTablet() {
        final var data = new TabletData();

        data.tier = Tier.Four;
        data.energy = Settings.get().bufferTablet;
        data.maxEnergy = data.energy;
        data.items = padTo(32,
            safeGetStack(Constants.BlockName.ScreenTier1),
            safeGetStack(Constants.BlockName.Keyboard),

            safeGetStack(Constants.ItemName.SignUpgrade),
            safeGetStack(Constants.ItemName.PistonUpgrade),
            safeGetStack(Constants.BlockName.Geolyzer),
            safeGetStack(Constants.ItemName.NavigationUpgrade),
            safeGetStack(Constants.ItemName.Analyzer),

            safeGetStack(Constants.ItemName.GraphicsCardTier2),
            safeGetStack(Constants.ItemName.RedstoneCardTier2),
            safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),

            LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
            safeGetStack(Constants.ItemName.RAMTier6),
            safeGetStack(Constants.ItemName.RAMTier6),

            safeGetStack(Constants.ItemName.LuaBios),
            safeGetStack(Constants.ItemName.HDDTier3)
        );
        data.items[31] = safeGetStack(Constants.ItemName.OpenOS);
        data.container = safeGetStack(Constants.BlockName.DiskDrive);

        return data.createItemStack();
    }

    public static ItemStack createChargedHoverBoots() {
        final HoverBootsData data = new HoverBootsData();
        data.charge = Settings.get().bufferHoverBoots;

        return data.createItemStack();
    }

    // ----------------------------------------------------------------------- //

    private static Properties defaultProps() {
        return new Properties().arch$tab(CreativeTab.TAB);
    }

    private static boolean initialized;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        initMaterials();
        initTools();
        initComponents();
        initCards();
        initUpgrades();
        initStorage();
        initSpecial();

        // Register aliases.
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            descriptors.computeIfAbsent(alias.getKey(), k -> descriptors.get(alias.getValue()));
        }

        ITEMS.register();

        li.cil.oc.common.item.ItemEvents.register();
    }

    // Crafting materials.
    private static void initMaterials() {
        registerItem(() -> new li.cil.oc.common.item.CuttingWire(defaultProps()), Constants.ItemName.CuttingWire);
        registerItem(() -> new li.cil.oc.common.item.Acid(defaultProps()), Constants.ItemName.Acid);
        registerItem(() -> new li.cil.oc.common.item.RawCircuitBoard(defaultProps()), Constants.ItemName.RawCircuitBoard);
        registerItem(() -> new li.cil.oc.common.item.CircuitBoard(defaultProps()), Constants.ItemName.CircuitBoard);
        registerItem(() -> new li.cil.oc.common.item.PrintedCircuitBoard(defaultProps()), Constants.ItemName.PrintedCircuitBoard);
        registerItem(() -> new li.cil.oc.common.item.CardBase(defaultProps()), Constants.ItemName.Card);
        registerItem(() -> new li.cil.oc.common.item.Transistor(defaultProps()), Constants.ItemName.Transistor);
        registerItem(() -> new li.cil.oc.common.item.Microchip(defaultProps(), Tier.One), Constants.ItemName.ChipTier1);
        registerItem(() -> new li.cil.oc.common.item.Microchip(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.ChipTier2);
        registerItem(() -> new li.cil.oc.common.item.Microchip(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.ChipTier3);
        registerItem(() -> new li.cil.oc.common.item.ALU(defaultProps()), Constants.ItemName.Alu);
        registerItem(() -> new li.cil.oc.common.item.ControlUnit(defaultProps()), Constants.ItemName.ControlUnit);
        registerItem(() -> new li.cil.oc.common.item.Disk(defaultProps()), Constants.ItemName.Disk);
        registerItem(() -> new li.cil.oc.common.item.Interweb(defaultProps()), Constants.ItemName.Interweb);
        registerItem(() -> new li.cil.oc.common.item.ButtonGroup(defaultProps()), Constants.ItemName.ButtonGroup);
        registerItem(() -> new li.cil.oc.common.item.ArrowKeys(defaultProps()), Constants.ItemName.ArrowKeys);
        registerItem(() -> new li.cil.oc.common.item.NumPad(defaultProps()), Constants.ItemName.NumPad);

        registerItem(() -> new li.cil.oc.common.item.TabletCase(defaultProps(), Tier.One), Constants.ItemName.TabletCaseTier1);
        registerItem(() -> new li.cil.oc.common.item.TabletCase(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.TabletCaseTier2);
        registerItem(() -> new li.cil.oc.common.item.TabletCase(defaultProps().rarity(Rarity.EPIC), Tier.Four), Constants.ItemName.TabletCaseCreative);
        registerItem(() -> new li.cil.oc.common.item.MicrocontrollerCase(defaultProps(), Tier.One), Constants.ItemName.MicrocontrollerCaseTier1);
        registerItem(() -> new li.cil.oc.common.item.MicrocontrollerCase(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.MicrocontrollerCaseTier2);
        registerItem(() -> new li.cil.oc.common.item.MicrocontrollerCase(defaultProps().rarity(Rarity.EPIC), Tier.Four), Constants.ItemName.MicrocontrollerCaseCreative);
        registerItem(() -> new li.cil.oc.common.item.DroneCase(defaultProps(), Tier.One), Constants.ItemName.DroneCaseTier1);
        registerItem(() -> new li.cil.oc.common.item.DroneCase(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.DroneCaseTier2);
        registerItem(() -> new li.cil.oc.common.item.DroneCase(defaultProps().rarity(Rarity.EPIC), Tier.Four), Constants.ItemName.DroneCaseCreative);

        final RegistrySupplier<li.cil.oc.common.item.InkCartridgeEmpty> inkCartridgeEmpty =
            registerItem(() -> new li.cil.oc.common.item.InkCartridgeEmpty(defaultProps().stacksTo(1)), Constants.ItemName.InkCartridgeEmpty);
        // Registered after the empty cartridge, so it exists when this supplier runs.
        registerItem(() -> new li.cil.oc.common.item.InkCartridge(defaultProps().stacksTo(1).craftRemainder(inkCartridgeEmpty.get())), Constants.ItemName.InkCartridge);
        registerItem(() -> new li.cil.oc.common.item.Chamelium(defaultProps()), Constants.ItemName.Chamelium);

        registerItem(() -> new li.cil.oc.common.item.DiamondChip(defaultProps()), Constants.ItemName.DiamondChip);
    }

    // All kinds of tools.
    private static void initTools() {
        registerItem(() -> new li.cil.oc.common.item.Analyzer(defaultProps()), Constants.ItemName.Analyzer);
        registerItem(() -> new li.cil.oc.common.item.Debugger(defaultProps()), Constants.ItemName.Debugger);
        registerItem(() -> new li.cil.oc.common.item.Terminal(defaultProps().stacksTo(1)), Constants.ItemName.Terminal);
        registerItem(() -> new li.cil.oc.common.item.TexturePicker(defaultProps()), Constants.ItemName.TexturePicker);
        registerItem(() -> new li.cil.oc.common.item.Manual(defaultProps()), Constants.ItemName.Manual);
        // TODO(port): Forge ToolType "wrench" (addToolType(WrenchType, 1)) is gone.
        registerItem(() -> new li.cil.oc.common.item.Wrench(defaultProps().stacksTo(1)), Constants.ItemName.Wrench);

        // 1.5.11
        // setNoRepair (Forge) -> HoverBoots.isValidRepairItem returns false.
        final RegistrySupplier<li.cil.oc.common.item.HoverBoots> hoverBoots =
            registerItem(() -> ItemPlatform.createHoverBoots(defaultProps().stacksTo(1).rarity(Rarity.UNCOMMON)), Constants.ItemName.HoverBoots);
        hoverBoots.listen(li.cil.oc.common.item.HoverBoots::registerCauldronInteraction);

        // 1.5.18
        registerItem(() -> new li.cil.oc.common.item.Nanomachines(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.Nanomachines);
    }

    // General purpose components.
    private static void initComponents() {
        registerItem(() -> new li.cil.oc.common.item.CPU(defaultProps(), Tier.One), Constants.ItemName.CPUTier1);
        registerItem(() -> new li.cil.oc.common.item.CPU(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.CPUTier2);
        registerItem(() -> new li.cil.oc.common.item.CPU(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.CPUTier3);

        registerItem(() -> new li.cil.oc.common.item.ComponentBus(defaultProps(), Tier.One), Constants.ItemName.ComponentBusTier1);
        registerItem(() -> new li.cil.oc.common.item.ComponentBus(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.ComponentBusTier2);
        registerItem(() -> new li.cil.oc.common.item.ComponentBus(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.ComponentBusTier3);

        registerItem(() -> new li.cil.oc.common.item.Memory(defaultProps(), Tier.One), Constants.ItemName.RAMTier1);
        registerItem(() -> new li.cil.oc.common.item.Memory(defaultProps(), Tier.Two), Constants.ItemName.RAMTier2);
        registerItem(() -> new li.cil.oc.common.item.Memory(defaultProps().rarity(Rarity.UNCOMMON), Tier.Three), Constants.ItemName.RAMTier3);
        registerItem(() -> new li.cil.oc.common.item.Memory(defaultProps().rarity(Rarity.UNCOMMON), Tier.Four), Constants.ItemName.RAMTier4);
        registerItem(() -> new li.cil.oc.common.item.Memory(defaultProps().rarity(Rarity.RARE), Tier.Five), Constants.ItemName.RAMTier5);
        registerItem(() -> new li.cil.oc.common.item.Memory(defaultProps().rarity(Rarity.RARE), Tier.Six), Constants.ItemName.RAMTier6);

        registerItem(() -> new li.cil.oc.common.item.Server(defaultProps().stacksTo(1).rarity(Rarity.EPIC), Tier.Four), Constants.ItemName.ServerCreative);
        registerItem(() -> new li.cil.oc.common.item.Server(defaultProps().stacksTo(1), Tier.One), Constants.ItemName.ServerTier1);
        registerItem(() -> new li.cil.oc.common.item.Server(defaultProps().stacksTo(1).rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.ServerTier2);
        registerItem(() -> new li.cil.oc.common.item.Server(defaultProps().stacksTo(1).rarity(Rarity.RARE), Tier.Three), Constants.ItemName.ServerTier3);

        // 1.5.10
        registerItem(() -> new li.cil.oc.common.item.APU(defaultProps().rarity(Rarity.UNCOMMON), Tier.One), Constants.ItemName.APUTier1);
        registerItem(() -> new li.cil.oc.common.item.APU(defaultProps().rarity(Rarity.RARE), Tier.Two), Constants.ItemName.APUTier2);

        // 1.5.12
        registerItem(() -> new li.cil.oc.common.item.APU(defaultProps().rarity(Rarity.EPIC), Tier.Three), Constants.ItemName.APUCreative);

        // 1.6
        registerItem(() -> new li.cil.oc.common.item.TerminalServer(defaultProps().stacksTo(1)), Constants.ItemName.TerminalServer);
        registerItem(() -> new li.cil.oc.common.item.DiskDriveMountable(defaultProps().stacksTo(1)), Constants.ItemName.DiskDriveMountable);
    }

    // Card components.
    private static void initCards() {
        registerItem(() -> new li.cil.oc.common.item.DebugCard(defaultProps()), Constants.ItemName.DebugCard);
        registerItem(() -> new li.cil.oc.common.item.GraphicsCard(defaultProps(), Tier.One), Constants.ItemName.GraphicsCardTier1);
        registerItem(() -> new li.cil.oc.common.item.GraphicsCard(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.GraphicsCardTier2);
        registerItem(() -> new li.cil.oc.common.item.GraphicsCard(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.GraphicsCardTier3);
        registerItem(() -> new li.cil.oc.common.item.RedstoneCard(defaultProps(), Tier.One), Constants.ItemName.RedstoneCardTier1);
        // Not in the creative tab by default: only listed if ModOpenComputers.hasRedstoneCardT2 (decorateCreativeTab).
        registerItem(() -> new li.cil.oc.common.item.RedstoneCard(new Properties().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.RedstoneCardTier2);
        registerItem(() -> new li.cil.oc.common.item.NetworkCard(defaultProps()), Constants.ItemName.NetworkCard);
        registerItem(() -> new li.cil.oc.common.item.WirelessNetworkCard(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.WirelessNetworkCardTier2);
        registerItem(() -> new li.cil.oc.common.item.InternetCard(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.InternetCard);
        registerItem(() -> new li.cil.oc.common.item.LinkedCard(defaultProps().rarity(Rarity.RARE)), Constants.ItemName.LinkedCard);

        // 1.5.13
        registerItem(() -> new li.cil.oc.common.item.DataCard(defaultProps(), Tier.One), Constants.ItemName.DataCardTier1);

        // 1.5.15
        registerItem(() -> new li.cil.oc.common.item.DataCard(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.DataCardTier2);
        registerItem(() -> new li.cil.oc.common.item.DataCard(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.DataCardTier3);
    }

    // Upgrade components.
    private static void initUpgrades() {
        registerItem(() -> new li.cil.oc.common.item.UpgradeAngel(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.AngelUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeBattery(defaultProps(), Tier.One), Constants.ItemName.BatteryUpgradeTier1);
        registerItem(() -> new li.cil.oc.common.item.UpgradeBattery(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.BatteryUpgradeTier2);
        registerItem(() -> new li.cil.oc.common.item.UpgradeBattery(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.BatteryUpgradeTier3);
        registerItem(() -> new li.cil.oc.common.item.UpgradeChunkloader(defaultProps().rarity(Rarity.RARE)), Constants.ItemName.ChunkloaderUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeContainerCard(defaultProps(), Tier.One), Constants.ItemName.CardContainerTier1);
        registerItem(() -> new li.cil.oc.common.item.UpgradeContainerCard(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.CardContainerTier2);
        registerItem(() -> new li.cil.oc.common.item.UpgradeContainerCard(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.CardContainerTier3);
        registerItem(() -> new li.cil.oc.common.item.UpgradeContainerUpgrade(defaultProps(), Tier.One), Constants.ItemName.UpgradeContainerTier1);
        registerItem(() -> new li.cil.oc.common.item.UpgradeContainerUpgrade(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.UpgradeContainerTier2);
        registerItem(() -> new li.cil.oc.common.item.UpgradeContainerUpgrade(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.UpgradeContainerTier3);
        registerItem(() -> new li.cil.oc.common.item.UpgradeCrafting(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.CraftingUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeDatabase(defaultProps(), Tier.One), Constants.ItemName.DatabaseUpgradeTier1);
        registerItem(() -> new li.cil.oc.common.item.UpgradeDatabase(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.DatabaseUpgradeTier2);
        registerItem(() -> new li.cil.oc.common.item.UpgradeDatabase(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.DatabaseUpgradeTier3);
        registerItem(() -> new li.cil.oc.common.item.UpgradeExperience(defaultProps().rarity(Rarity.RARE)), Constants.ItemName.ExperienceUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeGenerator(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.GeneratorUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeInventory(defaultProps()), Constants.ItemName.InventoryUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeInventoryController(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.InventoryControllerUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeNavigation(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.NavigationUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradePiston(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.PistonUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeSign(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.SignUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeSolarGenerator(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.SolarGeneratorUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeTank(defaultProps()), Constants.ItemName.TankUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeTankController(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.TankControllerUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeTractorBeam(defaultProps().rarity(Rarity.RARE)), Constants.ItemName.TractorBeamUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeLeash(defaultProps()), Constants.ItemName.LeashUpgrade);

        // 1.5.8
        registerItem(() -> new li.cil.oc.common.item.UpgradeHover(defaultProps(), Tier.One), Constants.ItemName.HoverUpgradeTier1);
        registerItem(() -> new li.cil.oc.common.item.UpgradeHover(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.HoverUpgradeTier2);

        // 1.6
        registerItem(() -> new li.cil.oc.common.item.UpgradeTrading(defaultProps().rarity(Rarity.UNCOMMON)), Constants.ItemName.TradingUpgrade);
        registerItem(() -> new li.cil.oc.common.item.UpgradeMF(defaultProps().rarity(Rarity.RARE)), Constants.ItemName.MFU);

        // 1.7.2
        registerItem(() -> new li.cil.oc.common.item.WirelessNetworkCard(defaultProps(), Tier.One), Constants.ItemName.WirelessNetworkCardTier1);
        registerItem(() -> new li.cil.oc.common.item.ComponentBus(defaultProps().rarity(Rarity.EPIC), Tier.Four), Constants.ItemName.ComponentBusCreative);

        // 1.8
        registerItem(() -> new li.cil.oc.common.item.UpgradeStickyPiston(defaultProps()), Constants.ItemName.StickyPistonUpgrade);
    }

    // Storage media of all kinds.
    private static void initStorage() {
        registerItem(() -> new li.cil.oc.common.item.EEPROM(defaultProps()), Constants.ItemName.EEPROM);
        registerItem(() -> new li.cil.oc.common.item.FloppyDisk(defaultProps()), Constants.ItemName.Floppy);
        registerItem(() -> new li.cil.oc.common.item.HardDiskDrive(defaultProps(), Tier.One), Constants.ItemName.HDDTier1);
        registerItem(() -> new li.cil.oc.common.item.HardDiskDrive(defaultProps().rarity(Rarity.UNCOMMON), Tier.Two), Constants.ItemName.HDDTier2);
        registerItem(() -> new li.cil.oc.common.item.HardDiskDrive(defaultProps().rarity(Rarity.RARE), Tier.Three), Constants.ItemName.HDDTier3);

        // The EEPROM item does not exist yet (deferred registration), so the BIOS stack is created lazily.
        final ItemStack[] luaBios = new ItemStack[1];
        final Supplier<ItemStack> luaBiosStack = () -> {
            if (luaBios[0] == null) {
                final byte[] code = new byte[4 * 1024];
                int count = 0;
                try (InputStream stream = OpenComputers.class.getResourceAsStream(Settings.scriptPath + "bios.lua")) {
                    count = Math.max(0, stream.read(code));
                } catch (Throwable t) {
                    OpenComputers.log.warn("Failed loading the Lua BIOS.", t);
                }
                luaBios[0] = createEEPROM("EEPROM (Lua BIOS)", Arrays.copyOf(code, count), null, false);
            }
            return luaBios[0];
        };
        registeredItems.add(luaBiosStack);
        registerStack(luaBiosStack, Constants.ItemName.LuaBios);
    }

    // Special purpose items that don't fit into any other category.
    private static void initSpecial() {
        // Tablet, drone and present must be assembled / found, so they are not in the creative tab.
        registerItem(() -> new li.cil.oc.common.item.Tablet(new Properties().stacksTo(1)), Constants.ItemName.Tablet);
        registerItem(() -> ItemPlatform.createDrone(new Properties()), Constants.ItemName.Drone);
        registerItem(() -> new li.cil.oc.common.item.Present(new Properties()), Constants.ItemName.Present);
    }

    /** Extra creative tab entries (formerly fillItemCategory / decorateCreativeTab). */
    public static void decorateCreativeTab(NonNullList<ItemStack> list) {
        if (ModOpenComputers.hasRedstoneCardT2) list.add(safeGetStack(Constants.ItemName.RedstoneCardTier2));
        list.add(createChargedHoverBoots());
        list.add(createConfiguredDrone());
        list.add(createConfiguredMicrocontroller());
        list.add(createConfiguredRobot());
        list.add(createConfiguredTablet());
        list.addAll(Loot.disksForClient);
        for (Supplier<ItemStack> stack : registeredItems) list.add(stack.get());
    }
}
