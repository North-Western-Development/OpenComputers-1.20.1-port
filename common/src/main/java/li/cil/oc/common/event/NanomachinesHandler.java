package li.cil.oc.common.event;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;

public final class NanomachinesHandler {
    private NanomachinesHandler() {
    }

    public static final class Common {
        private Common() {
        }

        public static void register() {
            PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd) -> onPlayerRespawn(player));
            TickEvent.PLAYER_PRE.register(Common::onLivingUpdate);
            PlayerEvent.PLAYER_QUIT.register(Common::onPlayerDisconnect);
            // Player file save/load is forwarded from li.cil.oc.common.mixin.PlayerDataStorageMixin.
        }

        public static void onPlayerRespawn(Player player) {
            Controller controller = Nanomachines.getController(player);
            if (controller != null) controller.changeBuffer(-controller.getLocalBuffer());
        }

        public static void onLivingUpdate(Player player) {
            if (Nanomachines.getController(player) instanceof ControllerImpl controller) {
                if (controller.player == player) {
                    controller.update();
                }
                else {
                    // Player entity instance changed (e.g. respawn), recreate the controller.
                    CompoundTag nbt = new CompoundTag();
                    controller.saveData(nbt);
                    Nanomachines.uninstallController(controller.player);
                    if (Nanomachines.installController(player) instanceof ControllerImpl newController) {
                        newController.loadData(nbt);
                        newController.reset();
                    }
                }
            }
        }

        /**
         * Called after the player's data file was written to {@code playerDir}.
         */
        public static void onPlayerSave(Player player, File playerDir) {
            File file = new File(playerDir, player.getStringUUID() + ".ocnm");
            if (Nanomachines.getController(player) instanceof ControllerImpl controller) {
                try {
                    CompoundTag nbt = new CompoundTag();
                    controller.saveData(nbt);
                    NbtIo.writeCompressed(nbt, file);
                }
                catch (Throwable t) {
                    OpenComputers.log.warn("Error saving nanomachine state.", t);
                }
            }
        }

        /**
         * Called after the player's data file was read from {@code playerDir}.
         */
        public static void onPlayerLoad(Player player, File playerDir) {
            File file = new File(playerDir, player.getStringUUID() + ".ocnm");
            if (file.exists() && Nanomachines.getController(player) instanceof ControllerImpl controller) {
                try {
                    controller.loadData(NbtIo.readCompressed(file));
                }
                catch (Throwable t) {
                    OpenComputers.log.warn("Error loading nanomachine state.", t);
                }
            }
        }

        public static void onPlayerDisconnect(Player player) {
            if (Nanomachines.getController(player) instanceof ControllerImpl) {
                // Wait a tick because saving is done after this event.
                EventHandler.scheduleServer(() -> Nanomachines.uninstallController(player));
            }
        }
    }
}
