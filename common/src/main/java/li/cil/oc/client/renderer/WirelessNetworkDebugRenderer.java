package li.cil.oc.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Settings;
import li.cil.oc.server.network.WirelessNetwork;
import li.cil.oc.util.RTree;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public final class WirelessNetworkDebugRenderer {
    private WirelessNetworkDebugRenderer() {
    }

    public static final int[] colors = {0xFF0000, 0x00FFFF, 0x00FF00, 0x0000FF, 0xFF00FF, 0xFFFF00, 0xFFFFFF, 0x000000};

    /**
     * Registered via {@link li.cil.oc.client.platform.RenderPlatform#registerLevelRenderer}.
     * <p>
     * Port note: boxes are drawn as line boxes (the old code used polygon mode
     * line rendering, which does not exist in the core profile).
     */
    public static void onRenderWorldLastEvent(PoseStack stack, float partialTicks, Camera camera) {
        if (!Settings.rTreeDebugRenderer) return;
        RenderState.checkError(WirelessNetworkDebugRenderer.class.getName() + ".onRenderWorldLastEvent: entering (aka: wasntme)");

        final ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;
        // Only available in single player (the tree lives on the integrated server).
        final RTree<?> tree = WirelessNetwork.dimensions.get(world.dimension());
        if (tree != null) {
            final Vec3 cam = camera.getPosition();

            stack.pushPose();
            stack.translate(-cam.x, -cam.y, -cam.z);

            final MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            final VertexConsumer r = buffers.getBuffer(RenderTypes.MFU_LINES);
            for (Pair<Pair<Triple<Double, Double, Double>, Triple<Double, Double, Double>>, Integer> entry : tree.allBounds()) {
                final Triple<Double, Double, Double> min = entry.getLeft().getLeft();
                final Triple<Double, Double, Double> max = entry.getLeft().getRight();
                final int level = entry.getRight();
                final int color = colors[level % colors.length];
                final float size = 0.5f - level * 0.05f;
                LevelRenderer.renderLineBox(stack, r,
                    min.getLeft() - size, min.getMiddle() - size, min.getRight() - size,
                    max.getLeft() + size, max.getMiddle() + size, max.getRight() + size,
                    ((color >> 16) & 0xFF) / 255f,
                    ((color >> 8) & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    0.25f);
            }
            buffers.endBatch(RenderTypes.MFU_LINES);

            stack.popPose();
        }

        RenderState.checkError(WirelessNetworkDebugRenderer.class.getName() + ".onRenderWorldLastEvent: leaving");
    }
}
