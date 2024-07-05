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
        register(e.getRegistry(), "adapter", BlockEntityType.Builder.of(() -> new Adapter(ADAPTER),
            Items.get(Constants.BlockName$.MODULE$.Adapter()).block()));
        BlockEntityType<Adapter>.BlockEntityType.Builder.of(ADAPTER, Items.get(Constants.BlockName$.MODULE$.Adapter()).block());
        register(e.getRegistry(), "assembler", BlockEntityType.Builder.of(() -> new Assembler(ASSEMBLER),
            Items.get(Constants.BlockName$.MODULE$.Assembler()).block()));
        register(e.getRegistry(), "cable", BlockEntityType.Builder.of(() -> new Cable(CABLE),
            Items.get(Constants.BlockName$.MODULE$.Cable()).block()));
        register(e.getRegistry(), "capacitor", BlockEntityType.Builder.of(() -> new Capacitor(CAPACITOR),
            Items.get(Constants.BlockName$.MODULE$.Capacitor()).block()));
        register(e.getRegistry(), "carpeted_capacitor", BlockEntityType.Builder.of(() -> new CarpetedCapacitor(CARPETED_CAPACITOR),
            Items.get(Constants.BlockName$.MODULE$.CarpetedCapacitor()).block()));
        register(e.getRegistry(), "case", BlockEntityType.Builder.of(() -> new Case(CASE),
            Items.get(Constants.BlockName$.MODULE$.CaseCreative()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier2()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier3()).block()));
        register(e.getRegistry(), "charger", BlockEntityType.Builder.of(() -> new Charger(CHARGER),
            Items.get(Constants.BlockName$.MODULE$.Charger()).block()));
        register(e.getRegistry(), "disassembler", BlockEntityType.Builder.of(() -> new Disassembler(DISASSEMBLER),
            Items.get(Constants.BlockName$.MODULE$.Disassembler()).block()));
        register(e.getRegistry(), "disk_drive", BlockEntityType.Builder.of(() -> new DiskDrive(DISK_DRIVE),
            Items.get(Constants.BlockName$.MODULE$.DiskDrive()).block()));
        register(e.getRegistry(), "geolyzer", BlockEntityType.Builder.of(() -> new Geolyzer(GEOLYZER),
            Items.get(Constants.BlockName$.MODULE$.Geolyzer()).block()));
        register(e.getRegistry(), "hologram", BlockEntityType.Builder.of(() -> new Hologram(HOLOGRAM),
            Items.get(Constants.BlockName$.MODULE$.HologramTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.HologramTier2()).block()));
        register(e.getRegistry(), "keyboard", BlockEntityType.Builder.of(() -> new Keyboard(KEYBOARD),
            Items.get(Constants.BlockName$.MODULE$.Keyboard()).block()));
        register(e.getRegistry(), "microcontroller", BlockEntityType.Builder.of(() -> new Microcontroller(MICROCONTROLLER),
            Items.get(Constants.BlockName$.MODULE$.Microcontroller()).block()));
        register(e.getRegistry(), "motion_sensor", BlockEntityType.Builder.of(() -> new MotionSensor(MOTION_SENSOR),
            Items.get(Constants.BlockName$.MODULE$.MotionSensor()).block()));
        register(e.getRegistry(), "net_splitter", BlockEntityType.Builder.of(() -> new NetSplitter(NET_SPLITTER),
            Items.get(Constants.BlockName$.MODULE$.NetSplitter()).block()));
        register(e.getRegistry(), "power_converter", BlockEntityType.Builder.of(() -> new PowerConverter(POWER_CONVERTER),
            Items.get(Constants.BlockName$.MODULE$.PowerConverter()).block()));
        register(e.getRegistry(), "power_distributor", BlockEntityType.Builder.of(() -> new PowerDistributor(POWER_DISTRIBUTOR),
            Items.get(Constants.BlockName$.MODULE$.PowerDistributor()).block()));
        register(e.getRegistry(), "print", BlockEntityType.Builder.of(() -> new Print(PRINT),
            Items.get(Constants.BlockName$.MODULE$.Print()).block()));
        register(e.getRegistry(), "printer", BlockEntityType.Builder.of(() -> new Printer(PRINTER),
            Items.get(Constants.BlockName$.MODULE$.Printer()).block()));
        register(e.getRegistry(), "rack", BlockEntityType.Builder.of(() -> new Rack(RACK),
            Items.get(Constants.BlockName$.MODULE$.Rack()).block()));
        register(e.getRegistry(), "raid", BlockEntityType.Builder.of(() -> new Raid(RAID),
            Items.get(Constants.BlockName$.MODULE$.Raid()).block()));
        register(e.getRegistry(), "redstone_io", BlockEntityType.Builder.of(() -> new Redstone(REDSTONE_IO),
            Items.get(Constants.BlockName$.MODULE$.Redstone()).block()));
        register(e.getRegistry(), "relay", BlockEntityType.Builder.of(() -> new Relay(RELAY),
            Items.get(Constants.BlockName$.MODULE$.Relay()).block()));
        register(e.getRegistry(), "robot", BlockEntityType.Builder.of(() -> new RobotProxy(ROBOT),
            Items.get(Constants.BlockName$.MODULE$.Robot()).block()));
        register(e.getRegistry(), "screen", BlockEntityType.Builder.of(() -> new Screen(SCREEN),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier2()).block(),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier3()).block()));
        register(e.getRegistry(), "transposer", BlockEntityType.Builder.of(() -> new Transposer(TRANSPOSER),
            Items.get(Constants.BlockName$.MODULE$.Transposer()).block()));
        register(e.getRegistry(), "waypoint", BlockEntityType.Builder.of(() -> new Waypoint(WAYPOINT),
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
