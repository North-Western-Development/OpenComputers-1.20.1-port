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
import li.cil.oc.common.tileentity.Case;

public class CaseRenderer implements BlockEntityRenderer<Case> {
    public CaseRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Case computer, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);
        RenderUtil.rotateYaw(stack, computer.yaw());
        stack.translate(-0.5, 0.5, 0.505);
        stack.scale(1, -1, 1);

        if (computer.isRunning()) {
            RenderUtil.renderFrontOverlay(stack, Textures.Block.CaseFrontOn, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY));
            if (System.currentTimeMillis() - computer.lastFileSystemAccess < 400 && computer.getLevel() != null && computer.getLevel().random.nextDouble() > 0.1) {
                RenderUtil.renderFrontOverlay(stack, Textures.Block.CaseFrontActivity, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY));
            }
        } else if (computer.hasErrored() && RenderUtil.shouldShowErrorLight(computer.hashCode())) {
            RenderUtil.renderFrontOverlay(stack, Textures.Block.CaseFrontError, buffer.getBuffer(RenderTypes.BLOCK_OVERLAY));
        }

        stack.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
