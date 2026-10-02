package li.cil.oc.common.platform.forge;

import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.HoverBoots;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public final class ItemPlatformImpl {
    private ItemPlatformImpl() {
    }

    // initializeClient is only called by Forge on the physical client; the client
    // parts live in li.cil.oc.client.platform.forge.ClientItemExtensions.

    public static Drone createDrone(Item.Properties props) {
        return new Drone(props) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                consumer.accept(li.cil.oc.client.platform.forge.ClientItemExtensions.drone());
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
                consumer.accept(li.cil.oc.client.platform.forge.ClientItemExtensions.hoverBoots());
            }
        };
    }
}
