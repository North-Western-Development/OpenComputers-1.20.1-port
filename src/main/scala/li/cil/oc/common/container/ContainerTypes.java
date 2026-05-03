package li.cil.oc.common.container;

import li.cil.oc.OpenComputers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

public final class ContainerTypes {
    public static MenuType<Adapter> ADAPTER;
    public static MenuType<Assembler> ASSEMBLER;
    public static MenuType<Case> CASE;
    public static MenuType<Charger> CHARGER;
    public static MenuType<Database> DATABASE;
    public static MenuType<Disassembler> DISASSEMBLER;
    public static MenuType<DiskDrive> DISK_DRIVE;
    public static MenuType<Drone> DRONE;
    public static MenuType<Printer> PRINTER;
    public static MenuType<Rack> RACK;
    public static MenuType<Raid> RAID;
    public static MenuType<Relay> RELAY;
    public static MenuType<Robot> ROBOT;
    public static MenuType<Server> SERVER;
    public static MenuType<Tablet> TABLET;

    @SubscribeEvent
    public static void registerContainers(RegisterEvent e) {
        e.register(ForgeRegistries.Keys.MENU_TYPES, registry -> {
            ADAPTER = register(registry, "adapter", (id, plr, buff) -> new Adapter(ADAPTER, id, plr, new SimpleContainer(1)));
            ASSEMBLER = register(registry, "assembler", (id, plr, buff) -> new Assembler(ASSEMBLER, id, plr, new SimpleContainer(22)));
            CASE = register(registry, "case", (id, plr, buff) -> {
                int invSize = buff.readVarInt();
                int tier = buff.readVarInt();
                return new Case(CASE, id, plr, new SimpleContainer(invSize), tier);
            });

            CHARGER = register(registry, "charger", (id, plr, buff) -> new Charger(CHARGER, id, plr, new SimpleContainer(1)));
            DATABASE = register(registry, "database", (id, plr, buff) -> {
                ItemStack containerStack = buff.readItem();
                int invSize = buff.readVarInt();
                int tier = buff.readVarInt();
                return new Database(DATABASE, id, plr, containerStack, new SimpleContainer(invSize), tier);
            });
            DISASSEMBLER = register(registry, "disassembler", (id, plr, buff) -> new Disassembler(DISASSEMBLER, id, plr, new SimpleContainer(1)));
            DISK_DRIVE = register(registry, "disk_drive", (id, plr, buff) -> new DiskDrive(DISK_DRIVE, id, plr, new SimpleContainer(1)));
            DRONE = register(registry, "drone", (id, plr, buff) -> {
                int invSize = buff.readVarInt();
                return new Drone(DRONE, id, plr, new SimpleContainer(8), invSize);
            });
            PRINTER = register(registry, "printer", (id, plr, buff) -> new Printer(PRINTER, id, plr, new SimpleContainer(3)));
            RACK = register(registry, "rack", (id, plr, buff) -> new Rack(RACK, id, plr, new SimpleContainer(4)));
            RAID = register(registry, "raid", (id, plr, buff) -> new Raid(RAID, id, plr, new SimpleContainer(3)));
            RELAY = register(registry, "relay", (id, plr, buff) -> new Relay(RELAY, id, plr, new SimpleContainer(4)));
            ROBOT = register(registry, "robot", (id, plr, buff) -> {
                RobotInfo info = RobotInfo$.MODULE$.readRobotInfo(buff);
                return new Robot(ROBOT, id, plr, new SimpleContainer(100), info);
            });
            SERVER = register(registry, "server", (id, plr, buff) -> {
                ItemStack containerStack = buff.readItem();
                int invSize = buff.readVarInt();
                int tier = buff.readVarInt();
                int rackSlot = buff.readVarInt() - 1;
                return new Server(SERVER, id, plr, containerStack, new SimpleContainer(invSize), tier, rackSlot);
            });
            TABLET = register(registry, "tablet", (id, plr, buff) -> {
                ItemStack containerStack = buff.readItem();
                int invSize = buff.readVarInt();
                String slot1 = buff.readUtf(32);
                int tier1 = buff.readVarInt();
                return new Tablet(TABLET, id, plr, containerStack, new SimpleContainer(invSize), slot1, tier1);
            });
        });
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> register(
            RegisterEvent.RegisterHelper<MenuType<?>> registry,
            String name,
            IContainerFactory<T> factory
    ) {
        MenuType<T> type = IForgeMenuType.create(factory);
        registry.register(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), name), type);
        return type;
    }
    
    public static void openAdapterGui(ServerPlayer player, li.cil.oc.common.tileentity.Adapter adapter) {
        NetworkHooks.openScreen(player, adapter);
    }

    public static void openAssemblerGui(ServerPlayer player, li.cil.oc.common.tileentity.Assembler assembler) {
        NetworkHooks.openScreen(player, assembler);
    }

    public static void openCaseGui(ServerPlayer player, li.cil.oc.common.tileentity.Case computer) {
        NetworkHooks.openScreen(player, computer, buff -> {
            buff.writeVarInt(computer.getContainerSize());
            buff.writeVarInt(computer.tier());
        });
    }

    public static void openChargerGui(ServerPlayer player, li.cil.oc.common.tileentity.Charger charger) {
        NetworkHooks.openScreen(player, charger);
    }

    public static void openDatabaseGui(ServerPlayer player, li.cil.oc.common.inventory.DatabaseInventory database) {
        NetworkHooks.openScreen(player, database, buff -> {
            buff.writeItem(database.container());
            buff.writeVarInt(database.getContainerSize());
            buff.writeVarInt(database.tier());
        });
    }

    public static void openDisassemblerGui(ServerPlayer player, li.cil.oc.common.tileentity.Disassembler disassembler) {
        NetworkHooks.openScreen(player, disassembler);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.common.tileentity.DiskDrive diskDrive) {
        NetworkHooks.openScreen(player, diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.server.component.DiskDriveMountable diskDrive) {
        NetworkHooks.openScreen(player, diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.common.inventory.DiskDriveMountableInventory diskDrive) {
        NetworkHooks.openScreen(player, diskDrive);
    }

    public static void openDroneGui(ServerPlayer player, li.cil.oc.common.entity.Drone drone) {
        NetworkHooks.openScreen(player, drone.containerProvider(), buff -> {
            buff.writeVarInt(drone.mainInventory().getContainerSize());
        });
    }

    public static void openPrinterGui(ServerPlayer player, li.cil.oc.common.tileentity.Printer printer) {
        NetworkHooks.openScreen(player, printer);
    }

    public static void openRackGui(ServerPlayer player, li.cil.oc.common.tileentity.Rack rack) {
        NetworkHooks.openScreen(player, rack);
    }

    public static void openRaidGui(ServerPlayer player, li.cil.oc.common.tileentity.Raid raid) {
        NetworkHooks.openScreen(player, raid);
    }

    public static void openRelayGui(ServerPlayer player, li.cil.oc.common.tileentity.Relay relay) {
        NetworkHooks.openScreen(player, relay);
    }

    public static void openRobotGui(ServerPlayer player, li.cil.oc.common.tileentity.Robot robot) {
        NetworkHooks.openScreen(player, robot, buff -> {
            RobotInfo$.MODULE$.writeRobotInfo(buff, new RobotInfo(robot));
        });
    }

    public static void openServerGui(ServerPlayer player, li.cil.oc.common.inventory.ServerInventory server, int rackSlot) {
        NetworkHooks.openScreen(player, server, buff -> {
            buff.writeItem(server.container());
            buff.writeVarInt(server.getContainerSize());
            buff.writeVarInt(server.tier());
            buff.writeVarInt(rackSlot + 1);
        });
    }

    public static void openTabletGui(ServerPlayer player, li.cil.oc.common.item.TabletWrapper tablet) {
        NetworkHooks.openScreen(player, tablet, buff -> {
            buff.writeItem(tablet.stack());
            buff.writeVarInt(tablet.getContainerSize());
            buff.writeUtf(tablet.containerSlotType(), 32);
            buff.writeVarInt(tablet.containerSlotTier());
        });
    }

    private ContainerTypes() {
        throw new Error();
    }
}
