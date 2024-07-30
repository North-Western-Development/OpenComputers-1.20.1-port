package li.cil.oc.client.gui;

import li.cil.oc.common.container.ContainerTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class GuiTypes {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent e) {
        // ScreenManager.register is not thread-safe.
        e.enqueueWork(() -> {
            MenuScreens.register(ContainerTypes.ADAPTER, Adapter::new);
            MenuScreens.register(ContainerTypes.ASSEMBLER, Assembler::new);
            MenuScreens.register(ContainerTypes.CASE, Case::new);
            MenuScreens.register(ContainerTypes.CHARGER, Charger::new);
            MenuScreens.register(ContainerTypes.DATABASE, Database::new);
            MenuScreens.register(ContainerTypes.DISASSEMBLER, Disassembler::new);
            MenuScreens.register(ContainerTypes.DISK_DRIVE, DiskDrive::new);
            MenuScreens.register(ContainerTypes.DRONE, Drone::new);
            MenuScreens.register(ContainerTypes.PRINTER, Printer::new);
            MenuScreens.register(ContainerTypes.RACK, Rack::new);
            MenuScreens.register(ContainerTypes.RAID, Raid::new);
            MenuScreens.register(ContainerTypes.RELAY, Relay::new);
            MenuScreens.register(ContainerTypes.ROBOT, Robot::new);
            MenuScreens.register(ContainerTypes.SERVER, Server::new);
            MenuScreens.register(ContainerTypes.TABLET, Tablet::new);
        });
    }

    private GuiTypes() {
        throw new Error();
    }
}
