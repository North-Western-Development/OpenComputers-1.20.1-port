package li.cil.oc.client.renderer.tileentity;

import com.google.common.base.Strings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.tileentity.Robot;
import li.cil.oc.common.tileentity.RobotProxy;
import li.cil.oc.util.RenderState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RobotRenderer implements BlockEntityRenderer<RobotProxy> {
    private static RobotRenderer instance;

    /**
     * Renders a robot chassis without a robot (used for pets).
     */
    public static void renderChassis(PoseStack stack, MultiBufferSource buffer, int light, double offset, boolean isRunningOverride) {
        if (instance == null) instance = new RobotRenderer(null);
        instance.renderChassis(stack, buffer, light, null, offset, isRunningOverride);
    }

    public static void renderChassis(PoseStack stack, MultiBufferSource buffer, int light) {
        renderChassis(stack, buffer, light, 0, false);
    }

    private final RobotRenderEvent.MountPoint[] mountPoints = new RobotRenderEvent.MountPoint[7];

    private static final Map<String, Integer> slotNameMapping = new HashMap<>();

    static {
        slotNameMapping.put(MountPointName.TopLeft, 0);
        slotNameMapping.put(MountPointName.TopRight, 1);
        slotNameMapping.put(MountPointName.TopBack, 2);
        slotNameMapping.put(MountPointName.BottomLeft, 3);
        slotNameMapping.put(MountPointName.BottomRight, 4);
        slotNameMapping.put(MountPointName.BottomBack, 5);
        slotNameMapping.put(MountPointName.BottomFront, 6);
    }

    private static final float size = 0.4f;
    private static final float l = 0.5f - size;
    private static final float h = 0.5f + size;
    private static final float gap = 1.0f / 28.0f;
    private static final float gt = 0.5f + gap;
    private static final float gb = 0.5f - gap;

    public RobotRenderer(BlockEntityRendererProvider.Context context) {
        for (Map.Entry<String, Integer> entry : slotNameMapping.entrySet()) {
            mountPoints[entry.getValue()] = new RobotRenderEvent.MountPoint(entry.getKey());
        }
    }

    // ----------------------------------------------------------------------- //

    private static void chassisVertex(VertexConsumer r, Matrix4f pose, Matrix3f normal, float x, float y, float z,
                                      int red, int green, int blue, float u, float v, int light, double nx, double ny, double nz) {
        final Vec3 normalized = new Vec3(nx, ny, nz).normalize();
        r.vertex(pose, x, y, z)
            .color(red, green, blue, 0xFF)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(normal, (float) normalized.x, (float) normalized.y, (float) normalized.z)
            .endVertex();
    }

    private void drawTop(PoseStack stack, MultiBufferSource buffer, int light, int red, int green, int blue) {
        final VertexConsumer r = buffer.getBuffer(RenderTypes.ROBOT_CHASSIS);
        final Matrix4f pose = stack.last().pose();
        final Matrix3f n = stack.last().normal();

        chassisVertex(r, pose, n, 0.5f, 1, 0.5f, red, green, blue, 0.25f, 0.25f, light, 0, 0.2, 1);
        chassisVertex(r, pose, n, l, gt, h, red, green, blue, 0, 0.5f, light, 0, 0.2, 1);
        chassisVertex(r, pose, n, h, gt, h, red, green, blue, 0.5f, 0.5f, light, 0, 0.2, 1);

        chassisVertex(r, pose, n, 0.5f, 1, 0.5f, red, green, blue, 0.25f, 0.25f, light, 0, 0.2, 1);
        chassisVertex(r, pose, n, h, gt, h, red, green, blue, 0.5f, 0.5f, light, 0, 0.2, 1);
        chassisVertex(r, pose, n, h, gt, l, red, green, blue, 0.5f, 0, light, 1, 0.2, 0);

        chassisVertex(r, pose, n, 0.5f, 1, 0.5f, red, green, blue, 0.25f, 0.25f, light, 0, 0.2, 1);
        chassisVertex(r, pose, n, h, gt, l, red, green, blue, 0.5f, 0, light, 1, 0.2, 0);
        chassisVertex(r, pose, n, l, gt, l, red, green, blue, 0, 0, light, 0, 0.2, -1);

        chassisVertex(r, pose, n, 0.5f, 1, 0.5f, red, green, blue, 0.25f, 0.25f, light, 0, 0.2, 1);
        chassisVertex(r, pose, n, l, gt, l, red, green, blue, 0, 0, light, 0, 0.2, -1);
        chassisVertex(r, pose, n, l, gt, h, red, green, blue, 0, 0.5f, light, -1, 0.2, 0);

        chassisVertex(r, pose, n, l, gt, h, red, green, blue, 0, 1, light, 0, -1, 0);
        chassisVertex(r, pose, n, l, gt, l, red, green, blue, 0, 0.5f, light, 0, -1, 0);
        chassisVertex(r, pose, n, h, gt, l, red, green, blue, 0.5f, 0.5f, light, 0, -1, 0);

        chassisVertex(r, pose, n, l, gt, h, red, green, blue, 0, 1, light, 0, -1, 0);
        chassisVertex(r, pose, n, h, gt, l, red, green, blue, 0.5f, 0.5f, light, 0, -1, 0);
        chassisVertex(r, pose, n, h, gt, h, red, green, blue, 0.5f, 1, light, 0, -1, 0);
    }

    private void drawBottom(PoseStack stack, MultiBufferSource buffer, int light, int red, int green, int blue) {
        final VertexConsumer r = buffer.getBuffer(RenderTypes.ROBOT_CHASSIS);
        final Matrix4f pose = stack.last().pose();
        final Matrix3f n = stack.last().normal();

        chassisVertex(r, pose, n, 0.5f, 0.03f, 0.5f, red, green, blue, 0.75f, 0.25f, light, 0, -0.2, 1);
        chassisVertex(r, pose, n, l, gb, l, red, green, blue, 0.5f, 0, light, 0, -0.2, 1);
        chassisVertex(r, pose, n, h, gb, l, red, green, blue, 1, 0, light, 0, -0.2, 1);

        chassisVertex(r, pose, n, 0.5f, 0.03f, 0.5f, red, green, blue, 0.75f, 0.25f, light, 0, -0.2, 1);
        chassisVertex(r, pose, n, h, gb, l, red, green, blue, 1, 0, light, 0, -0.2, 1);
        chassisVertex(r, pose, n, h, gb, h, red, green, blue, 1, 0.5f, light, 1, -0.2, 0);

        chassisVertex(r, pose, n, 0.5f, 0.03f, 0.5f, red, green, blue, 0.75f, 0.25f, light, 0, -0.2, 1);
        chassisVertex(r, pose, n, h, gb, h, red, green, blue, 1, 0.5f, light, 1, -0.2, 0);
        chassisVertex(r, pose, n, l, gb, h, red, green, blue, 0.5f, 0.5f, light, 0, -0.2, -1);

        chassisVertex(r, pose, n, 0.5f, 0.03f, 0.5f, red, green, blue, 0.75f, 0.25f, light, 0, -0.2, 1);
        chassisVertex(r, pose, n, l, gb, h, red, green, blue, 0.5f, 0.5f, light, 0, -0.2, -1);
        chassisVertex(r, pose, n, l, gb, l, red, green, blue, 0.5f, 0, light, -1, -0.2, 0);

        chassisVertex(r, pose, n, l, gb, l, red, green, blue, 0, 0.5f, light, 0, 1, 0);
        chassisVertex(r, pose, n, l, gb, h, red, green, blue, 0, 1, light, 0, 1, 0);
        chassisVertex(r, pose, n, h, gb, h, red, green, blue, 0.5f, 1, light, 0, 1, 0);

        chassisVertex(r, pose, n, l, gb, l, red, green, blue, 0, 0.5f, light, 0, 1, 0);
        chassisVertex(r, pose, n, h, gb, h, red, green, blue, 0.5f, 1, light, 0, 1, 0);
        chassisVertex(r, pose, n, h, gb, l, red, green, blue, 0.5f, 0.5f, light, 0, 1, 0);
    }

    private static void setMountPoint(RobotRenderEvent.MountPoint mountPoint, float y, float angle) {
        mountPoint.offset.set(0, y, 0.24f);
        mountPoint.rotation.set(0, 1, 0, angle);
    }

    public void resetMountPoints(boolean running) {
        final float offset = running ? 0 : -0.06f;

        // Left top.
        setMountPoint(mountPoints[0], 0.2f, 90);
        // Right top.
        setMountPoint(mountPoints[1], 0.2f, -90);
        // Back top.
        setMountPoint(mountPoints[2], 0.2f, 180);
        // Left bottom.
        setMountPoint(mountPoints[3], -0.2f - offset, 90);
        // Right bottom.
        setMountPoint(mountPoints[4], -0.2f - offset, -90);
        // Back bottom.
        setMountPoint(mountPoints[5], -0.2f - offset, 180);
        // Front bottom.
        setMountPoint(mountPoints[6], -0.2f - offset, 0);
    }

    public void renderChassis(PoseStack stack, MultiBufferSource buffer, int light, Robot robot, double offset, boolean isRunningOverride) {
        final boolean isRunning = robot == null ? isRunningOverride : robot.isRunning();

        final float size = 0.3f;
        final float l = 0.5f - size;
        final float h = 0.5f + size;
        final float vStep = 1.0f / 32.0f;

        final float offsetV = ((int) ((offset - (int) offset) * 16)) * vStep;
        final float u0, u1, v0, v1;
        if (isRunning) {
            u0 = 0.5f;
            u1 = 1f;
            v0 = 0.5f + offsetV;
            v1 = 0.5f + vStep + offsetV;
        } else {
            u0 = 0.25f - vStep;
            u1 = 0.25f + vStep;
            v0 = 0.75f - vStep;
            v1 = 0.75f + vStep;
        }

        resetMountPoints(robot != null && robot.isRunning());
        final RobotRenderEvent event = new RobotRenderEvent(robot, mountPoints);
        if (!EventBus.INSTANCE.post(event)) {
            final int color = event.getColorMultiplier();
            final int cr = (color >> 16) & 0xFF;
            final int cg = (color >> 8) & 0xFF;
            final int cb = color & 0xFF;
            if (!isRunning) {
                stack.translate(0, -2 * gap, 0);
            }
            drawBottom(stack, buffer, light, cr, cg, cb);
            if (!isRunning) {
                stack.translate(0, -2 * gap, 0);
            }
            drawTop(stack, buffer, light, cr, cg, cb);

            if (isRunning) {
                // Light color.
                final int lightColor;
                if (event.lightColor < 0) {
                    lightColor = robot != null && robot.info != null ? robot.info.lightColor : 0xF23030;
                } else lightColor = event.lightColor & 0xFFFFFF;
                final int red = (lightColor >>> 16) & 0xFF;
                final int green = (lightColor >>> 8) & 0xFF;
                final int blue = lightColor & 0xFF;

                final VertexConsumer r = buffer.getBuffer(RenderTypes.ROBOT_LIGHT);
                final Matrix4f pose = stack.last().pose();
                r.vertex(pose, l, gt, l).color(red, green, blue, 0xFF).uv(u0, v0).endVertex();
                r.vertex(pose, l, gb, l).color(red, green, blue, 0xFF).uv(u0, v1).endVertex();
                r.vertex(pose, l, gb, h).color(red, green, blue, 0xFF).uv(u1, v1).endVertex();
                r.vertex(pose, l, gt, h).color(red, green, blue, 0xFF).uv(u1, v0).endVertex();

                r.vertex(pose, l, gt, h).color(red, green, blue, 0xFF).uv(u0, v0).endVertex();
                r.vertex(pose, l, gb, h).color(red, green, blue, 0xFF).uv(u0, v1).endVertex();
                r.vertex(pose, h, gb, h).color(red, green, blue, 0xFF).uv(u1, v1).endVertex();
                r.vertex(pose, h, gt, h).color(red, green, blue, 0xFF).uv(u1, v0).endVertex();

                r.vertex(pose, h, gt, h).color(red, green, blue, 0xFF).uv(u0, v0).endVertex();
                r.vertex(pose, h, gb, h).color(red, green, blue, 0xFF).uv(u0, v1).endVertex();
                r.vertex(pose, h, gb, l).color(red, green, blue, 0xFF).uv(u1, v1).endVertex();
                r.vertex(pose, h, gt, l).color(red, green, blue, 0xFF).uv(u1, v0).endVertex();

                r.vertex(pose, h, gt, l).color(red, green, blue, 0xFF).uv(u0, v0).endVertex();
                r.vertex(pose, h, gb, l).color(red, green, blue, 0xFF).uv(u0, v1).endVertex();
                r.vertex(pose, l, gb, l).color(red, green, blue, 0xFF).uv(u1, v1).endVertex();
                r.vertex(pose, l, gt, l).color(red, green, blue, 0xFF).uv(u1, v0).endVertex();
            }
        }
    }

    @Override
    public void render(RobotProxy proxy, float f, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        final Robot robot = proxy.robot;
        final double worldTime = proxy.getLevel().getGameTime() + f;

        matrix.pushPose();

        matrix.translate(0.5, 0.5, 0.5);

        // If the move started while we were rendering and we have a reference to
        // the *old* proxy the robot would be rendered at the wrong position, so we
        // correct for the offset.
        if (robot.proxy != proxy) {
            matrix.translate(robot.proxy.x() - proxy.x(), robot.proxy.y() - proxy.y(), robot.proxy.z() - proxy.z());
        }

        if (robot.isAnimatingMove()) {
            final double remaining = (robot.animationTicksLeft - f) / (double) robot.animationTicksTotal;
            final BlockPos delta = robot.moveFrom.get().subtract(robot.getBlockPos());
            matrix.translate(delta.getX() * remaining, delta.getY() * remaining, delta.getZ() * remaining);
        }

        final int timeJitter = robot.hashCode() ^ 0xFF;
        final float hover = robot.isRunning()
            ? (float) (Math.sin(timeJitter + worldTime / 20.0) * 0.03)
            : -0.03f;
        matrix.translate(0, hover, 0);

        matrix.pushPose();

        if (robot.isAnimatingTurn()) {
            final float remaining = (robot.animationTicksLeft - f) / (float) robot.animationTicksTotal;
            final Axis axis = robot.turnAxis < 0 ? Axis.YN : Axis.YP;
            matrix.mulPose(axis.rotationDegrees(90 * remaining));
        }

        RenderUtil.rotateYaw(matrix, robot.yaw());

        matrix.translate(-0.5f, -0.5f, -0.5f);

        final double offset = timeJitter + worldTime / 20.0;
        renderChassis(matrix, buffer, light, robot, offset, false);

        final BlockPos pos = proxy.getBlockPos();
        final double dist = Minecraft.getInstance().player.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (!robot.renderingErrored && dist < 24 * 24) {
            final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            final ItemStack stack = robot.getItem(0);
            if (!stack.isEmpty()) {
                matrix.pushPose();
                try {
                    // Copy-paste from player render code, with minor adjustments for
                    // robot scale.

                    RenderState.mirrorScale(matrix, 1, -1, -1);
                    matrix.translate(0, -8 * 0.0625F - 0.0078125F, -0.5F);

                    if (robot.isAnimatingSwing()) {
                        final int wantedTicksPerCycle = 10;
                        final int cycles = Math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1);
                        final int ticksPerCycle = robot.animationTicksTotal / cycles;
                        final double remaining = (robot.animationTicksLeft - f) / (double) ticksPerCycle;
                        matrix.mulPose(Axis.XP.rotationDegrees((float) (Math.sin((remaining - (int) remaining) * Math.PI) * 45)));
                    }

                    final Item item = stack.getItem();
                    if (item instanceof BlockItem) {
                        matrix.mulPose(Axis.XP.rotationDegrees(180.0F));
                        matrix.mulPose(Axis.YP.rotationDegrees(90.0F));
                        final float scale = 0.625F;
                        matrix.scale(scale, scale, scale);
                    } else if (item == Items.BOW) {
                        matrix.translate(0, -3f / 16f, -0.125F);
                        matrix.mulPose(Axis.ZP.rotationDegrees(170.0F));
                        final float scale = 0.625F;
                        matrix.scale(scale, scale, scale);
                    } else {
                        matrix.translate(1f / 16f, 1f / 16f, -2f / 16f);
                        final float scale = 0.625F;
                        matrix.scale(scale, scale, scale);
                        matrix.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    }

                    itemRenderer.renderStatic(Minecraft.getInstance().player, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrix, buffer, proxy.getLevel(), light, overlay, 0);
                } catch (Throwable e) {
                    OpenComputers.log.warn("Failed rendering equipped item.", e);
                    robot.renderingErrored = true;
                }
                matrix.popPose();
            }

            final Set<String> availableSlots = new HashSet<>(slotNameMapping.keySet());
            final List<Object[]> wildcardRenderers = new ArrayList<>();
            final Object[][] slotMapping = new Object[mountPoints.length][];

            final List<Integer> slots = new ArrayList<>();
            for (int slot : robot.componentSlots()) slots.add(slot);
            for (int slot : robot.containerSlots()) slots.add(slot);
            for (int slot : slots) {
                final ItemStack upgrade = robot.getItem(slot);
                final UpgradeRenderer renderer = upgrade.isEmpty() ? null : li.cil.oc.client.renderer.item.UpgradeRenderer.forStack(upgrade);
                if (renderer != null) {
                    final String preferredSlot = renderer.computePreferredMountPoint(upgrade, robot, availableSlots);
                    if (availableSlots.remove(preferredSlot)) {
                        slotMapping[slotNameMapping.get(preferredSlot)] = new Object[]{upgrade, renderer};
                    } else if (MountPointName.Any.equals(preferredSlot)) {
                        wildcardRenderers.add(new Object[]{upgrade, renderer});
                    }
                }
            }

            int firstEmpty = Arrays.asList(slotMapping).indexOf(null);
            for (Object[] entry : wildcardRenderers) {
                if (firstEmpty < 0) break;
                slotMapping[firstEmpty] = entry;
                firstEmpty = Arrays.asList(slotMapping).indexOf(null);
            }

            for (int i = 0; i < slotMapping.length; i++) {
                final Object[] info = slotMapping[i];
                if (info == null) continue;
                try {
                    final ItemStack upgrade = (ItemStack) info[0];
                    final UpgradeRenderer renderer = (UpgradeRenderer) info[1];
                    matrix.pushPose();
                    matrix.translate(0.5f, 0.5f, 0.5f);
                    renderer.render(matrix, buffer, light, upgrade, mountPoints[i], robot, f);
                    matrix.popPose();
                } catch (Throwable e) {
                    OpenComputers.log.warn("Failed rendering equipped upgrade.", e);
                    robot.renderingErrored = true;
                }
            }
        }
        matrix.popPose();

        final String name = robot.name();
        // Vanilla's name tag render distance (64 blocks).
        if (Settings.get().robotLabels && !Strings.isNullOrEmpty(name) && dist <= 4096.0) {
            // This is pretty much copy-pasta from the entity's label renderer.
            final Font font = Minecraft.getInstance().font;
            final float scale = 1.6f / 60f;
            final int width = font.width(name);
            final int halfWidth = width / 2;
            final int bgColor = (int) (255f * Minecraft.getInstance().options.getBackgroundOpacity(0.25F)) << 24;

            matrix.translate(0, 0.8, 0);
            matrix.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            RenderState.mirrorScale(matrix, -scale, -scale, scale);

            font.drawInBatch((EventHandler.isItTime() ? ChatFormatting.OBFUSCATED.toString() : "") + name,
                -halfWidth, 0, -1, false, matrix.last().pose(), buffer, Font.DisplayMode.NORMAL, bgColor, light);
        }

        matrix.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
