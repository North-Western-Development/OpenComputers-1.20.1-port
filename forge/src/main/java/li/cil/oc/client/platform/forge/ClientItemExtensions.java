package li.cil.oc.client.platform.forge;

import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.HoverBoots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Client item extensions for the Forge items created in
 * {@link li.cil.oc.common.platform.forge.ItemPlatformImpl}. Separate class so the
 * (server loaded) item classes don't reference client types.
 */
public final class ClientItemExtensions {
    private ClientItemExtensions() {
    }

    public static IClientItemExtensions drone() {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                final Object renderer = Drone.customRenderer != null ? Drone.customRenderer.get() : null;
                if (renderer instanceof BlockEntityWithoutLevelRenderer bewlr) return bewlr;
                return Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer();
            }
        };
    }

    public static IClientItemExtensions hoverBoots() {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (equipmentSlot == EquipmentSlot.FEET && HoverBoots.armorModel != null) {
                    final Object model = HoverBoots.armorModel.apply(itemStack);
                    if (model instanceof HumanoidModel<?> humanoid) {
                        // Follow the wearer's leg animation.
                        @SuppressWarnings("unchecked")
                        final HumanoidModel<LivingEntity> source = (HumanoidModel<LivingEntity>) original;
                        @SuppressWarnings("unchecked")
                        final HumanoidModel<LivingEntity> target = (HumanoidModel<LivingEntity>) humanoid;
                        source.copyPropertiesTo(target);
                        return humanoid;
                    }
                }
                return original;
            }
        };
    }
}
