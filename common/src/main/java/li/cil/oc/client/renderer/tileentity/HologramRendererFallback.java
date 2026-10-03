package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc.common.tileentity.Hologram;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

public final class HologramRendererFallback {
    private HologramRendererFallback() {
    }

    public static String text = "Requires OpenGL 1.5";

    public static void render(Hologram hologram, float f, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(HologramRendererFallback.class.getName() + ".render: entering (aka: wasntme)");

        final Font fontRenderer = Minecraft.getInstance().font;

        stack.pushPose();
        stack.translate(0.5, 0.75, 0.5);
        RenderState.mirrorScale(stack, 1 / 128f, -1 / 128f, 1 / 128f);

        fontRenderer.drawInBatch(text, -fontRenderer.width(text) / 2f, 0, 0xFFFFFFFF,
            false, stack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
        stack.mulPose(Axis.YP.rotationDegrees(180));
        fontRenderer.drawInBatch(text, -fontRenderer.width(text) / 2f, 0, 0xFFFFFFFF,
            false, stack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);

        stack.popPose();

        RenderState.checkError(HologramRendererFallback.class.getName() + ".render: leaving");
    }
}
