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
import li.cil.oc.common.tileentity.Disassembler;

public class DisassemblerRenderer implements BlockEntityRenderer<Disassembler> {
    public DisassemblerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Disassembler disassembler, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        if (disassembler.isActive) {
            stack.pushPose();

            stack.translate(0.5, 0.5, 0.5);
            stack.scale(1.0025f, -1.0025f, 1.0025f);
            stack.translate(-0.5f, -0.5f, -0.5f);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);

            RenderUtil.renderTopOverlay(stack, r, Textures.getSprite(Textures.Block.DisassemblerTopOn));
            RenderUtil.renderSideOverlays(stack, r, Textures.getSprite(Textures.Block.DisassemblerSideOn));

            stack.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
