package li.cil.oc.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.Textures;
import li.cil.oc.common.entity.Drone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DroneRenderer extends EntityRenderer<Drone> {
    public static final ModelQuadcopter model = new ModelQuadcopter();

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Drone entity, float yaw, float dt, PoseStack stack, MultiBufferSource buffer, int light) {
        final RenderType renderType = getRenderType(entity);
        if (renderType != null) {
            stack.pushPose();
            stack.translate(0, 2f / 16f, 0);
            final VertexConsumer builder = buffer.getBuffer(renderType);
            model.prepareMobModel(entity, 0, 0, dt);
            final float xRot = Mth.rotLerp(dt, entity.xRotO, entity.getXRot());
            final float yRot = Mth.rotLerp(dt, entity.yRotO, entity.getYRot());
            model.setupAnim(entity, 0, 0, entity.tickCount, yRot, xRot);
            model.renderToBuffer(stack, builder, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            stack.popPose();
        }
        super.render(entity, yaw, dt, stack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Drone entity) {
        return Textures.Model.Drone;
    }

    public RenderType getRenderType(Drone entity) {
        final Minecraft mc = Minecraft.getInstance();
        final ResourceLocation texture = getTextureLocation(entity);
        if (!entity.isInvisible()) return model.renderType(texture);
        else if (!entity.isInvisibleTo(mc.player)) return RenderType.itemEntityTranslucentCull(texture);
        else if (mc.shouldEntityAppearGlowing(entity)) return RenderType.outline(texture);
        else return null;
    }
}
