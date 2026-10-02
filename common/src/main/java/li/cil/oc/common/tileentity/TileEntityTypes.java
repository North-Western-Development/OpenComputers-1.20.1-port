package li.cil.oc.common.tileentity;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class TileEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITY_TYPES = DeferredRegister.create(OpenComputers.ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<Adapter>> ADAPTER = register("adapter",
        () -> BlockEntityType.Builder.of((pos, state) -> new Adapter(TileEntityTypes.ADAPTER.get(), pos, state),
            block(Constants.BlockName.Adapter)));
    public static final RegistrySupplier<BlockEntityType<Assembler>> ASSEMBLER = register("assembler",
        () -> BlockEntityType.Builder.of((pos, state) -> new Assembler(TileEntityTypes.ASSEMBLER.get(), pos, state),
            block(Constants.BlockName.Assembler)));
    public static final RegistrySupplier<BlockEntityType<Cable>> CABLE = register("cable",
        () -> BlockEntityType.Builder.of((pos, state) -> new Cable(TileEntityTypes.CABLE.get(), pos, state),
            block(Constants.BlockName.Cable)));
    public static final RegistrySupplier<BlockEntityType<Capacitor>> CAPACITOR = register("capacitor",
        () -> BlockEntityType.Builder.of((pos, state) -> new Capacitor(TileEntityTypes.CAPACITOR.get(), pos, state),
            block(Constants.BlockName.Capacitor)));
    public static final RegistrySupplier<BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR = register("carpeted_capacitor",
        () -> BlockEntityType.Builder.of((pos, state) -> new CarpetedCapacitor(TileEntityTypes.CARPETED_CAPACITOR.get(), pos, state),
            block(Constants.BlockName.CarpetedCapacitor)));
    public static final RegistrySupplier<BlockEntityType<Case>> CASE = register("case",
        () -> BlockEntityType.Builder.of((pos, state) -> new Case(TileEntityTypes.CASE.get(), pos, state),
            block(Constants.BlockName.CaseCreative),
            block(Constants.BlockName.CaseTier1),
            block(Constants.BlockName.CaseTier2),
            block(Constants.BlockName.CaseTier3)));
    public static final RegistrySupplier<BlockEntityType<Charger>> CHARGER = register("charger",
        () -> BlockEntityType.Builder.of((pos, state) -> new Charger(TileEntityTypes.CHARGER.get(), pos, state),
            block(Constants.BlockName.Charger)));
    public static final RegistrySupplier<BlockEntityType<Disassembler>> DISASSEMBLER = register("disassembler",
        () -> BlockEntityType.Builder.of((pos, state) -> new Disassembler(TileEntityTypes.DISASSEMBLER.get(), pos, state),
            block(Constants.BlockName.Disassembler)));
    public static final RegistrySupplier<BlockEntityType<DiskDrive>> DISK_DRIVE = register("disk_drive",
        () -> BlockEntityType.Builder.of((pos, state) -> new DiskDrive(TileEntityTypes.DISK_DRIVE.get(), pos, state),
            block(Constants.BlockName.DiskDrive)));
    public static final RegistrySupplier<BlockEntityType<Geolyzer>> GEOLYZER = register("geolyzer",
        () -> BlockEntityType.Builder.of((pos, state) -> new Geolyzer(TileEntityTypes.GEOLYZER.get(), pos, state),
            block(Constants.BlockName.Geolyzer)));
    public static final RegistrySupplier<BlockEntityType<Hologram>> HOLOGRAM = register("hologram",
        () -> BlockEntityType.Builder.of((pos, state) -> new Hologram(TileEntityTypes.HOLOGRAM.get(), pos, state),
            block(Constants.BlockName.HologramTier1),
            block(Constants.BlockName.HologramTier2)));
    public static final RegistrySupplier<BlockEntityType<Keyboard>> KEYBOARD = register("keyboard",
        () -> BlockEntityType.Builder.of((pos, state) -> new Keyboard(TileEntityTypes.KEYBOARD.get(), pos, state),
            block(Constants.BlockName.Keyboard)));
    public static final RegistrySupplier<BlockEntityType<Microcontroller>> MICROCONTROLLER = register("microcontroller",
        () -> BlockEntityType.Builder.of((pos, state) -> new Microcontroller(TileEntityTypes.MICROCONTROLLER.get(), pos, state),
            block(Constants.BlockName.Microcontroller)));
    public static final RegistrySupplier<BlockEntityType<MotionSensor>> MOTION_SENSOR = register("motion_sensor",
        () -> BlockEntityType.Builder.of((pos, state) -> new MotionSensor(TileEntityTypes.MOTION_SENSOR.get(), pos, state),
            block(Constants.BlockName.MotionSensor)));
    public static final RegistrySupplier<BlockEntityType<NetSplitter>> NET_SPLITTER = register("net_splitter",
        () -> BlockEntityType.Builder.of((pos, state) -> new NetSplitter(TileEntityTypes.NET_SPLITTER.get(), pos, state),
            block(Constants.BlockName.NetSplitter)));
    public static final RegistrySupplier<BlockEntityType<PowerConverter>> POWER_CONVERTER = register("power_converter",
        () -> BlockEntityType.Builder.of((pos, state) -> new PowerConverter(TileEntityTypes.POWER_CONVERTER.get(), pos, state),
            block(Constants.BlockName.PowerConverter)));
    public static final RegistrySupplier<BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR = register("power_distributor",
        () -> BlockEntityType.Builder.of((pos, state) -> new PowerDistributor(TileEntityTypes.POWER_DISTRIBUTOR.get(), pos, state),
            block(Constants.BlockName.PowerDistributor)));
    public static final RegistrySupplier<BlockEntityType<Print>> PRINT = register("print",
        () -> BlockEntityType.Builder.of((pos, state) -> new Print(TileEntityTypes.PRINT.get(), pos, state),
            block(Constants.BlockName.Print)));
    public static final RegistrySupplier<BlockEntityType<Printer>> PRINTER = register("printer",
        () -> BlockEntityType.Builder.of((pos, state) -> new Printer(TileEntityTypes.PRINTER.get(), pos, state),
            block(Constants.BlockName.Printer)));
    public static final RegistrySupplier<BlockEntityType<Rack>> RACK = register("rack",
        () -> BlockEntityType.Builder.of((pos, state) -> new Rack(TileEntityTypes.RACK.get(), pos, state),
            block(Constants.BlockName.Rack)));
    public static final RegistrySupplier<BlockEntityType<Raid>> RAID = register("raid",
        () -> BlockEntityType.Builder.of((pos, state) -> new Raid(TileEntityTypes.RAID.get(), pos, state),
            block(Constants.BlockName.Raid)));
    public static final RegistrySupplier<BlockEntityType<Redstone>> REDSTONE_IO = register("redstone_io",
        () -> BlockEntityType.Builder.of((pos, state) -> new Redstone(TileEntityTypes.REDSTONE_IO.get(), pos, state),
            block(Constants.BlockName.Redstone)));
    public static final RegistrySupplier<BlockEntityType<Relay>> RELAY = register("relay",
        () -> BlockEntityType.Builder.of((pos, state) -> new Relay(TileEntityTypes.RELAY.get(), pos, state),
            block(Constants.BlockName.Relay)));
    // We use the RobotProxy instead of Robot here because those are the ones actually found in the world.
    // Beware of BlockEntityType.create for this as it will construct a new, empty robot.
    public static final RegistrySupplier<BlockEntityType<RobotProxy>> ROBOT = register("robot",
        () -> BlockEntityType.Builder.of((pos, state) -> new RobotProxy(TileEntityTypes.ROBOT.get(), pos, state),
            block(Constants.BlockName.Robot)));
    public static final RegistrySupplier<BlockEntityType<Screen>> SCREEN = register("screen",
        () -> BlockEntityType.Builder.of((pos, state) -> new Screen(TileEntityTypes.SCREEN.get(), pos, state),
            block(Constants.BlockName.ScreenTier1),
            block(Constants.BlockName.ScreenTier2),
            block(Constants.BlockName.ScreenTier3)));
    public static final RegistrySupplier<BlockEntityType<Transposer>> TRANSPOSER = register("transposer",
        () -> BlockEntityType.Builder.of((pos, state) -> new Transposer(TileEntityTypes.TRANSPOSER.get(), pos, state),
            block(Constants.BlockName.Transposer)));
    public static final RegistrySupplier<BlockEntityType<Waypoint>> WAYPOINT = register("waypoint",
        () -> BlockEntityType.Builder.of((pos, state) -> new Waypoint(TileEntityTypes.WAYPOINT.get(), pos, state),
            block(Constants.BlockName.Waypoint)));

    private static Block block(String name) {
        return Blocks.get(name).get();
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> RegistrySupplier<BlockEntityType<T>> register(String name, Supplier<BlockEntityType.Builder<T>> builder) {
        // Data fixer type is null, as in 1.16.
        return TILE_ENTITY_TYPES.register(name, () -> builder.get().build(null));
    }

    /** Registers the block entity types; called from {@code common.Proxy.preInit()}. */
    public static void init() {
        TILE_ENTITY_TYPES.register();
    }

    /** Alias of {@link #init()}. */
    public static void register() {
        init();
    }

    private TileEntityTypes() {
        throw new Error();
    }
}
