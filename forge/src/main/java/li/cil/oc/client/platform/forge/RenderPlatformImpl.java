package li.cil.oc.client.platform.forge;

import dev.architectury.platform.forge.EventBuses;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.platform.RenderPlatform;
import li.cil.oc.client.renderer.block.ModelInitialization;
import li.cil.oc.client.renderer.block.SmartBlockModelBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.Map;

public final class RenderPlatformImpl {
    private RenderPlatformImpl() {
    }

    // ----------------------------------------------------------------------- //
    // Models.

    public static void registerModelHooks() {
        final IEventBus modBus = EventBuses.getModEventBus(OpenComputers.ID)
            .orElseThrow(() -> new IllegalStateException("Mod event bus not registered."));

        modBus.addListener(EventPriority.NORMAL, false, ModelEvent.RegisterAdditional.class, event -> {
            for (ResourceLocation location : ModelInitialization.additionalModels()) {
                event.register(location);
            }
        });

        modBus.addListener(EventPriority.NORMAL, false, ModelEvent.ModifyBakingResult.class, event -> {
            final Map<ResourceLocation, BakedModel> models = event.getModels();
            for (Map.Entry<ResourceLocation, BakedModel> entry : models.entrySet()) {
                final BakedModel original = entry.getValue();
                final BakedModel replaced = ModelInitialization.modifyBakedModel(entry.getKey(), original);
                if (replaced != original && replaced != null) {
                    entry.setValue(replaced instanceof SmartBlockModelBase smart ? new ForgeSmartBakedModel(smart) : replaced);
                }
            }
        });
    }

    public static BakedModel getModel(ResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    // ----------------------------------------------------------------------- //
    // World rendering.

    public static void registerLevelRenderer(RenderPlatform.LevelRenderCallback callback) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderLevelStageEvent.class, event -> {
            // AFTER_WEATHER is the last stage that gets the level pose stack (with the camera
            // rotation); AFTER_LEVEL gets the projection pose stack, so things drawn there with
            // it end up outside the view. At this stage the model-view matrix also holds the
            // camera rotation (pushed for particles / weather), so reset it to identity like
            // Fabric's WorldRenderEvents.LAST, or the rotation would be applied twice.
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                final com.mojang.blaze3d.vertex.PoseStack modelView = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
                modelView.pushPose();
                modelView.setIdentity();
                com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
                try {
                    callback.render(event.getPoseStack(), event.getPartialTick(), event.getCamera());
                } finally {
                    modelView.popPose();
                    com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
                }
            }
        });
    }

    public static void registerBlockHighlightRenderer(RenderPlatform.BlockHighlightCallback callback) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderHighlightEvent.Block.class, event ->
            callback.render(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera(), event.getTarget()));
    }

    // ----------------------------------------------------------------------- //
    // Items.

    // On Forge, custom item renderers / armor models must be provided by the item
    // itself (IClientItemExtensions from Item#initializeClient). OC's Forge items
    // do that in li.cil.oc.common.platform.forge.ItemPlatformImpl, reading the
    // client hooks Drone.customRenderer / HoverBoots.armorModel, which
    // ClientRenderers sets on both loaders. Nothing to do here.

    public static void registerItemRenderer(Item item, RenderPlatform.ItemRenderCallback renderer) {
    }

    public static void registerArmorModel(Item item, RenderPlatform.ArmorModelProvider provider, ResourceLocation texture) {
    }
}
