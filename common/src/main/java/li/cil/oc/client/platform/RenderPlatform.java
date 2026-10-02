package li.cil.oc.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.Camera;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side rendering hooks that need loader specific code. Implemented in
 * {@code li.cil.oc.client.platform.forge.RenderPlatformImpl} and
 * {@code li.cil.oc.client.platform.fabric.RenderPlatformImpl}.
 * <p>
 * All registration methods are called once from
 * {@link li.cil.oc.client.renderer.ClientRenderers}.
 */
public final class RenderPlatform {
    private RenderPlatform() {
    }

    // ----------------------------------------------------------------------- //
    // Callback types.

    @FunctionalInterface
    public interface LevelRenderCallback {
        /**
         * Called after the level was rendered. The pose stack contains the
         * camera rotation only (world coordinates must be offset by the camera
         * position).
         */
        void render(PoseStack poseStack, float partialTick, Camera camera);
    }

    @FunctionalInterface
    public interface BlockHighlightCallback {
        /**
         * Called when the block outline of the block the player is looking at is
         * rendered. The pose stack contains the camera rotation only.
         */
        void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, BlockHitResult hit);
    }

    @FunctionalInterface
    public interface ItemRenderCallback {
        /**
         * Renders an item whose model is {@code builtin/entity}. The pose stack is
         * already transformed by the model's display transform and offset by
         * (-0.5, -0.5, -0.5), like for vanilla's block entity item renderer.
         */
        void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay);
    }

    @FunctionalInterface
    public interface ArmorModelProvider {
        /**
         * Returns the model to render for the given armor piece, or null to use
         * the default.
         */
        @Nullable
        HumanoidModel<LivingEntity> getModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot);
    }

    // ----------------------------------------------------------------------- //

    /**
     * Hooks model loading/baking so that
     * {@link li.cil.oc.client.renderer.block.ModelInitialization#additionalModels()}
     * get loaded and every baked top level model is passed through
     * {@link li.cil.oc.client.renderer.block.ModelInitialization#modifyBakedModel}.
     * Results that are {@link li.cil.oc.client.renderer.block.SmartBlockModelBase}s
     * must be wrapped so that their block quads are computed with the block
     * entity at the rendered position
     * ({@link li.cil.oc.client.renderer.block.SmartBlockModelBase#getBlockQuads}).
     */
    @ExpectPlatform
    public static void registerModelHooks() {
        throw new AssertionError();
    }

    /**
     * Get a baked model loaded through
     * {@link li.cil.oc.client.renderer.block.ModelInitialization#registerAdditionalModel}.
     */
    @ExpectPlatform
    public static BakedModel getModel(ResourceLocation location) {
        throw new AssertionError();
    }

    /**
     * Registers a callback invoked after the level has been rendered (Forge:
     * {@code RenderLevelStageEvent} AFTER_LEVEL, Fabric: {@code WorldRenderEvents.LAST}).
     */
    @ExpectPlatform
    public static void registerLevelRenderer(LevelRenderCallback callback) {
        throw new AssertionError();
    }

    /**
     * Registers a callback invoked when a block highlight is drawn (Forge:
     * {@code RenderHighlightEvent.Block}, Fabric: {@code WorldRenderEvents.BEFORE_BLOCK_OUTLINE}).
     */
    @ExpectPlatform
    public static void registerBlockHighlightRenderer(BlockHighlightCallback callback) {
        throw new AssertionError();
    }

    /**
     * Registers a renderer for an item using a {@code builtin/entity} model
     * (Forge: {@code IClientItemExtensions#getCustomRenderer}, Fabric:
     * {@code BuiltinItemRendererRegistry}). Must be called after items are registered.
     */
    @ExpectPlatform
    public static void registerItemRenderer(Item item, ItemRenderCallback renderer) {
        throw new AssertionError();
    }

    /**
     * Registers a custom armor model (Forge: {@code IClientItemExtensions#getHumanoidArmorModel},
     * Fabric: {@code ArmorRenderer}). The texture is used on Fabric; on Forge the
     * item has to provide it via {@code getArmorTexture}. Must be called after
     * items are registered.
     */
    @ExpectPlatform
    public static void registerArmorModel(Item item, ArmorModelProvider provider, ResourceLocation texture) {
        throw new AssertionError();
    }
}
