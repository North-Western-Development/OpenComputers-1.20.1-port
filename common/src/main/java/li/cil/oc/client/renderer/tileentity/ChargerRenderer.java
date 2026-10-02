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
import li.cil.oc.common.tileentity.Charger;

public class ChargerRenderer implements BlockEntityRenderer<Charger> {
    public ChargerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Charger charger, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        if (charger.chargeSpeed > 0) {
            stack.pushPose();

            stack.translate(0.5, 0.5, 0.5);
            RenderUtil.rotateYaw(stack, charger.yaw());
            stack.translate(-0.5f, 0.5f, 0.5f);
            stack.scale(1, -1, 1);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            final Matrix4f pose = stack.last().pose();

            {
                final float inverse = 1 - (float) charger.chargeSpeed;
                final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.ChargerFrontOn);
                r.vertex(pose, 0, 1, 0.005f).uv(icon.getU0(), icon.getV1()).endVertex();
                r.vertex(pose, 1, 1, 0.005f).uv(icon.getU1(), icon.getV1()).endVertex();
                r.vertex(pose, 1, inverse, 0.005f).uv(icon.getU1(), icon.getV(inverse * 16)).endVertex();
                r.vertex(pose, 0, inverse, 0.005f).uv(icon.getU0(), icon.getV(inverse * 16)).endVertex();
            }

            if (charger.hasPower) {
                final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.ChargerSideOn);

                r.vertex(pose, -0.005f, 1, -1).uv(icon.getU0(), icon.getV1()).endVertex();
                r.vertex(pose, -0.005f, 1, 0).uv(icon.getU1(), icon.getV1()).endVertex();
                r.vertex(pose, -0.005f, 0, 0).uv(icon.getU1(), icon.getV0()).endVertex();
                r.vertex(pose, -0.005f, 0, -1).uv(icon.getU0(), icon.getV0()).endVertex();

                r.vertex(pose, 1, 1, -1.005f).uv(icon.getU0(), icon.getV1()).endVertex();
                r.vertex(pose, 0, 1, -1.005f).uv(icon.getU1(), icon.getV1()).endVertex();
                r.vertex(pose, 0, 0, -1.005f).uv(icon.getU1(), icon.getV0()).endVertex();
                r.vertex(pose, 1, 0, -1.005f).uv(icon.getU0(), icon.getV0()).endVertex();

                r.vertex(pose, 1.005f, 1, 0).uv(icon.getU0(), icon.getV1()).endVertex();
                r.vertex(pose, 1.005f, 1, -1).uv(icon.getU1(), icon.getV1()).endVertex();
                r.vertex(pose, 1.005f, 0, -1).uv(icon.getU1(), icon.getV0()).endVertex();
                r.vertex(pose, 1.005f, 0, 0).uv(icon.getU0(), icon.getV0()).endVertex();
            }

            stack.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
