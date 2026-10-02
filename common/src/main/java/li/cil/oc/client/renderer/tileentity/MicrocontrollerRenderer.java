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
import li.cil.oc.common.tileentity.Microcontroller;

public class MicrocontrollerRenderer implements BlockEntityRenderer<Microcontroller> {
    public MicrocontrollerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Microcontroller mcu, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);
        RenderUtil.rotateYaw(stack, mcu.yaw());
        stack.translate(-0.5, 0.5, 0.505);
        stack.scale(1, -1, 1);

        final VertexConsumer r = buffer.getBuffer(RenderTypes.BLOCK_OVERLAY);

        RenderUtil.renderFrontOverlay(stack, Textures.Block.MicrocontrollerFrontLight, r);

        if (mcu.isRunning()) {
            RenderUtil.renderFrontOverlay(stack, Textures.Block.MicrocontrollerFrontOn, r);
        } else if (mcu.hasErrored() && RenderUtil.shouldShowErrorLight(mcu.hashCode())) {
            RenderUtil.renderFrontOverlay(stack, Textures.Block.MicrocontrollerFrontError, r);
        }

        stack.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
