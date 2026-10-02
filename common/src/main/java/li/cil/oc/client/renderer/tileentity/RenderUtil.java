package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.client.Textures;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static boolean shouldShowErrorLight(int hash) {
        final long time = System.currentTimeMillis() + hash;
        final long timeSlice = time / 500;
        return timeSlice % 2 == 0;
    }

    // ----------------------------------------------------------------------- //
    // Helpers shared by the block entity renderers.

    /**
     * Rotates around the Y axis so that south faces the specified yaw.
     */
    public static void rotateYaw(PoseStack stack, Direction yaw) {
        if (yaw == Direction.WEST) stack.mulPose(Axis.YP.rotationDegrees(-90));
        else if (yaw == Direction.NORTH) stack.mulPose(Axis.YP.rotationDegrees(180));
        else if (yaw == Direction.EAST) stack.mulPose(Axis.YP.rotationDegrees(90));
        // else: no yaw.
    }

    /**
     * Draws a front overlay quad in the (0,0)-(1,1) square at z = 0 (for the
     * {@code BLOCK_OVERLAY} render type).
     */
    public static void renderFrontOverlay(PoseStack stack, ResourceLocation texture, VertexConsumer r) {
        final TextureAtlasSprite icon = Textures.getSprite(texture);
        final Matrix4f pose = stack.last().pose();
        r.vertex(pose, 0, 1, 0).uv(icon.getU0(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 1, 0).uv(icon.getU1(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 0, 0).uv(icon.getU1(), icon.getV0()).endVertex();
        r.vertex(pose, 0, 0, 0).uv(icon.getU0(), icon.getV0()).endVertex();
    }

    /**
     * Draws the four side faces of the unit cube (north, south, east, west)
     * with the same sprite, after flipping the y axis (for the
     * {@code BLOCK_OVERLAY} render type).
     */
    public static void renderSideOverlays(PoseStack stack, VertexConsumer r, TextureAtlasSprite icon) {
        final Matrix4f pose = stack.last().pose();
        r.vertex(pose, 1, 1, 0).uv(icon.getU0(), icon.getV1()).endVertex();
        r.vertex(pose, 0, 1, 0).uv(icon.getU1(), icon.getV1()).endVertex();
        r.vertex(pose, 0, 0, 0).uv(icon.getU1(), icon.getV0()).endVertex();
        r.vertex(pose, 1, 0, 0).uv(icon.getU0(), icon.getV0()).endVertex();

        r.vertex(pose, 0, 1, 1).uv(icon.getU0(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 1, 1).uv(icon.getU1(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 0, 1).uv(icon.getU1(), icon.getV0()).endVertex();
        r.vertex(pose, 0, 0, 1).uv(icon.getU0(), icon.getV0()).endVertex();

        r.vertex(pose, 1, 1, 1).uv(icon.getU0(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 1, 0).uv(icon.getU1(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 0, 0).uv(icon.getU1(), icon.getV0()).endVertex();
        r.vertex(pose, 1, 0, 1).uv(icon.getU0(), icon.getV0()).endVertex();

        r.vertex(pose, 0, 1, 0).uv(icon.getU0(), icon.getV1()).endVertex();
        r.vertex(pose, 0, 1, 1).uv(icon.getU1(), icon.getV1()).endVertex();
        r.vertex(pose, 0, 0, 1).uv(icon.getU1(), icon.getV0()).endVertex();
        r.vertex(pose, 0, 0, 0).uv(icon.getU0(), icon.getV0()).endVertex();
    }

    /**
     * Draws the top face (y = 0 after flipping the y axis).
     */
    public static void renderTopOverlay(PoseStack stack, VertexConsumer r, TextureAtlasSprite icon) {
        final Matrix4f pose = stack.last().pose();
        r.vertex(pose, 0, 0, 1).uv(icon.getU0(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 0, 1).uv(icon.getU1(), icon.getV1()).endVertex();
        r.vertex(pose, 1, 0, 0).uv(icon.getU1(), icon.getV0()).endVertex();
        r.vertex(pose, 0, 0, 0).uv(icon.getU0(), icon.getV0()).endVertex();
    }

    /**
     * Draws the overlay on every open side (for adapter and net splitter).
     */
    public static void renderOpenSideOverlays(PoseStack stack, VertexConsumer r, TextureAtlasSprite sideActivity, java.util.function.Predicate<Direction> isOpen) {
        final Matrix4f pose = stack.last().pose();
        if (isOpen.test(Direction.DOWN)) {
            r.vertex(pose, 0, 1, 0).uv(sideActivity.getU1(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 1, 1, 0).uv(sideActivity.getU0(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 1, 1, 1).uv(sideActivity.getU0(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 0, 1, 1).uv(sideActivity.getU1(), sideActivity.getV1()).endVertex();
        }

        if (isOpen.test(Direction.UP)) {
            r.vertex(pose, 0, 0, 0).uv(sideActivity.getU1(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 0, 0, 1).uv(sideActivity.getU1(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 1, 0, 1).uv(sideActivity.getU0(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 1, 0, 0).uv(sideActivity.getU0(), sideActivity.getV1()).endVertex();
        }

        if (isOpen.test(Direction.NORTH)) {
            r.vertex(pose, 1, 1, 0).uv(sideActivity.getU0(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 0, 1, 0).uv(sideActivity.getU1(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 0, 0, 0).uv(sideActivity.getU1(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 1, 0, 0).uv(sideActivity.getU0(), sideActivity.getV0()).endVertex();
        }

        if (isOpen.test(Direction.SOUTH)) {
            r.vertex(pose, 0, 1, 1).uv(sideActivity.getU0(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 1, 1, 1).uv(sideActivity.getU1(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 1, 0, 1).uv(sideActivity.getU1(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 0, 0, 1).uv(sideActivity.getU0(), sideActivity.getV0()).endVertex();
        }

        if (isOpen.test(Direction.WEST)) {
            r.vertex(pose, 0, 1, 0).uv(sideActivity.getU0(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 0, 1, 1).uv(sideActivity.getU1(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 0, 0, 1).uv(sideActivity.getU1(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 0, 0, 0).uv(sideActivity.getU0(), sideActivity.getV0()).endVertex();
        }

        if (isOpen.test(Direction.EAST)) {
            r.vertex(pose, 1, 1, 1).uv(sideActivity.getU0(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 1, 1, 0).uv(sideActivity.getU1(), sideActivity.getV1()).endVertex();
            r.vertex(pose, 1, 0, 0).uv(sideActivity.getU1(), sideActivity.getV0()).endVertex();
            r.vertex(pose, 1, 0, 1).uv(sideActivity.getU0(), sideActivity.getV0()).endVertex();
        }
    }

    public static boolean anySideOpen(java.util.function.Predicate<Direction> isOpen) {
        for (Direction side : Direction.values()) {
            if (isOpen.test(side)) return true;
        }
        return false;
    }
}
