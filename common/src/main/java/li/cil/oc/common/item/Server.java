package li.cil.oc.common.item;

import li.cil.oc.client.KeyBindings;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.ServerInventory;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Server extends SimpleItem {
    public final int tier;

    public Server(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        return super.getDescriptionId() + tier;
    }

    @Override
    protected Optional<String> tooltipName() {
        return Optional.ofNullable(unlocalizedName);
    }

    // Formerly `private object HelperInventory extends ServerInventory`.
    private final ItemStack[] helperContainer = {ItemStack.EMPTY};

    private ServerInventory helperInventory;

    private ServerInventory helperInventory() {
        if (helperInventory == null) {
            helperInventory = new ServerInventory() {
                private final ItemsHolder itemsHolder = new ItemsHolder();

                @Override
                public ItemStack[] items() {
                    return itemsHolder.get(this);
                }

                @Override
                public ItemStack container() {
                    return helperContainer[0];
                }

                @Override
                public int rackSlot() {
                    return -1;
                }
            };
        }
        return helperInventory;
    }

    @Override
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        super.tooltipExtended(stack, tooltip);
        if (KeyBindings.showExtendedTooltips()) {
            helperContainer[0] = stack;
            final ServerInventory helper = helperInventory();
            helper.reinitialize();
            final Map<String, Integer> stacks = new HashMap<>();
            for (int slot = 0; slot < helper.getContainerSize(); slot++) {
                final ItemStack aStack = helper.getItem(slot);
                if (!aStack.isEmpty()) {
                    final String displayName = aStack.getHoverName().getString();
                    stacks.merge(displayName, 1, Integer::sum);
                }
            }
            if (!stacks.isEmpty()) {
                for (String curr : Tooltip.get("server.Components")) {
                    tooltip.add(Component.literal(curr).setStyle(Tooltip.DefaultStyle));
                }
                final List<String> names = new ArrayList<>(stacks.keySet());
                Collections.sort(names);
                for (String itemName : names) {
                    tooltip.add(Component.literal("- " + stacks.get(itemName) + "x " + itemName).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (!player.isCrouching()) {
            if (!world.isClientSide && player instanceof ServerPlayer srvPlr) {
                ContainerTypes.openServerGui(srvPlr, new ServerInventory() {
                private final ItemsHolder itemsHolder = new ItemsHolder();

                @Override
                public ItemStack[] items() {
                    return itemsHolder.get(this);
                }

                    @Override
                    public ItemStack container() {
                        return stack;
                    }

                    @Override
                    public int rackSlot() {
                        return -1;
                    }

                    @Override
                    public boolean stillValid(Player player) {
                        return player == srvPlr;
                    }
                }, -1);
            }
            player.swing(InteractionHand.MAIN_HAND);
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }
}
