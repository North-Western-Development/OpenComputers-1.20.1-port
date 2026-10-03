package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import com.mojang.math.Axis;
import li.cil.oc.common.tileentity.DiskDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DiskDriveRenderer implements BlockEntityRenderer<DiskDrive> {
    public DiskDriveRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DiskDrive drive, float dt, PoseStack matrix, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        matrix.pushPose();

        matrix.translate(0.5, 0.5, 0.5);
        RenderUtil.rotateYaw(matrix, drive.yaw());

        final ItemStack stack = drive.getItem(0);
        if (!stack.isEmpty()) {
            matrix.pushPose();
            matrix.translate(0, 3.5f / 16, 6 / 16f);
            matrix.mulPose(Axis.XN.rotationDegrees(90));
            matrix.scale(0.5f, 0.5f, 0.5f);

            // Light the item like the block face it sticks out of, not like the (dark) inside of the block.
            final int itemLight = drive.getLevel() != null ? LevelRenderer.getLightColor(drive.getLevel(), drive.getBlockPos().relative(drive.facing())) : light;
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, itemLight, overlay, matrix, buffer, drive.getLevel(), 0);
            matrix.popPose();
        }

        if (System.currentTimeMillis() - drive.lastAccess < 400 && drive.getLevel() != null && drive.getLevel().random.nextDouble() > 0.1) {
            matrix.translate(-0.5, 0.5, 0.505);
            matrix.scale(1, -1, 1);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            RenderUtil.renderFrontOverlay(matrix, Textures.Block.DiskDriveFrontActivity, r);
        }

        matrix.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
