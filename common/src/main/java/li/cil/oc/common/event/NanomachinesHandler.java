package li.cil.oc.common.event;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.client.Textures;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;

public final class NanomachinesHandler {
    private NanomachinesHandler() {
    }

    /**
     * Client side HUD rendering. {@link #register()} must only be called on the physical client.
     */
    public static final class Client {
        private Client() {
        }

        public static void register() {
            ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> onRenderGameOverlay(graphics));
        }

        public static void onRenderGameOverlay(GuiGraphics graphics) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            Controller controller = Nanomachines.getController(mc.player);
            if (controller == null) return; // Nothing to show.
            int sizeX = 8;
            int sizeY = 12;
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            Pair<Double, Double> pos = Settings.get().nanomachineHudPos;
            double x = pos.getLeft();
            double y = pos.getRight();
            double left =
                Math.min(width - sizeX,
                    x < 0 ? width / 2 - 91 - 12
                        : x < 1 ? width * x
                        : x);
            double top =
                Math.min(height - sizeY,
                    y < 0 ? height - 39
                        : y < 1 ? y * height
                        : y);
            double fill = controller.getLocalBuffer() / controller.getLocalBufferSize();
            drawRect(graphics, Textures.GUI.Nanomachines, (int) left, (int) top, sizeX, sizeY, 1);
            drawRect(graphics, Textures.GUI.NanomachinesBar, (int) left, (int) top, sizeX, sizeY, (float) fill);
        }

        // Draws the bottom `fill` part of the texture (which is exactly w x h in size).
        private static void drawRect(GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, int w, int h, float fill) {
            int filled = Math.round(h * Math.max(0, Math.min(1, fill)));
            if (filled <= 0) return;
            graphics.blit(texture, x, y + h - filled, 0, h - filled, w, filled, w, h);
        }
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
