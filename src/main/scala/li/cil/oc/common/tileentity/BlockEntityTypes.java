package li.cil.oc.common.tileentity;

import li.cil.oc.OpenComputers;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder("opencomputers")
public final class BlockEntityTypes {
    public static final BlockEntityType<Adapter> ADAPTER = null;
    public static final BlockEntityType<Assembler> ASSEMBLER = null;
    public static final BlockEntityType<Cable> CABLE = null;
    public static final BlockEntityType<Capacitor> CAPACITOR = null;
    public static final BlockEntityType<CarpetedCapacitor> CARPETED_CAPACITOR = null;
    public static final BlockEntityType<Case> CASE = null;
    public static final BlockEntityType<Charger> CHARGER = null;
    public static final BlockEntityType<Disassembler> DISASSEMBLER = null;
    public static final BlockEntityType<DiskDrive> DISK_DRIVE = null;
    public static final BlockEntityType<Geolyzer> GEOLYZER = null;
    public static final BlockEntityType<Hologram> HOLOGRAM = null;
    public static final BlockEntityType<Keyboard> KEYBOARD = null;
    public static final BlockEntityType<Microcontroller> MICROCONTROLLER = null;
    public static final BlockEntityType<MotionSensor> MOTION_SENSOR = null;
    public static final BlockEntityType<NetSplitter> NET_SPLITTER = null;
    public static final BlockEntityType<PowerConverter> POWER_CONVERTER = null;
    public static final BlockEntityType<PowerDistributor> POWER_DISTRIBUTOR = null;
    public static final BlockEntityType<Print> PRINT = null;
    public static final BlockEntityType<Printer> PRINTER = null;
    public static final BlockEntityType<Rack> RACK = null;
    public static final BlockEntityType<Raid> RAID = null;
    public static final BlockEntityType<Redstone> REDSTONE_IO = null;
    public static final BlockEntityType<Relay> RELAY = null;
    // We use the RobotProxy instead of Robot here because those are the ones actually found in the world.
    // Beware of BlockEntityType.create for this as it will construct a new, empty robot.
    public static final BlockEntityType<RobotProxy> ROBOT = null;
    public static final BlockEntityType<Screen> SCREEN = null;
    public static final BlockEntityType<Transposer> TRANSPOSER = null;
    public static final BlockEntityType<Waypoint> WAYPOINT = null;

