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
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.common.tileentity.Rack;

public class RackRenderer implements BlockEntityRenderer<Rack> {
    private static final float vOffset = 2 / 16f;
    private static final float vSize = 3 / 16f;

    public RackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Rack rack, float dt, PoseStack stack, MultiBufferSource buffer, int light, int overlay) {
        RenderState.checkError(getClass().getName() + ".render: entering (aka: wasntme)");

        stack.pushPose();

        stack.translate(0.5, 0.5, 0.5);
        RenderUtil.rotateYaw(stack, rack.yaw());
        stack.translate(-0.5, 0.5, 0.505 - 0.5f / 16f);
        stack.scale(1, -1, 1);

        // Note: we manually sync the rack inventory for this to work.
        for (int i = 0; i < rack.getContainerSize(); i++) {
            if (!rack.getItem(i).isEmpty()) {
                final float v0 = vOffset + i * vSize;
                final float v1 = vOffset + (i + 1) * vSize;
                final RackMountableRenderEvent.TileEntity event = new RackMountableRenderEvent.TileEntity(rack, i, rack.lastData[i], stack, buffer, light, overlay, v0, v1);
                EventBus.INSTANCE.post(event);
            }
        }

        stack.popPose();

        RenderState.checkError(getClass().getName() + ".render: leaving");
    }
}
