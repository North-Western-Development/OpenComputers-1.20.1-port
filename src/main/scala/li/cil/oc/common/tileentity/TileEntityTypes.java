package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES;

public final class TileEntityTypes {
    @ObjectHolder(registryName = "minecraft:block_entity_type", value = "opencomputers")
    public static final RegistryObject<BlockEntityType<Adapter>> ADAPTER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "adapter"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Assembler>> ASSEMBLER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "assembler"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Cable>> CABLE = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "cable"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Capacitor>> CAPACITOR = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "capacitor"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<CarpetedCapacitor>> CARPETED_CAPACITOR = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "carpeted_capacitor"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Case>> CASE = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "case"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Charger>> CHARGER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "charger"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Disassembler>> DISASSEMBLER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "disassembler"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<DiskDrive>> DISK_DRIVE = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "disk_drive"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Geolyzer>> GEOLYZER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "geolyzer"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Hologram>> HOLOGRAM = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "hologram"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Keyboard>> KEYBOARD = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "keyboard"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Microcontroller>> MICROCONTROLLER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "microcontroller"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<MotionSensor>> MOTION_SENSOR = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "motion_sensor"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<NetSplitter>> NET_SPLITTER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "net_splitter"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<PowerConverter>> POWER_CONVERTER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "power_converter"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<PowerDistributor>> POWER_DISTRIBUTOR = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "power_distributor"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Print>> PRINT = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "print"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Printer>> PRINTER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "printer"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Rack>> RACK = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "rack"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Raid>> RAID = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "raid"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Redstone>> REDSTONE_IO = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "redstone_io"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Relay>> RELAY = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "relay"), BLOCK_ENTITY_TYPES);
    // We use the RobotProxy instead of Robot here because those are the ones actually found in the world.
    // Beware of BlockEntityType.create for this as it will construct a new, empty robot.
    public static final RegistryObject<BlockEntityType<RobotProxy>> ROBOT = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "robot"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Screen>> SCREEN = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "screen"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Transposer>> TRANSPOSER = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "transposer"), BLOCK_ENTITY_TYPES);
    public static final RegistryObject<BlockEntityType<Waypoint>> WAYPOINT = RegistryObject.create(new ResourceLocation(OpenComputers.ID(), "waypoint"), BLOCK_ENTITY_TYPES);

    @SubscribeEvent
    public static void registerTileEntities(RegisterEvent e) {
        register(e, "adapter", BlockEntityType.Builder.of((pos, state) -> new Adapter(ADAPTER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Adapter()).block()));
        register(e, "assembler", BlockEntityType.Builder.of((pos, state) -> new Assembler(ASSEMBLER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Assembler()).block()));
        register(e, "cable", BlockEntityType.Builder.of((pos, state) -> new Cable(CABLE.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Cable()).block()));
        register(e, "capacitor", BlockEntityType.Builder.of((pos, state) -> new Capacitor(CAPACITOR.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Capacitor()).block()));
        register(e, "carpeted_capacitor", BlockEntityType.Builder.of((pos, state) -> new CarpetedCapacitor(CARPETED_CAPACITOR.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.CarpetedCapacitor()).block()));
        register(e, "case", BlockEntityType.Builder.of((pos, state) -> new Case(CASE.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.CaseCreative()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier2()).block(),
            Items.get(Constants.BlockName$.MODULE$.CaseTier3()).block()));
        register(e, "charger", BlockEntityType.Builder.of((pos, state) -> new Charger(CHARGER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Charger()).block()));
        register(e, "disassembler", BlockEntityType.Builder.of((pos, state) -> new Disassembler(DISASSEMBLER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Disassembler()).block()));
        register(e, "disk_drive", BlockEntityType.Builder.of((pos, state) -> new DiskDrive(DISK_DRIVE.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.DiskDrive()).block()));
        register(e, "geolyzer", BlockEntityType.Builder.of((pos, state) -> new Geolyzer(GEOLYZER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Geolyzer()).block()));
        register(e, "hologram", BlockEntityType.Builder.of((pos, state) -> new Hologram(HOLOGRAM.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.HologramTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.HologramTier2()).block()));
        register(e, "keyboard", BlockEntityType.Builder.of((pos, state) -> new Keyboard(KEYBOARD.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Keyboard()).block()));
        register(e, "microcontroller", BlockEntityType.Builder.of((pos, state) -> new Microcontroller(MICROCONTROLLER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Microcontroller()).block()));
        register(e, "motion_sensor", BlockEntityType.Builder.of((pos, state) -> new MotionSensor(MOTION_SENSOR.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.MotionSensor()).block()));
        register(e, "net_splitter", BlockEntityType.Builder.of((pos, state) -> new NetSplitter(NET_SPLITTER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.NetSplitter()).block()));
        register(e, "power_converter", BlockEntityType.Builder.of((pos, state) -> new PowerConverter(POWER_CONVERTER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.PowerConverter()).block()));
        register(e, "power_distributor", BlockEntityType.Builder.of((pos, state) -> new PowerDistributor(POWER_DISTRIBUTOR.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.PowerDistributor()).block()));
        register(e, "print", BlockEntityType.Builder.of((pos, state) -> new Print(PRINT.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Print()).block()));
        register(e, "printer", BlockEntityType.Builder.of((pos, state) -> new Printer(PRINTER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Printer()).block()));
        register(e, "rack", BlockEntityType.Builder.of((pos, state) -> new Rack(RACK.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Rack()).block()));
        register(e, "raid", BlockEntityType.Builder.of((pos, state) -> new Raid(RAID.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Raid()).block()));
        register(e, "redstone_io", BlockEntityType.Builder.of((pos, state) -> new Redstone(REDSTONE_IO.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Redstone()).block()));
        register(e, "relay", BlockEntityType.Builder.of((pos, state) -> new Relay(RELAY.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Relay()).block()));
        register(e, "robot", BlockEntityType.Builder.of((pos, state) -> new RobotProxy(ROBOT.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Robot()).block()));
        register(e, "screen", BlockEntityType.Builder.of((pos, state) -> new Screen(SCREEN.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier1()).block(),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier2()).block(),
            Items.get(Constants.BlockName$.MODULE$.ScreenTier3()).block()));
        register(e, "transposer", BlockEntityType.Builder.of((pos, state) -> new Transposer(TRANSPOSER.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Transposer()).block()));
        register(e, "waypoint", BlockEntityType.Builder.of((pos, state) -> new Waypoint(WAYPOINT.get(), pos, state),
            Items.get(Constants.BlockName$.MODULE$.Waypoint()).block()));
    }

    private static void register(RegisterEvent e, String name, BlockEntityType.Builder<?> builder) {
        e.register(Registries.BLOCK_ENTITY_TYPE, new ResourceLocation(OpenComputers.ID(), name), () -> builder.build(null));
    }

    private TileEntityTypes() {
        throw new Error();
    }
}
