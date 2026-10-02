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
import li.cil.oc.common.tileentity.Transposer;

public class TransposerRenderer implements BlockEntityRenderer<Transposer> {
    public TransposerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Transposer transposer, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        final float activity = Math.max(0, 1 - (System.currentTimeMillis() - transposer.lastOperation) / 1000.0f);
        if (activity > 0) {
            stack.pushPose();

            stack.translate(0.5, 0.5, 0.5);
            stack.scale(1.0025f, -1.0025f, 1.0025f);
            stack.translate(-0.5f, -0.5f, -0.5f);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY_COLOR);
            final Matrix4f pose = stack.last().pose();

            final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.TransposerOn);
            r.vertex(pose, 0, 1, 0).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, 1, 1, 0).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV0()).endVertex();
            r.vertex(pose, 1, 1, 1).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV1()).endVertex();
            r.vertex(pose, 0, 1, 1).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV1()).endVertex();

            r.vertex(pose, 0, 0, 0).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV1()).endVertex();
            r.vertex(pose, 0, 0, 1).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, 1, 0, 1).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV0()).endVertex();
            r.vertex(pose, 1, 0, 0).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV1()).endVertex();

            r.vertex(pose, 1, 1, 0).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV1()).endVertex();
            r.vertex(pose, 0, 1, 0).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV1()).endVertex();
            r.vertex(pose, 0, 0, 0).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, 1, 0, 0).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV0()).endVertex();

            r.vertex(pose, 0, 1, 1).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV1()).endVertex();
            r.vertex(pose, 1, 1, 1).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV1()).endVertex();
            r.vertex(pose, 1, 0, 1).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, 0, 0, 1).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV0()).endVertex();

            r.vertex(pose, 0, 1, 0).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV1()).endVertex();
            r.vertex(pose, 0, 1, 1).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV1()).endVertex();
            r.vertex(pose, 0, 0, 1).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, 0, 0, 0).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV0()).endVertex();

            r.vertex(pose, 1, 1, 1).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV1()).endVertex();
            r.vertex(pose, 1, 1, 0).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV1()).endVertex();
            r.vertex(pose, 1, 0, 0).color(1, 1, 1, activity).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, 1, 0, 1).color(1, 1, 1, activity).uv(icon.getU0(), icon.getV0()).endVertex();

            stack.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
