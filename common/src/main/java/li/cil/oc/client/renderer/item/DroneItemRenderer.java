package li.cil.oc.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.client.renderer.block.DroneModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the drone item (whose model is {@code builtin/entity}) using the
 * geometry of {@link DroneModel}.
 */
public final class DroneItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static DroneItemRenderer instance;

    /**
     * Lazily created, since the block entity render dispatcher does not exist yet
     * during mod initialization.
     */
    public static synchronized DroneItemRenderer instance() {
        if (instance == null) {
            final Minecraft mc = Minecraft.getInstance();
            instance = new DroneItemRenderer(mc);
        }
        return instance;
    }

    private final RandomSource random = RandomSource.create();

    private DroneItemRenderer(Minecraft mc) {
        super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        render(stack, context, poseStack, buffers, light, overlay);
    }

    /**
     * Renders the drone item. The pose stack is expected to be set up as for
     * block entity item renderers (display transform applied, offset by -0.5).
     */
    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        final DroneModel model = DroneModel.INSTANCE;
        poseStack.pushPose();

        // The builtin/entity item model has no display transforms of its own,
        // apply the ones the old baked drone model had.
        poseStack.translate(0.5f, 0.5f, 0.5f);
        model.getTransforms().getTransform(context).apply(false, poseStack);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        final VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        for (BakedQuad quad : model.getQuads(null, null, instance().random)) {
            consumer.putBulkData(poseStack.last(), quad, 1, 1, 1, light, overlay);
        }

        poseStack.popPose();
    }
}