    @SubscribeEvent
    public static void registerTileEntities(RegistryEvent.Register<BlockEntityType<?>> e) {
        register(e.getRegistry(), "adapter", BlockEntityType.Builder.of((pos, state) -> new Adapter(ADAPTER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Adapter()).block()));
//        BlockEntityType<Adapter>.BlockEntityType.Builder.of(ADAPTER, Items.get(Constants.BlockName$.MODULE$.Adapter()).block());
        register(e.getRegistry(), "assembler", BlockEntityType.Builder.of((pos, state) -> new Assembler(ASSEMBLER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Assembler()).block()));
        register(e.getRegistry(), "cable", BlockEntityType.Builder.of((pos, state) -> new Cable(CABLE, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Cable()).block()));
        register(e.getRegistry(), "capacitor", BlockEntityType.Builder.of((pos, state) -> new Capacitor(CAPACITOR, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Capacitor()).block()));
        register(e.getRegistry(), "carpeted_capacitor", BlockEntityType.Builder.of((pos, state) -> new CarpetedCapacitor(CARPETED_CAPACITOR, pos, state),
            Items.get(Constants.BlockName$.MODULE$.CarpetedCapacitor()).block()));
        register(e.getRegistry(), "case", BlockEntityType.Builder.of((pos, state) -> new Case(CASE, pos, state),
            Items.get(Constants.BlockName$.MODULE$.CaseCreative()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier2()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier3()).block()));
        register(e.getRegistry(), "charger", BlockEntityType.Builder.of((pos, state) -> new Charger(CHARGER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Charger()).block()));
        register(e.getRegistry(), "disassembler", BlockEntityType.Builder.of((pos, state) -> new Disassembler(DISASSEMBLER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Disassembler()).block()));
        register(e.getRegistry(), "disk_drive", BlockEntityType.Builder.of((pos, state) -> new DiskDrive(DISK_DRIVE, pos, state),
            Items.get(Constants.BlockName$.MODULE$.DiskDrive()).block()));
        register(e.getRegistry(), "geolyzer", BlockEntityType.Builder.of((pos, state) -> new Geolyzer(GEOLYZER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Geolyzer()).block()));
        register(e.getRegistry(), "hologram", BlockEntityType.Builder.of((pos, state) -> new Hologram(HOLOGRAM, pos, state),
            Items.get(Constants.BlockName$.MODULE$.HologramTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.HologramTier2()).block()));
        register(e.getRegistry(), "keyboard", BlockEntityType.Builder.of((pos, state) -> new Keyboard(KEYBOARD, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Keyboard()).block()));
        register(e.getRegistry(), "microcontroller", BlockEntityType.Builder.of((pos, state) -> new Microcontroller(MICROCONTROLLER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Microcontroller()).block()));
        register(e.getRegistry(), "motion_sensor", BlockEntityType.Builder.of((pos, state) -> new MotionSensor(MOTION_SENSOR, pos, state),
            Items.get(Constants.BlockName$.MODULE$.MotionSensor()).block()));
        register(e.getRegistry(), "net_splitter", BlockEntityType.Builder.of((pos, state) -> new NetSplitter(NET_SPLITTER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.NetSplitter()).block()));
        register(e.getRegistry(), "power_converter", BlockEntityType.Builder.of((pos, state) -> new PowerConverter(POWER_CONVERTER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.PowerConverter()).block()));
        register(e.getRegistry(), "power_distributor", BlockEntityType.Builder.of((pos, state) -> new PowerDistributor(POWER_DISTRIBUTOR, pos, state),
            Items.get(Constants.BlockName$.MODULE$.PowerDistributor()).block()));
        register(e.getRegistry(), "print", BlockEntityType.Builder.of((pos, state) -> new Print(PRINT, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Print()).block()));
        register(e.getRegistry(), "printer", BlockEntityType.Builder.of((pos, state) -> new Printer(PRINTER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Printer()).block()));
        register(e.getRegistry(), "rack", BlockEntityType.Builder.of((pos, state) -> new Rack(RACK, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Rack()).block()));
        register(e.getRegistry(), "raid", BlockEntityType.Builder.of((pos, state) -> new Raid(RAID, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Raid()).block()));
        register(e.getRegistry(), "redstone_io", BlockEntityType.Builder.of((pos, state) -> new Redstone(REDSTONE_IO, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Redstone()).block()));
        register(e.getRegistry(), "relay", BlockEntityType.Builder.of((pos, state) -> new Relay(RELAY, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Relay()).block()));
        register(e.getRegistry(), "robot", BlockEntityType.Builder.of((pos, state) -> new RobotProxy(ROBOT, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Robot()).block()));
        register(e.getRegistry(), "screen", BlockEntityType.Builder.of((pos, state) -> new Screen(SCREEN, pos, state),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier2()).block(),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier3()).block()));
        register(e.getRegistry(), "transposer", BlockEntityType.Builder.of((pos, state) -> new Transposer(TRANSPOSER, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Transposer()).block()));
        register(e.getRegistry(), "waypoint", BlockEntityType.Builder.of((pos, state) -> new Waypoint(WAYPOINT, pos, state),
            Items.get(Constants.BlockName$.MODULE$.Waypoint()).block()));
    }

    private static void register(IForgeRegistry<BlockEntityType<?>> registry, String name, BlockEntityType.Builder<?> builder) {
        BlockEntityType<?> type = builder.build(null);
        type.setRegistryName(new ResourceLocation(OpenComputers.ID(), name));
        registry.register(type);
    }

    private BlockEntityTypes() {
        throw new Error();
    }
}
