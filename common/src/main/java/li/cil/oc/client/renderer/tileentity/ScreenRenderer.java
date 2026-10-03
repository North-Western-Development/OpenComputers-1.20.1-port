package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.tileentity.Screen;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ScreenRenderer implements BlockEntityRenderer<Screen> {
    private final double maxRenderDistanceSq = Settings.get().maxScreenTextRenderDistance * Settings.get().maxScreenTextRenderDistance;

    private final double fadeDistanceSq = Settings.get().screenTextFadeStartDistance * Settings.get().screenTextFadeStartDistance;

    private final double fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq);

    private Screen screen = null;

    public ScreenRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static boolean isScreen(ItemInfo descriptor) {
        return descriptor != null && (
            descriptor == Items.get(Constants.BlockName.ScreenTier1) ||
                descriptor == Items.get(Constants.BlockName.ScreenTier2) ||
                descriptor == Items.get(Constants.BlockName.ScreenTier3));
    }

    // Multi-block screens are rendered by their origin block and may extend
    // beyond the origin's chunk section.
    @Override
    public boolean shouldRenderOffScreen(Screen screen) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Math.max(64, (int) Math.ceil(Settings.get().maxScreenTextRenderDistance) + 16);
    }

    // ----------------------------------------------------------------------- //
    // Rendering
    // ----------------------------------------------------------------------- //

    @Override
    public void render(Screen screen, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        this.screen = screen;
        // Stale block entities of unloaded chunks may still be handed to us
        // until their render section is rebuilt.
        if (screen.isRemoved() || !screen.isOrigin()) {
            return;
        }

        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        final double distance = playerDistanceSq() / Math.min(screen.width, screen.height);
        if (distance > maxRenderDistanceSq) {
            return;
        }

        final Vec3 eyePos = player.getEyePosition(dt);
        final double eyeDelta = screen.getBlockPos().getY() - eyePos.y;

        // Crude check whether screen text can be seen by the local player based
        // on the player's position -> angle relative to screen.
        final Direction screenFacing = screen.facing().getOpposite();
        final double x = screen.getBlockPos().getX() - eyePos.x;
        final double z = screen.getBlockPos().getZ() - eyePos.z;
        if (screenFacing.getStepX() * (x + 0.5) + screenFacing.getStepY() * (eyeDelta + 0.5) + screenFacing.getStepZ() * (z + 0.5) < 0) {
            return;
        }

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);

        RenderState.checkError(getClass().getName() + ".render: setup");

        drawOverlay(stack, buffer);

        RenderState.checkError(getClass().getName() + ".render: overlay");

        // TODO(port): the fade alpha was already unused in 1.16 (text rendering ignores it).
        final float alpha = distance > fadeDistanceSq ? Math.max(0, 1 - (float) ((distance - fadeDistanceSq) * fadeRatio)) : 1f;

        RenderState.checkError(getClass().getName() + ".render: fade");

        if (screen.buffer().isRenderingEnabled()) {
            draw(stack, alpha);
        }

        stack.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private void transform(PoseStack stack) {
        RenderUtil.rotateYaw(stack, screen.yaw());
        final Direction pitch = screen.pitch();
        if (pitch == Direction.DOWN) stack.mulPose(Axis.XP.rotationDegrees(90));
        else if (pitch == Direction.UP) stack.mulPose(Axis.XP.rotationDegrees(-90));

        // Fit area to screen (bottom left = bottom left).
        stack.translate(-0.5f, -0.5f, 0.5f);
        stack.translate(0, screen.height, 0);

        // Flip text upside down.
        stack.scale(1, -1, 1);
    }

    private void drawOverlay(PoseStack matrix, MultiBufferSource buffer) {
        if (screen.facing() == Direction.UP || screen.facing() == Direction.DOWN) {
            // Show up vector overlay when holding same screen block.
            final LocalPlayer player = Minecraft.getInstance().player;
            final ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!stack.isEmpty()) {
                if (Wrench.holdsApplicableWrench(player, screen.getBlockPos()) || isScreen(Items.get(stack))) {
                    final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
                    matrix.pushPose();
                    transform(matrix);
                    matrix.translate(screen.width / 2f - 0.5f, screen.height / 2f - 0.5f, 0.05f);

                    final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.ScreenUpIndicator);
                    final Matrix4f pose = matrix.last().pose();
                    r.vertex(pose, 0, 1, 0).uv(icon.getU0(), icon.getV1()).endVertex();
                    r.vertex(pose, 1, 1, 0).uv(icon.getU1(), icon.getV1()).endVertex();
                    r.vertex(pose, 1, 0, 0).uv(icon.getU1(), icon.getV0()).endVertex();
                    r.vertex(pose, 0, 0, 0).uv(icon.getU0(), icon.getV0()).endVertex();

                    matrix.popPose();
                }
            }
        }
    }

    private void draw(PoseStack stack, float alpha) {
        RenderState.checkError(getClass().getName() + ".draw: entering (aka: wasntme)");

        final int sx = screen.width;
        final int sy = screen.height;
        final float tw = sx * 16f;
        final float th = sy * 16f;

        transform(stack);

        // Offset from border.
        stack.translate(sx * 2.25f / tw, sy * 2.25f / th, 0);

        // Inner size (minus borders).
        final float isx = sx - (4.5f / 16);
        final float isy = sy - (4.5f / 16);

        // Scale based on actual buffer size.
        final int sizeX = screen.buffer().renderWidth();
        final int sizeY = screen.buffer().renderHeight();
        final float scaleX = isx / sizeX;
        final float scaleY = isy / sizeY;
        if (scaleX > scaleY) {
            stack.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0);
            stack.scale(scaleY, scaleY, 1);
        } else {
            stack.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0);
            stack.scale(scaleX, scaleX, 1);
        }

        // Slightly offset the text so it doesn't clip into the screen.
        stack.translate(0, 0, 0.01);

        RenderState.checkError(getClass().getName() + ".draw: setup");

        // Render the actual text. This draws immediately (cached vertex buffers).
        screen.buffer().renderText(stack);

        RenderState.checkError(getClass().getName() + ".draw: text");
    }

    private double playerDistanceSq() {
        final LocalPlayer player = Minecraft.getInstance().player;
        final AABB bounds = screen.getRenderBoundingBox();

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        final double ex = bounds.maxX - bounds.minX;
        final double ey = bounds.maxY - bounds.minY;
        final double ez = bounds.maxZ - bounds.minZ;
        final double cx = bounds.minX + ex * 0.5;
        final double cy = bounds.minY + ey * 0.5;
        final double cz = bounds.minZ + ez * 0.5;
        final double dx = px - cx;
        final double dy = py - cy;
        final double dz = pz - cz;

        return axisDistanceSq(dx, ex) + axisDistanceSq(dy, ey) + axisDistanceSq(dz, ez);
    }

    private static double axisDistanceSq(double d, double e) {
        if (d < -e) {
            final double v = d + e;
            return v * v;
        } else if (d > e) {
            final double v = d - e;
            return v * v;
        } else return 0;
    }
}
