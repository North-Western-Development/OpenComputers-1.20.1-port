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
import li.cil.oc.common.tileentity.NetSplitter;

public class NetSplitterRenderer implements BlockEntityRenderer<NetSplitter> {
    public NetSplitterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NetSplitter splitter, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        if (RenderUtil.anySideOpen(splitter::isSideOpen)) {
            stack.pushPose();

            stack.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(stack, 1.0025f, -1.0025f, 1.0025f);
            stack.translate(-0.5f, -0.5f, -0.5f);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            final TextureAtlasSprite sideActivity = Textures.getSprite(Textures.Block.NetSplitterOn);
            RenderUtil.renderOpenSideOverlays(stack, r, sideActivity, splitter::isSideOpen);

            stack.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
