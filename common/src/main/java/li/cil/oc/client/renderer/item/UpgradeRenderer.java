package li.cil.oc.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName;
import li.cil.oc.api.event.RobotRenderEvent.MountPoint;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.client.renderer.tileentity.RenderUtil;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Set;

public final class UpgradeRenderer {
    private UpgradeRenderer() {
    }

    private static ItemInfo craftingUpgrade() {
        return Items.get(Constants.ItemName.CraftingUpgrade);
    }

    private static ItemInfo generatorUpgrade() {
        return Items.get(Constants.ItemName.GeneratorUpgrade);
    }

    private static ItemInfo inventoryUpgrade() {
        return Items.get(Constants.ItemName.InventoryUpgrade);
    }

    /**
     * Robot upgrade renderer used for OC's own items ({@link li.cil.oc.common.item.traits.SimpleItem}),
     * which no longer implement the API interface themselves (that would make the
     * server-side item classes reference client types).
     */
    public static final li.cil.oc.api.driver.item.UpgradeRenderer DEFAULT = new li.cil.oc.api.driver.item.UpgradeRenderer() {
        @Override
        public String computePreferredMountPoint(ItemStack stack, li.cil.oc.api.internal.Robot robot, Set<String> availableMountPoints) {
            return preferredMountPoint(stack, availableMountPoints);
        }

        @Override
        public void render(PoseStack matrix, MultiBufferSource buffer, ItemStack stack, MountPoint mountPoint, li.cil.oc.api.internal.Robot robot, float pt) {
            UpgradeRenderer.render(matrix, buffer, stack, mountPoint);
        }
    };

    /** The API renderer for the given upgrade, or null if it has none. */
    public static li.cil.oc.api.driver.item.UpgradeRenderer forStack(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.api.driver.item.UpgradeRenderer renderer) return renderer;
        if (stack.getItem() instanceof li.cil.oc.common.item.traits.SimpleItem) return DEFAULT;
        return null;
    }

    public static String preferredMountPoint(ItemStack stack, Set<String> availableMountPoints) {
        final ItemInfo descriptor = Items.get(stack);

        if (descriptor != null && (descriptor == craftingUpgrade() || descriptor == generatorUpgrade() || descriptor == inventoryUpgrade())) {
            if (descriptor == generatorUpgrade() && availableMountPoints.contains(MountPointName.BottomBack)) return MountPointName.BottomBack;
            else if (descriptor == inventoryUpgrade() && availableMountPoints.contains(MountPointName.TopBack)) return MountPointName.TopBack;
            else return MountPointName.Any;
        } else return MountPointName.None;
    }

    public static boolean canRender(ItemStack stack) {
        final ItemInfo descriptor = Items.get(stack);

        return descriptor != null && (descriptor == craftingUpgrade() || descriptor == generatorUpgrade() || descriptor == inventoryUpgrade());
    }

    public static void render(PoseStack matrix, MultiBufferSource buffer, ItemStack stack, MountPoint mountPoint) {
        final ItemInfo descriptor = Items.get(stack);
        if (descriptor == null) return;

        if (descriptor == craftingUpgrade()) {
            drawSimpleBlock(matrix, buffer.getBuffer(RenderTypes.UPGRADE_CRAFTING), mountPoint, 0);

            RenderState.checkError(UpgradeRenderer.class.getName() + ".renderItem: crafting upgrade");
        } else if (descriptor == generatorUpgrade()) {
            final boolean active = li.cil.oc.integration.opencomputers.Item.dataTagStatic(stack).getInt("remainingTicks") > 0;
            drawSimpleBlock(matrix, buffer.getBuffer(RenderTypes.UPGRADE_GENERATOR), mountPoint, active ? 0.5f : 0);

            RenderState.checkError(UpgradeRenderer.class.getName() + ".renderItem: generator upgrade");
        } else if (descriptor == inventoryUpgrade()) {
            drawSimpleBlock(matrix, buffer.getBuffer(RenderTypes.UPGRADE_INVENTORY), mountPoint, 0);

            RenderState.checkError(UpgradeRenderer.class.getName() + ".renderItem: inventory upgrade");
        }
    }

    private static final float minX = -0.1f, minY = -0.1f, minZ = -0.1f;
    private static final float maxX = 0.1f, maxY = 0.1f, maxZ = 0.1f;

    private static void vertex(VertexConsumer r, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        r.vertex(pose, x, y, z)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, nx, ny, nz)
            .endVertex();
    }

    private static void drawSimpleBlock(PoseStack stack, VertexConsumer r, MountPoint mountPoint, float frontOffset) {
        RenderUtil.rotate(stack, mountPoint.rotation.w(), mountPoint.rotation.x(), mountPoint.rotation.y(), mountPoint.rotation.z());
        stack.translate(mountPoint.offset.x(), mountPoint.offset.y(), mountPoint.offset.z());

        final Matrix4f pose = stack.last().pose();
        final Matrix3f normal = stack.last().normal();

        // Front.
        vertex(r, pose, normal, minX, minY, maxZ, frontOffset, 0.5f, 0, 0, 1);
        vertex(r, pose, normal, maxX, minY, maxZ, frontOffset + 0.5f, 0.5f, 0, 0, 1);
        vertex(r, pose, normal, maxX, maxY, maxZ, frontOffset + 0.5f, 0, 0, 0, 1);
        vertex(r, pose, normal, minX, maxY, maxZ, frontOffset, 0, 0, 0, 1);

        // Top.
        vertex(r, pose, normal, maxX, maxY, maxZ, 1, 0.5f, 0, 1, 0);
        vertex(r, pose, normal, maxX, maxY, minZ, 1, 1, 0, 1, 0);
        vertex(r, pose, normal, minX, maxY, minZ, 0.5f, 1, 0, 1, 0);
        vertex(r, pose, normal, minX, maxY, maxZ, 0.5f, 0.5f, 0, 1, 0);

        // Bottom.
        vertex(r, pose, normal, minX, minY, maxZ, 0.5f, 0.5f, 0, -1, 0);
        vertex(r, pose, normal, minX, minY, minZ, 0.5f, 1, 0, -1, 0);
        vertex(r, pose, normal, maxX, minY, minZ, 1, 1, 0, -1, 0);
        vertex(r, pose, normal, maxX, minY, maxZ, 1, 0.5f, 0, -1, 0);

        // Left.
        vertex(r, pose, normal, maxX, maxY, maxZ, 0, 0.5f, 1, 0, 0);
        vertex(r, pose, normal, maxX, minY, maxZ, 0, 1, 1, 0, 0);
        vertex(r, pose, normal, maxX, minY, minZ, 0.5f, 1, 1, 0, 0);
        vertex(r, pose, normal, maxX, maxY, minZ, 0.5f, 0.5f, 1, 0, 0);

        // Right.
        vertex(r, pose, normal, minX, minY, maxZ, 0, 1, -1, 0, 0);
        vertex(r, pose, normal, minX, maxY, maxZ, 0, 0.5f, -1, 0, 0);
        vertex(r, pose, normal, minX, maxY, minZ, 0.5f, 0.5f, -1, 0, 0);
        vertex(r, pose, normal, minX, minY, minZ, 0.5f, 1, -1, 0, 0);
    }
}
