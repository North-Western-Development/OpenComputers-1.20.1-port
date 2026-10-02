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
import com.mojang.math.Axis;
import li.cil.oc.common.tileentity.Assembler;

public class AssemblerRenderer implements BlockEntityRenderer<Assembler> {
    public AssemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Assembler assembler, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);

        final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);

        {
            final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.AssemblerTopOn);
            final Matrix4f pose = stack.last().pose();
            r.vertex(pose, -0.5f, 0.55f, 0.5f).uv(icon.getU0(), icon.getV1()).endVertex();
            r.vertex(pose, 0.5f, 0.55f, 0.5f).uv(icon.getU1(), icon.getV1()).endVertex();
            r.vertex(pose, 0.5f, 0.55f, -0.5f).uv(icon.getU1(), icon.getV0()).endVertex();
            r.vertex(pose, -0.5f, 0.55f, -0.5f).uv(icon.getU0(), icon.getV0()).endVertex();
        }

        final float indent = 6 / 16f + 0.005f;
        for (int i = 0; i < 4; i++) {
            final Matrix4f pose = stack.last().pose();
            if (assembler.isAssembling()) {
                final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.AssemblerSideAssembling);
                r.vertex(pose, indent, 0.5f, -indent).uv(icon.getU((0.5f - indent) * 16), icon.getV1()).endVertex();
                r.vertex(pose, indent, 0.5f, indent).uv(icon.getU((0.5f + indent) * 16), icon.getV1()).endVertex();
                r.vertex(pose, indent, -0.5f, indent).uv(icon.getU((0.5f + indent) * 16), icon.getV0()).endVertex();
                r.vertex(pose, indent, -0.5f, -indent).uv(icon.getU((0.5f - indent) * 16), icon.getV0()).endVertex();
            }

            {
                final TextureAtlasSprite icon = Textures.getSprite(Textures.Block.AssemblerSideOn);
                r.vertex(pose, 0.5005f, 0.5f, -0.5f).uv(icon.getU0(), icon.getV1()).endVertex();
                r.vertex(pose, 0.5005f, 0.5f, 0.5f).uv(icon.getU1(), icon.getV1()).endVertex();
                r.vertex(pose, 0.5005f, -0.5f, 0.5f).uv(icon.getU1(), icon.getV0()).endVertex();
                r.vertex(pose, 0.5005f, -0.5f, -0.5f).uv(icon.getU0(), icon.getV0()).endVertex();
            }

            stack.mulPose(Axis.YP.rotationDegrees(90));
        }

        stack.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
