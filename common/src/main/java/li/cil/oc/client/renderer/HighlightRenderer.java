package li.cil.oc.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.client.Textures;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;

import java.util.Random;

public final class HighlightRenderer {
    private HighlightRenderer() {
    }

    private static final Random random = new Random();

    public static final RenderType TexHologram = RenderTypes.createTexturedQuad("hologram_effect", Textures.Model.HologramEffect, DefaultVertexFormat.POSITION_TEX_COLOR, true);

    /**
     * Registered via {@link li.cil.oc.client.platform.RenderPlatform#registerBlockHighlightRenderer}.
     */
    public static void onDrawBlockHighlight(PoseStack stack, MultiBufferSource buffers, Camera camera, BlockHitResult hitInfo) {
        if (hitInfo == null || hitInfo.getBlockPos() == null) return;
        final Minecraft mc = Minecraft.getInstance();
        final ClientLevel world = mc.level;
        if (world == null || mc.player == null) return;
        final BlockPos blockPos = hitInfo.getBlockPos();
        if (Items.get(mc.player.getItemInHand(InteractionHand.MAIN_HAND)) != Items.get(Constants.ItemName.Tablet)) return;
        if (world.isEmptyBlock(blockPos)) return;

        final VoxelShape shape = world.getBlockState(blockPos).getShape(world, blockPos,
            camera.getEntity() != null ? CollisionContext.of(camera.getEntity()) : CollisionContext.empty());
        if (shape.isEmpty()) return;
        final float minX = (float) shape.min(Direction.Axis.X), minY = (float) shape.min(Direction.Axis.Y), minZ = (float) shape.min(Direction.Axis.Z);
        final float maxX = (float) shape.max(Direction.Axis.X), maxY = (float) shape.max(Direction.Axis.Y), maxZ = (float) shape.max(Direction.Axis.Z);
        final Direction sideHit = hitInfo.getDirection();
        final Vec3 view = camera.getPosition();

        stack.pushPose();

        stack.translate(blockPos.getX() - view.x, blockPos.getY() - view.y, blockPos.getZ() - view.z);
        stack.scale(1.002f, 1.002f, 1.002f);

        if (Settings.get().hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get().hologramFlickerFrequency) {
            final int sx = 1 - Math.abs(sideHit.getStepX()), sy = 1 - Math.abs(sideHit.getStepY()), sz = 1 - Math.abs(sideHit.getStepZ());
            stack.scale(1f + (float) (random.nextGaussian() * 0.01), 1f + (float) (random.nextGaussian() * 0.001), 1f + (float) (random.nextGaussian() * 0.01));
            stack.translate((float) (random.nextGaussian() * 0.01 * sx), (float) (random.nextGaussian() * 0.01 * sy), (float) (random.nextGaussian() * 0.01 * sz));
        }

        final VertexConsumer r = buffers.getBuffer(TexHologram);
        final Matrix4f m = stack.last().pose();
        switch (sideHit) {
            case UP:
                v(r, m, maxX, maxY + 0.002f, maxZ, maxZ * 16, maxX * 16);
                v(r, m, maxX, maxY + 0.002f, minZ, minZ * 16, maxX * 16);
                v(r, m, minX, maxY + 0.002f, minZ, minZ * 16, minX * 16);
                v(r, m, minX, maxY + 0.002f, maxZ, maxZ * 16, minX * 16);
                break;
            case DOWN:
                v(r, m, maxX, minY - 0.002f, minZ, minZ * 16, maxX * 16);
                v(r, m, maxX, minY - 0.002f, maxZ, maxZ * 16, maxX * 16);
                v(r, m, minX, minY - 0.002f, maxZ, maxZ * 16, minX * 16);
                v(r, m, minX, minY - 0.002f, minZ, minZ * 16, minX * 16);
                break;
            case EAST:
                v(r, m, maxX + 0.002f, maxY, minZ, minZ * 16, maxY * 16);
                v(r, m, maxX + 0.002f, maxY, maxZ, maxZ * 16, maxY * 16);
                v(r, m, maxX + 0.002f, minY, maxZ, maxZ * 16, minY * 16);
                v(r, m, maxX + 0.002f, minY, minZ, minZ * 16, minY * 16);
                break;
            case WEST:
                v(r, m, minX - 0.002f, maxY, maxZ, maxZ * 16, maxY * 16);
                v(r, m, minX - 0.002f, maxY, minZ, minZ * 16, maxY * 16);
                v(r, m, minX - 0.002f, minY, minZ, minZ * 16, minY * 16);
                v(r, m, minX - 0.002f, minY, maxZ, maxZ * 16, minY * 16);
                break;
            case SOUTH:
                v(r, m, maxX, maxY, maxZ + 0.002f, maxX * 16, maxY * 16);
                v(r, m, minX, maxY, maxZ + 0.002f, minX * 16, maxY * 16);
                v(r, m, minX, minY, maxZ + 0.002f, minX * 16, minY * 16);
                v(r, m, maxX, minY, maxZ + 0.002f, maxX * 16, minY * 16);
                break;
            default:
                v(r, m, minX, maxY, minZ - 0.002f, minX * 16, maxY * 16);
                v(r, m, maxX, maxY, minZ - 0.002f, maxX * 16, maxY * 16);
                v(r, m, maxX, minY, minZ - 0.002f, maxX * 16, minY * 16);
                v(r, m, minX, minY, minZ - 0.002f, minX * 16, minY * 16);
                break;
        }

        stack.popPose();
    }

    private static void v(VertexConsumer r, Matrix4f m, float x, float y, float z, float u, float v) {
        r.vertex(m, x, y, z).uv(u, v).color(0.0F, 1.0F, 0.0F, 0.4F).endVertex();
    }
}
