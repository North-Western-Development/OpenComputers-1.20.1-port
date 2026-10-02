package li.cil.oc.common.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.HoverBoots;
import net.minecraft.world.item.Item;

/**
 * Loader specific item construction. Implemented in
 * {@code li.cil.oc.common.platform.forge.ItemPlatformImpl} and
 * {@code li.cil.oc.common.platform.fabric.ItemPlatformImpl}.
 */
public final class ItemPlatform {
    private ItemPlatform() {
    }

    /**
     * Creates the drone item. On Forge this is a subclass hooking
     * {@code initializeClient} so the {@code builtin/entity} item model renders through
     * {@link Drone#customRenderer}; on Fabric the client registers the renderer with
     * {@code BuiltinItemRendererRegistry} instead.
     */
    @ExpectPlatform
    public static Drone createDrone(Item.Properties props) {
        throw new AssertionError();
    }

    /**
     * Creates the hover boots item. On Forge this is a subclass overriding
     * {@code getArmorTexture} / {@code initializeClient} (armor model from
     * {@link HoverBoots#armorModel}); on Fabric the client registers an ArmorRenderer.
     */
    @ExpectPlatform
    public static HoverBoots createHoverBoots(Item.Properties props) {
        throw new AssertionError();
    }
}
