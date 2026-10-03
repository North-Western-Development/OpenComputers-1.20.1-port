package li.cil.oc.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.Textures;
import li.cil.oc.client.renderer.RenderTypes;
import li.cil.oc.util.RenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import li.cil.oc.common.tileentity.Raid;

public class RaidRenderer implements BlockEntityRenderer<Raid> {
    private static final float u1 = 2 / 16f;
    private static final float fs = 4 / 16f;

    public RaidRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Raid raid, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);
        RenderUtil.rotateYaw(stack, raid.yaw());
        stack.translate(-0.5, 0.5, 0.505);
        RenderState.mirrorScale(stack, 1, -1, 1);

        final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);

        {
            final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.RaidFrontError);
            for (int slot = 0; slot < raid.getContainerSize(); slot++) {
                if (!raid.presence[slot]) {
                    renderSlot(stack, r, slot, icon);
                }
            }
        }

        {
            final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.RaidFrontActivity);
            for (int slot = 0; slot < raid.getContainerSize(); slot++) {
                if (System.currentTimeMillis() - raid.lastAccess < 400 && raid.getLevel() != null && raid.getLevel().random.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getContainerSize()) {
                    renderSlot(stack, r, slot, icon);
                }
            }
        }

        stack.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }

    private void renderSlot(PoseStack stack, VertexConsumer r, int slot, TextureAtlasSprite icon) {
        final float l = u1 + slot * fs;
        final float h = u1 + (slot + 1) * fs;
        final Matrix4f pose = stack.last().pose();
        r.vertex(pose, l, 1, 0).uv(icon.getU(l * 16), icon.getV1()).endVertex();
        r.vertex(pose, h, 1, 0).uv(icon.getU(h * 16), icon.getV1()).endVertex();
        r.vertex(pose, h, 0, 0).uv(icon.getU(h * 16), icon.getV0()).endVertex();
        r.vertex(pose, l, 0, 0).uv(icon.getU(l * 16), icon.getV0()).endVertex();
    }
}
