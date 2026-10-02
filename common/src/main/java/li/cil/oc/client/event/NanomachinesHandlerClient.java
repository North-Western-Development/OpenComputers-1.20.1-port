package li.cil.oc.client.event;

import dev.architectury.event.events.client.ClientGuiEvent;
import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.client.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Client side HUD rendering. {@link #register()} must only be called on the physical client.
 */
public final class NanomachinesHandlerClient {
    private NanomachinesHandlerClient() {
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
