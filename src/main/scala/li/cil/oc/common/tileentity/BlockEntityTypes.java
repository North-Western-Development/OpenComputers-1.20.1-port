package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BlockEntityTypes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OpenComputers.ID());

    public static final RegistryObject<BlockEntityType<Adapter>> ADAPTER =
        BLOCK_ENTITY_TYPES.register("adapter", () ->
            BlockEntityType.Builder.of((pos, state) -> new Adapter(BlockEntityTypes.ADAPTER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Adapter()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Assembler>> ASSEMBLER =
        BLOCK_ENTITY_TYPES.register("assembler", () ->
            BlockEntityType.Builder.of((pos, state) -> new Assembler(BlockEntityTypes.ASSEMBLER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Assembler()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Cable>> CABLE =
        BLOCK_ENTITY_TYPES.register("cable", () ->
            BlockEntityType.Builder.of((pos, state) -> new Cable(BlockEntityTypes.CABLE.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Cable()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Capacitor>> CAPACITOR =
        BLOCK_ENTITY_TYPES.register("capacitor", () ->
            BlockEntityType.Builder.of((pos, state) -> new Capacitor(BlockEntityTypes.CAPACITOR.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Capacitor()).block()).build(null));

    public static final RegistryObject<BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR =
        BLOCK_ENTITY_TYPES.register("carpeted_capacitor", () ->
            BlockEntityType.Builder.of((pos, state) -> new CarpetedCapacitor(BlockEntityTypes.CARPETED_CAPACITOR.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.CarpetedCapacitor()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Case>> CASE =
        BLOCK_ENTITY_TYPES.register("case", () ->
            BlockEntityType.Builder.of((pos, state) -> new Case(BlockEntityTypes.CASE.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.CaseCreative()).block(),
                Items.get(Constants.BlockName$.MODULE$.CaseTier1()).block(),
                Items.get(Constants.BlockName$.MODULE$.CaseTier2()).block(),
                Items.get(Constants.BlockName$.MODULE$.CaseTier3()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Charger>> CHARGER =
        BLOCK_ENTITY_TYPES.register("charger", () ->
            BlockEntityType.Builder.of((pos, state) -> new Charger(BlockEntityTypes.CHARGER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Charger()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Disassembler>> DISASSEMBLER =
        BLOCK_ENTITY_TYPES.register("disassembler", () ->
            BlockEntityType.Builder.of((pos, state) -> new Disassembler(BlockEntityTypes.DISASSEMBLER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Disassembler()).block()).build(null));

    public static final RegistryObject<BlockEntityType<DiskDrive>> DISK_DRIVE =
        BLOCK_ENTITY_TYPES.register("disk_drive", () ->
            BlockEntityType.Builder.of((pos, state) -> new DiskDrive(BlockEntityTypes.DISK_DRIVE.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.DiskDrive()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Geolyzer>> GEOLYZER =
        BLOCK_ENTITY_TYPES.register("geolyzer", () ->
            BlockEntityType.Builder.of((pos, state) -> new Geolyzer(BlockEntityTypes.GEOLYZER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Geolyzer()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Hologram>> HOLOGRAM =
        BLOCK_ENTITY_TYPES.register("hologram", () ->
            BlockEntityType.Builder.of((pos, state) -> new Hologram(BlockEntityTypes.HOLOGRAM.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.HologramTier1()).block(),
                Items.get(Constants.BlockName$.MODULE$.HologramTier2()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Keyboard>> KEYBOARD =
        BLOCK_ENTITY_TYPES.register("keyboard", () ->
            BlockEntityType.Builder.of((pos, state) -> new Keyboard(BlockEntityTypes.KEYBOARD.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Keyboard()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Microcontroller>> MICROCONTROLLER =
        BLOCK_ENTITY_TYPES.register("microcontroller", () ->
            BlockEntityType.Builder.of((pos, state) -> new Microcontroller(BlockEntityTypes.MICROCONTROLLER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Microcontroller()).block()).build(null));

    public static final RegistryObject<BlockEntityType<MotionSensor>> MOTION_SENSOR =
        BLOCK_ENTITY_TYPES.register("motion_sensor", () ->
            BlockEntityType.Builder.of((pos, state) -> new MotionSensor(BlockEntityTypes.MOTION_SENSOR.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.MotionSensor()).block()).build(null));

    public static final RegistryObject<BlockEntityType<NetSplitter>> NET_SPLITTER =
        BLOCK_ENTITY_TYPES.register("net_splitter", () ->
            BlockEntityType.Builder.of((pos, state) -> new NetSplitter(BlockEntityTypes.NET_SPLITTER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.NetSplitter()).block()).build(null));

    public static final RegistryObject<BlockEntityType<PowerConverter>> POWER_CONVERTER =
        BLOCK_ENTITY_TYPES.register("power_converter", () ->
            BlockEntityType.Builder.of((pos, state) -> new PowerConverter(BlockEntityTypes.POWER_CONVERTER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.PowerConverter()).block()).build(null));

    public static final RegistryObject<BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR =
        BLOCK_ENTITY_TYPES.register("power_distributor", () ->
            BlockEntityType.Builder.of((pos, state) -> new PowerDistributor(BlockEntityTypes.POWER_DISTRIBUTOR.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.PowerDistributor()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Print>> PRINT =
        BLOCK_ENTITY_TYPES.register("print", () ->
            BlockEntityType.Builder.of((pos, state) -> new Print(BlockEntityTypes.PRINT.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Print()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Printer>> PRINTER =
        BLOCK_ENTITY_TYPES.register("printer", () ->
            BlockEntityType.Builder.of((pos, state) -> new Printer(BlockEntityTypes.PRINTER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Printer()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Rack>> RACK =
        BLOCK_ENTITY_TYPES.register("rack", () ->
            BlockEntityType.Builder.of((pos, state) -> new Rack(BlockEntityTypes.RACK.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Rack()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Raid>> RAID =
        BLOCK_ENTITY_TYPES.register("raid", () ->
            BlockEntityType.Builder.of((pos, state) -> new Raid(BlockEntityTypes.RAID.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Raid()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Redstone>> REDSTONE_IO =
        BLOCK_ENTITY_TYPES.register("redstone_io", () ->
            BlockEntityType.Builder.of((pos, state) -> new Redstone(BlockEntityTypes.REDSTONE_IO.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Redstone()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Relay>> RELAY =
        BLOCK_ENTITY_TYPES.register("relay", () ->
            BlockEntityType.Builder.of((pos, state) -> new Relay(BlockEntityTypes.RELAY.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Relay()).block()).build(null));

    public static final RegistryObject<BlockEntityType<RobotProxy>> ROBOT =
        BLOCK_ENTITY_TYPES.register("robot", () ->
            BlockEntityType.Builder.of((pos, state) -> new RobotProxy(BlockEntityTypes.ROBOT.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Robot()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Screen>> SCREEN =
        BLOCK_ENTITY_TYPES.register("screen", () ->
            BlockEntityType.Builder.of((pos, state) -> new Screen(BlockEntityTypes.SCREEN.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).block(),
                Items.get(Constants.BlockName$.MODULE$.ScreenTier2()).block(),
                Items.get(Constants.BlockName$.MODULE$.ScreenTier3()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Transposer>> TRANSPOSER =
        BLOCK_ENTITY_TYPES.register("transposer", () ->
            BlockEntityType.Builder.of((pos, state) -> new Transposer(BlockEntityTypes.TRANSPOSER.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Transposer()).block()).build(null));

    public static final RegistryObject<BlockEntityType<Waypoint>> WAYPOINT =
        BLOCK_ENTITY_TYPES.register("waypoint", () ->
            BlockEntityType.Builder.of((pos, state) -> new Waypoint(BlockEntityTypes.WAYPOINT.get(), pos, state),
                Items.get(Constants.BlockName$.MODULE$.Waypoint()).block()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    private BlockEntityTypes() {
        throw new Error();
    }
}
