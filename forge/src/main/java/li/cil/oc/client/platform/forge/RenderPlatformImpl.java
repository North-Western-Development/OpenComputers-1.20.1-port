package li.cil.oc.client.platform.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.platform.forge.EventBuses;
import li.cil.oc.OpenComputers;
import li.cil.oc.client.platform.RenderPlatform;
import li.cil.oc.client.renderer.block.ModelInitialization;
import li.cil.oc.client.renderer.block.SmartBlockModelBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

import java.lang.reflect.Field;
import java.util.HashMap;
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
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                callback.render(event.getPoseStack(), event.getPartialTick(), event.getCamera());
            }
        });
    }

    public static void registerBlockHighlightRenderer(RenderPlatform.BlockHighlightCallback callback) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderHighlightEvent.Block.class, event ->
            callback.render(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera(), event.getTarget()));
    }

    // ----------------------------------------------------------------------- //
    // Items.

    private static final Map<Item, ItemExtensions> extensions = new HashMap<>();

    public static void registerItemRenderer(Item item, RenderPlatform.ItemRenderCallback renderer) {
        extensionsFor(item).renderer = renderer;
    }

    public static void registerArmorModel(Item item, RenderPlatform.ArmorModelProvider provider, ResourceLocation texture) {
        // The texture is provided by the item itself on Forge (IForgeItem#getArmorTexture).
        extensionsFor(item).armorModel = provider;
    }

    private static synchronized ItemExtensions extensionsFor(Item item) {
        return extensions.computeIfAbsent(item, key -> {
            final ItemExtensions result = new ItemExtensions();
            try {
                // Forge only collects client extensions from Item#initializeClient
                // during item construction, which common code can't override.
                final Field field = Item.class.getDeclaredField("renderProperties");
                field.setAccessible(true);
                field.set(key, result);
            } catch (ReflectiveOperationException e) {
                OpenComputers.log.error("Failed registering client item extensions for " + key + ".", e);
            }
            return result;
        });
    }

    private static final class ItemExtensions implements IClientItemExtensions {
        RenderPlatform.ItemRenderCallback renderer;
        RenderPlatform.ArmorModelProvider armorModel;
        private BlockEntityWithoutLevelRenderer itemRenderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                return IClientItemExtensions.super.getCustomRenderer();
            }
            if (itemRenderer == null) {
                final Minecraft mc = Minecraft.getInstance();
                final RenderPlatform.ItemRenderCallback callback = renderer;
                itemRenderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                    @Override
                    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
                        callback.render(stack, context, poseStack, buffers, light, overlay);
                    }
                };
            }
            return itemRenderer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            if (armorModel != null) {
                final HumanoidModel<LivingEntity> model = armorModel.getModel(entity, stack, slot);
                if (model != null) {
                    ((HumanoidModel<LivingEntity>) original).copyPropertiesTo(model);
                    return model;
                }
            }
            return original;
        }
    }
}
