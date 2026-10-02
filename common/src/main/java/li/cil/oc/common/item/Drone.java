package li.cil.oc.common.item;

import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.Rarity;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * The drone item model is {@code builtin/entity}.
 * <p>
 * Client hook: the client sets {@link #customRenderer} to a supplier of its
 * {@code BlockEntityWithoutLevelRenderer} (used by the Forge item created in
 * {@code ItemPlatformImpl}); on Fabric it registers that renderer with
 * {@code BuiltinItemRendererRegistry} for this item. Instances are created through
 * {@link li.cil.oc.common.platform.ItemPlatform#createDrone}.
 */
public class Drone extends SimpleItem {
    /** Supplies the client's BlockEntityWithoutLevelRenderer for the drone item (client only). */
    public static volatile Supplier<Object> customRenderer;

    public Drone(Properties props) {
        super(props);
        // Explicit, because the Forge item is an anonymous subclass (see ItemPlatform).
        unlocalizedName = "drone";
    }

    @Override
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        if (li.cil.oc.util.Tooltip.showExtended()) {
            final DroneData info = new DroneData(stack);
            for (ItemStack component : info.components) {
                if (!component.isEmpty()) {
                    tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        final DroneData data = new DroneData(stack);
        return Rarity.byTier(data.tier);
    }

    // Must be assembled to be usable so we hide it in the item list (no creative tab, see Items).

    @Override
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        final Level world = position.world.get();
        if (!world.isClientSide) {
            final li.cil.oc.common.entity.Drone drone = EntityTypes.DRONE.get().create(world);
            if (player instanceof li.cil.oc.server.agent.Player fakePlayer) {
                drone.ownerName = fakePlayer.agent.ownerName();
                drone.ownerUUID = fakePlayer.agent.ownerUUID();
            } else {
                drone.ownerName = player.getName().getString();
                drone.ownerUUID = player.getGameProfile().getId();
            }
            drone.initializeAfterPlacement(stack, player, position.offset(hitX * 1.1f, hitY * 1.1f, hitZ * 1.1f));
            world.addFreshEntity(drone);
        }
        stack.shrink(1);
        return true;
    }
}
