package li.cil.oc.common.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.Constants;
import li.cil.oc.CreativeTab;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.Adapter;
import li.cil.oc.common.block.Assembler;
import li.cil.oc.common.block.Cable;
import li.cil.oc.common.block.Capacitor;
import li.cil.oc.common.block.CarpetedCapacitor;
import li.cil.oc.common.block.Case;
import li.cil.oc.common.block.ChameliumBlock;
import li.cil.oc.common.block.Charger;
import li.cil.oc.common.block.Disassembler;
import li.cil.oc.common.block.DiskDrive;
import li.cil.oc.common.block.FakeEndstone;
import li.cil.oc.common.block.Geolyzer;
import li.cil.oc.common.block.Hologram;
import li.cil.oc.common.block.Keyboard;
import li.cil.oc.common.block.Microcontroller;
import li.cil.oc.common.block.MotionSensor;
import li.cil.oc.common.block.NetSplitter;
import li.cil.oc.common.block.PowerConverter;
import li.cil.oc.common.block.PowerDistributor;
import li.cil.oc.common.block.Print;
import li.cil.oc.common.block.Printer;
import li.cil.oc.common.block.Rack;
import li.cil.oc.common.block.Raid;
import li.cil.oc.common.block.Redstone;
import li.cil.oc.common.block.Relay;
import li.cil.oc.common.block.RobotAfterimage;
import li.cil.oc.common.block.RobotProxy;
import li.cil.oc.common.block.Screen;
import li.cil.oc.common.block.Transposer;
import li.cil.oc.common.block.Waypoint;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Block registration. Blocks go into {@link #BLOCKS}, their block items into {@link Items#ITEMS}
 * (via {@link Items#registerBlock}). Look blocks up by name with {@code api.Items.get(name).block()}
 * or {@link #get(String)}.
 */
public final class Blocks {
    private Blocks() {
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(OpenComputers.ID, Registries.BLOCK);

    private static final Map<String, RegistrySupplier<Block>> byName = new HashMap<>();

    /** Registry supplier of the block registered under the given OC name (e.g. {@code Constants.BlockName.CaseTier1}). */
    public static RegistrySupplier<Block> get(String name) {
        return byName.get(name);
    }

    /** Called by Items.registerBlock / registerBlockOnly. */
    static RegistrySupplier<Block> registerBlock(String id, Supplier<? extends Block> block) {
        final RegistrySupplier<Block> supplier = BLOCKS.register(id, block::get);
        byName.put(id, supplier);
        return supplier;
    }

    private static Properties defaultProps() {
        // Formerly Material.METAL. Mining with pickaxes comes from the mineable/pickaxe tag;
        // OC blocks must not require the correct tool.
        return Properties.of().mapColor(MapColor.METAL).strength(2, 5);
    }

    private static Properties stoneProps() {
        return Properties.of().mapColor(MapColor.STONE).strength(2, 5);
    }

    private static Item.Properties defaultItemProps() {
        return new Item.Properties().arch$tab(CreativeTab.TAB);
    }

    private static boolean initialized;

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        Items.registerBlock(() -> new Adapter(defaultProps()), Constants.BlockName.Adapter, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Assembler(defaultProps()), Constants.BlockName.Assembler, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Cable(defaultProps()), Constants.BlockName.Cable, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Capacitor(defaultProps()), Constants.BlockName.Capacitor, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Case(defaultProps(), Tier.One), Constants.BlockName.CaseTier1, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Case(defaultProps(), Tier.Three), Constants.BlockName.CaseTier3, () -> defaultItemProps().rarity(Rarity.RARE));
        Items.registerBlock(() -> new Case(defaultProps(), Tier.Two), Constants.BlockName.CaseTier2, () -> defaultItemProps().rarity(Rarity.UNCOMMON));
        Items.registerBlock(() -> new ChameliumBlock(stoneProps()), Constants.BlockName.ChameliumBlock, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Charger(defaultProps()), Constants.BlockName.Charger, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Disassembler(defaultProps()), Constants.BlockName.Disassembler, Blocks::defaultItemProps);
        Items.registerBlock(() -> new DiskDrive(defaultProps()), Constants.BlockName.DiskDrive, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Geolyzer(defaultProps()), Constants.BlockName.Geolyzer, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Hologram(defaultProps(), Tier.One), Constants.BlockName.HologramTier1, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Hologram(defaultProps(), Tier.Two), Constants.BlockName.HologramTier2, () -> defaultItemProps().rarity(Rarity.UNCOMMON));
        Items.registerBlock(() -> new Keyboard(stoneProps().noOcclusion()), Constants.BlockName.Keyboard, Blocks::defaultItemProps);
        Items.registerBlock(() -> new MotionSensor(defaultProps()), Constants.BlockName.MotionSensor, Blocks::defaultItemProps);
        Items.registerBlock(() -> new PowerConverter(defaultProps()), Constants.BlockName.PowerConverter,
            () -> !Settings.get().ignorePower ? defaultItemProps() : new Item.Properties());
        Items.registerBlock(() -> new PowerDistributor(defaultProps()), Constants.BlockName.PowerDistributor, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Printer(defaultProps()), Constants.BlockName.Printer, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Raid(defaultProps()), Constants.BlockName.Raid, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Redstone(defaultProps()), Constants.BlockName.Redstone, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Relay(defaultProps()), Constants.BlockName.Relay, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Screen(defaultProps(), Tier.One), Constants.BlockName.ScreenTier1, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Screen(defaultProps(), Tier.Three), Constants.BlockName.ScreenTier3, () -> defaultItemProps().rarity(Rarity.RARE));
        Items.registerBlock(() -> new Screen(defaultProps(), Tier.Two), Constants.BlockName.ScreenTier2, () -> defaultItemProps().rarity(Rarity.UNCOMMON));
        Items.registerBlock(() -> new Rack(defaultProps()), Constants.BlockName.Rack, Blocks::defaultItemProps);
        Items.registerBlock(() -> new Waypoint(defaultProps()), Constants.BlockName.Waypoint, Blocks::defaultItemProps);

        Items.registerBlock(() -> new Case(defaultProps(), Tier.Four), Constants.BlockName.CaseCreative, () -> defaultItemProps().rarity(Rarity.EPIC));
        Items.registerBlock(() -> new Microcontroller(defaultProps()), Constants.BlockName.Microcontroller, Item.Properties::new);
        Items.registerBlock(() -> new Print(Properties.of().mapColor(MapColor.METAL).strength(1, 5).noOcclusion().dynamicShape()), Constants.BlockName.Print, Item.Properties::new);
        // Not air(): an "air" block in an otherwise empty chunk section can never be replaced by
        // air again (LevelChunk.setBlockState skips that), so the after-image stayed around.
        Items.registerBlockOnly(() -> new RobotAfterimage(Properties.of().noCollission().replaceable().instabreak().noOcclusion().dynamicShape().noLootTable()), Constants.BlockName.RobotAfterimage);
        Items.registerBlock(() -> new RobotProxy(defaultProps().noOcclusion().dynamicShape()), Constants.BlockName.Robot, Item.Properties::new);

        // v1.5.10
        Items.registerBlock(() -> new FakeEndstone(Properties.of().mapColor(MapColor.STONE).strength(3, 15)), Constants.BlockName.Endstone, Blocks::defaultItemProps);

        // v1.5.14
        Items.registerBlock(() -> new NetSplitter(defaultProps()), Constants.BlockName.NetSplitter, Blocks::defaultItemProps);

        // v1.5.16
        Items.registerBlock(() -> new Transposer(defaultProps()), Constants.BlockName.Transposer, Blocks::defaultItemProps);

        // v1.7.2
        Items.registerBlock(() -> new CarpetedCapacitor(defaultProps()), Constants.BlockName.CarpetedCapacitor, Blocks::defaultItemProps);

        BLOCKS.register();
    }
}
