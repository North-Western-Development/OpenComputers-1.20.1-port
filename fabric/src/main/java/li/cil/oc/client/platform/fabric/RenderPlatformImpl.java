package li.cil.oc.client.platform.fabric;

import li.cil.oc.client.platform.RenderPlatform;
import li.cil.oc.client.renderer.block.ModelInitialization;
import li.cil.oc.client.renderer.block.SmartBlockModelBase;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class RenderPlatformImpl {
    private RenderPlatformImpl() {
    }

    // ----------------------------------------------------------------------- //
    // Models.

    public static void registerModelHooks() {
        ModelLoadingPlugin.register(context -> {
            context.addModels(ModelInitialization.additionalModels());
            context.modifyModelAfterBake().register((model, modelContext) -> {
                final BakedModel replaced = ModelInitialization.modifyBakedModel(modelContext.id(), model);
                if (replaced != model && replaced instanceof SmartBlockModelBase smart) {
                    return new FabricSmartBakedModel(smart);
                }
                return replaced;
            });
        });
    }

    public static BakedModel getModel(ResourceLocation location) {
        return ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(location);
    }

    // ----------------------------------------------------------------------- //
    // World rendering.

    public static void registerLevelRenderer(RenderPlatform.LevelRenderCallback callback) {
        WorldRenderEvents.LAST.register(context ->
            callback.render(context.matrixStack(), context.tickDelta(), context.camera()));
    }

    public static void registerBlockHighlightRenderer(RenderPlatform.BlockHighlightCallback callback) {
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hit) -> {
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK && context.consumers() != null) {
                callback.render(context.matrixStack(), context.consumers(), context.camera(), blockHit);
            }
            return true;
        });
    }

    // ----------------------------------------------------------------------- //
    // Items.

    public static void registerItemRenderer(Item item, RenderPlatform.ItemRenderCallback renderer) {
        final BuiltinItemRendererRegistry.DynamicItemRenderer dynamicRenderer = renderer::render;
        BuiltinItemRendererRegistry.INSTANCE.register((ItemLike) item, dynamicRenderer);
    }

    public static void registerArmorModel(Item item, RenderPlatform.ArmorModelProvider provider, ResourceLocation texture) {
        ArmorRenderer.register((poseStack, buffers, stack, entity, slot, light, contextModel) -> {
            final HumanoidModel<LivingEntity> model = provider.getModel(entity, stack, slot);
            if (model != null) {
                contextModel.copyPropertiesTo(model);
                ArmorRenderer.renderPart(poseStack, buffers, light, stack, model, texture);
            }
        }, item);
    }
}
