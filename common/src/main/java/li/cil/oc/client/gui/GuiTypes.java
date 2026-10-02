package li.cil.oc.client.gui;

import dev.architectury.registry.menu.MenuRegistry;
import li.cil.oc.common.container.ContainerTypes;

public final class GuiTypes {
    /**
     * Registers the screen factories for OC's menu types. Must be called once the
     * menu type registry is populated (client setup).
     */
    public static void register() {
        MenuRegistry.registerScreenFactory(ContainerTypes.ADAPTER.get(), Adapter::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.ASSEMBLER.get(), Assembler::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.CASE.get(), Case::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.CHARGER.get(), Charger::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.DATABASE.get(), Database::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.DISASSEMBLER.get(), Disassembler::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.DISK_DRIVE.get(), DiskDrive::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.DRONE.get(), Drone::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.PRINTER.get(), Printer::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.RACK.get(), Rack::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.RAID.get(), Raid::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.RELAY.get(), Relay::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.ROBOT.get(), Robot::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.SERVER.get(), Server::new);
        MenuRegistry.registerScreenFactory(ContainerTypes.TABLET.get(), Tablet::new);
    }

    private GuiTypes() {
        throw new Error();
    }
}
