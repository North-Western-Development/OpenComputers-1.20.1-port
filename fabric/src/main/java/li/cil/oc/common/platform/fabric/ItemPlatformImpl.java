package li.cil.oc.common.platform.fabric;

import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.HoverBoots;
import net.minecraft.world.item.Item;

public final class ItemPlatformImpl {
    private ItemPlatformImpl() {
    }

    public static Drone createDrone(Item.Properties props) {
        // The client registers the builtin/entity renderer through Fabric's
        // BuiltinItemRendererRegistry (see Drone#customRenderer).
        return new Drone(props);
    }

    public static HoverBoots createHoverBoots(Item.Properties props) {
        // The client registers an ArmorRenderer (Fabric API) using HoverBoots#armorModel / ARMOR_TEXTURE.
        return new HoverBoots(props);
    }
}
