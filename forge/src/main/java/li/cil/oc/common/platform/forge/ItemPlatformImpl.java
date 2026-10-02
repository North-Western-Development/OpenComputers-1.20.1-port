package li.cil.oc.common.platform.forge;

import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.HoverBoots;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class ItemPlatformImpl {
    private ItemPlatformImpl() {
    }

    public static Drone createDrone(Item.Properties props) {
        return new Drone(props) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        final Object renderer = Drone.customRenderer != null ? Drone.customRenderer.get() : null;
                        if (renderer instanceof BlockEntityWithoutLevelRenderer bewlr) return bewlr;
                        return Minecraft.getInstance().getItemRenderer().getBlockEntityRenderer();
                    }
                });
            }
        };
    }

    public static HoverBoots createHoverBoots(Item.Properties props) {
        return new HoverBoots(props) {
            @Override
            public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
                return armorTexture(stack, entity, slot, type);
            }

            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
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
                });
            }
        };
    }
}
