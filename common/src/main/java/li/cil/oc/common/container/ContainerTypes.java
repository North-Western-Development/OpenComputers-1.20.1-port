package li.cil.oc.common.container;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ContainerTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(OpenComputers.ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<Adapter>> ADAPTER = register("adapter",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Adapter(ContainerTypes.ADAPTER.get(), id, plr, new SimpleContainer(1))));
    public static final RegistrySupplier<MenuType<Assembler>> ASSEMBLER = register("assembler",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Assembler(ContainerTypes.ASSEMBLER.get(), id, plr, new SimpleContainer(22))));
    public static final RegistrySupplier<MenuType<Case>> CASE = register("case",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> {
            int invSize = buff.readVarInt();
            int tier = buff.readVarInt();
            return new Case(ContainerTypes.CASE.get(), id, plr, new SimpleContainer(invSize), tier);
        }));
    public static final RegistrySupplier<MenuType<Charger>> CHARGER = register("charger",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Charger(ContainerTypes.CHARGER.get(), id, plr, new SimpleContainer(1))));
    public static final RegistrySupplier<MenuType<Database>> DATABASE = register("database",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> {
            ItemStack containerStack = buff.readItem();
            int invSize = buff.readVarInt();
            int tier = buff.readVarInt();
            return new Database(ContainerTypes.DATABASE.get(), id, plr, containerStack, new SimpleContainer(invSize), tier);
        }));
    public static final RegistrySupplier<MenuType<Disassembler>> DISASSEMBLER = register("disassembler",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Disassembler(ContainerTypes.DISASSEMBLER.get(), id, plr, new SimpleContainer(1))));
    public static final RegistrySupplier<MenuType<DiskDrive>> DISK_DRIVE = register("disk_drive",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new DiskDrive(ContainerTypes.DISK_DRIVE.get(), id, plr, new SimpleContainer(1))));
    public static final RegistrySupplier<MenuType<Drone>> DRONE = register("drone",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> {
            int invSize = buff.readVarInt();
            return new Drone(ContainerTypes.DRONE.get(), id, plr, new SimpleContainer(8), invSize);
        }));
    public static final RegistrySupplier<MenuType<Printer>> PRINTER = register("printer",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Printer(ContainerTypes.PRINTER.get(), id, plr, new SimpleContainer(3))));
    public static final RegistrySupplier<MenuType<Rack>> RACK = register("rack",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Rack(ContainerTypes.RACK.get(), id, plr, new SimpleContainer(4))));
    public static final RegistrySupplier<MenuType<Raid>> RAID = register("raid",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Raid(ContainerTypes.RAID.get(), id, plr, new SimpleContainer(3))));
    public static final RegistrySupplier<MenuType<Relay>> RELAY = register("relay",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> new Relay(ContainerTypes.RELAY.get(), id, plr, new SimpleContainer(4))));
    public static final RegistrySupplier<MenuType<Robot>> ROBOT = register("robot",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> {
            RobotInfo info = RobotInfo.readRobotInfo(buff);
            return new Robot(ContainerTypes.ROBOT.get(), id, plr, new SimpleContainer(100), info);
        }));
    public static final RegistrySupplier<MenuType<Server>> SERVER = register("server",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> {
            ItemStack containerStack = buff.readItem();
            int invSize = buff.readVarInt();
            int tier = buff.readVarInt();
            int rackSlot = buff.readVarInt() - 1;
            return new Server(ContainerTypes.SERVER.get(), id, plr, containerStack, new SimpleContainer(invSize), tier, rackSlot);
        }));
    public static final RegistrySupplier<MenuType<Tablet>> TABLET = register("tablet",
        () -> MenuRegistry.ofExtended((id, plr, buff) -> {
            ItemStack containerStack = buff.readItem();
            int invSize = buff.readVarInt();
            String slot1 = buff.readUtf(32);
            int tier1 = buff.readVarInt();
            return new Tablet(ContainerTypes.TABLET.get(), id, plr, containerStack, new SimpleContainer(invSize), slot1, tier1);
        }));

    private static <T extends AbstractContainerMenu> RegistrySupplier<MenuType<T>> register(String name, Supplier<MenuType<T>> factory) {
        return MENUS.register(name, factory);
    }

    public static void init() {
        MENUS.register();
    }

    // ----------------------------------------------------------------------- //

    private static void open(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        MenuRegistry.openExtendedMenu(player, provider, extraData);
    }

    private static void open(ServerPlayer player, MenuProvider provider) {
        open(player, provider, buff -> {
        });
    }

    public static void openAdapterGui(ServerPlayer player, li.cil.oc.common.tileentity.Adapter adapter) {
        open(player, adapter);
    }

    public static void openAssemblerGui(ServerPlayer player, li.cil.oc.common.tileentity.Assembler assembler) {
        open(player, assembler);
    }

    public static void openCaseGui(ServerPlayer player, li.cil.oc.common.tileentity.Case computer) {
        open(player, computer, buff -> {
            buff.writeVarInt(computer.getContainerSize());
            buff.writeVarInt(computer.tier);
        });
    }

    public static void openChargerGui(ServerPlayer player, li.cil.oc.common.tileentity.Charger charger) {
        open(player, charger);
    }

    public static void openDatabaseGui(ServerPlayer player, li.cil.oc.common.inventory.DatabaseInventory database) {
        open(player, database, buff -> {
            buff.writeItem(database.container());
            buff.writeVarInt(database.getContainerSize());
            buff.writeVarInt(database.tier());
        });
    }

    public static void openDisassemblerGui(ServerPlayer player, li.cil.oc.common.tileentity.Disassembler disassembler) {
        open(player, disassembler);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.common.tileentity.DiskDrive diskDrive) {
        open(player, diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.server.component.DiskDriveMountable diskDrive) {
        open(player, diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.common.inventory.DiskDriveMountableInventory diskDrive) {
        open(player, diskDrive);
    }

    public static void openDroneGui(ServerPlayer player, li.cil.oc.common.entity.Drone drone) {
        open(player, drone.containerProvider, buff -> buff.writeVarInt(drone.mainInventory().getContainerSize()));
    }

    public static void openPrinterGui(ServerPlayer player, li.cil.oc.common.tileentity.Printer printer) {
        open(player, printer);
    }

    public static void openRackGui(ServerPlayer player, li.cil.oc.common.tileentity.Rack rack) {
        open(player, rack);
    }

    public static void openRaidGui(ServerPlayer player, li.cil.oc.common.tileentity.Raid raid) {
        open(player, raid);
    }

    public static void openRelayGui(ServerPlayer player, li.cil.oc.common.tileentity.Relay relay) {
        open(player, relay);
    }

    public static void openRobotGui(ServerPlayer player, li.cil.oc.common.tileentity.Robot robot) {
        open(player, robot, buff -> RobotInfo.writeRobotInfo(buff, new RobotInfo(robot)));
    }

    public static void openServerGui(ServerPlayer player, li.cil.oc.common.inventory.ServerInventory server, int rackSlot) {
        open(player, server, buff -> {
            buff.writeItem(server.container());
            buff.writeVarInt(server.getContainerSize());
            buff.writeVarInt(server.tier());
            buff.writeVarInt(rackSlot + 1);
        });
    }

    public static void openTabletGui(ServerPlayer player, li.cil.oc.common.item.TabletWrapper tablet) {
        open(player, tablet, buff -> {
            buff.writeItem(tablet.stack);
            buff.writeVarInt(tablet.getContainerSize());
            buff.writeUtf(tablet.containerSlotType(), 32);
            buff.writeVarInt(tablet.containerSlotTier());
        });
    }

    private ContainerTypes() {
        throw new Error();
    }
}
