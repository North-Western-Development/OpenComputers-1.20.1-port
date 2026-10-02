package li.cil.oc.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MFUTargetRenderer {
    private MFUTargetRenderer() {
    }

    private static final float drawRed = 0.0f, drawGreen = 1.0f, drawBlue = 0.0f;

    /**
     * Registered via {@link li.cil.oc.client.platform.RenderPlatform#registerLevelRenderer}.
     */
    public static void onRenderWorldLastEvent(PoseStack matrix, float partialTicks, Camera camera) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        if (player == null) return;
        final ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty() || Items.get(stack) != Items.get(Constants.ItemName.MFU) || !stack.hasTag()) return;
        final CompoundTag data = stack.getTag();
        if (!data.contains(Settings.namespace + "coord", Tag.TAG_INT_ARRAY)) return;

        final ResourceLocation dimension = new ResourceLocation(data.getString(Settings.namespace + "dimension"));
        if (!player.level().dimension().location().equals(dimension)) return;
        final int[] coord = data.getIntArray(Settings.namespace + "coord");
        if (coord.length < 4) return;
        final int x = coord[0], y = coord[1], z = coord[2], side = coord[3];
        if (player.distanceToSqr(x, y, z) > 64 * 64) return;

        final AABB bounds = BlockPosition.apply(x, y, z).bounds().inflate(0.1, 0.1, 0.1);

        RenderState.checkError(MFUTargetRenderer.class.getName() + ".onRenderWorldLastEvent: entering (aka: wasntme)");

        matrix.pushPose();
        final Vec3 camPos = camera.getPosition();
        matrix.translate(-camPos.x, -camPos.y, -camPos.z);

        final MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(matrix, buffer.getBuffer(RenderTypes.MFU_LINES), bounds, drawRed, drawGreen, drawBlue, 0.5f);
        drawFace(matrix.last().pose(), buffer.getBuffer(RenderTypes.MFU_QUADS), (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ,
            (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, side, drawRed, drawGreen, drawBlue);
        buffer.endBatch(RenderTypes.MFU_LINES);
        buffer.endBatch(RenderTypes.MFU_QUADS);

        matrix.popPose();

        RenderState.checkError(MFUTargetRenderer.class.getName() + ".onRenderWorldLastEvent: leaving");
    }

    private static void drawFace(Matrix4f matrix, VertexConsumer builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int side, float r, float g, float b) {
        switch (side) {
            case 0: // Down
                builder.vertex(matrix, minX, minY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, minY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, minY, minZ).color(r, g, b, 0.25f).endVertex();
                break;
            case 1: // Up
                builder.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, maxY, minZ).color(r, g, b, 0.25f).endVertex();
                break;
            case 2: // North
                builder.vertex(matrix, minX, minY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, minY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, maxY, minZ).color(r, g, b, 0.25f).endVertex();
                break;
            case 3: // South
                builder.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, minY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 0.25f).endVertex();
                break;
            case 4: // East
                builder.vertex(matrix, minX, minY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, maxY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, minX, minY, maxZ).color(r, g, b, 0.25f).endVertex();
                break;
            case 5: // West
                builder.vertex(matrix, maxX, minY, minZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 0.25f).endVertex();
                builder.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 0.25f).endVertex();
                break;
            default: // WTF?
                break;
        }
    }
}
