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
import li.cil.oc.common.tileentity.Adapter;

public class AdapterRenderer implements BlockEntityRenderer<Adapter> {
    public AdapterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Adapter adapter, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        if (RenderUtil.anySideOpen(adapter::isSideOpen)) {
            stack.pushPose();

            stack.translate(0.5, 0.5, 0.5);
            RenderState.mirrorScale(stack, 1.0025f, -1.0025f, 1.0025f);
            stack.translate(-0.5f, -0.5f, -0.5f);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            final TextureAtlasSprite sideActivity = Textures.getSprite(Textures.Block.AdapterOn);
            RenderUtil.renderOpenSideOverlays(stack, r, sideActivity, adapter::isSideOpen);

            stack.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
