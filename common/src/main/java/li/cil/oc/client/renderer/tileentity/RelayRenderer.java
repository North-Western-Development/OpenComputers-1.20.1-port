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
import li.cil.oc.common.tileentity.Relay;

public class RelayRenderer implements BlockEntityRenderer<Relay> {
    public RelayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Relay relay, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        final double activity = Math.max(0, 1 - (System.currentTimeMillis() - relay.lastMessage) / 1000.0);
        if (activity > 0) {
            stack.pushPose();

            stack.translate(0.5, 0.5, 0.5);
            stack.scale(1.0025f, -1.0025f, 1.0025f);
            stack.translate(-0.5f, -0.5f, -0.5f);

            final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);
            RenderUtil.renderSideOverlays(stack, r, Textures.getSprite(Textures.Block.SwitchSideOn));

            stack.popPose();
        }

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
