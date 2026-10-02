package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.DebugCardData;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DebugCard extends SimpleItem {
    public DebugCard(Properties props) {
        super(props);
    }

    @Override
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        super.tooltipExtended(stack, tooltip);
        final DebugCardData data = new DebugCardData(stack);
        data.access.ifPresent(access -> tooltip.add(Component.literal("§8" + access.player + "§r")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide && player.isCrouching()) {
            final DebugCardData data = new DebugCardData(stack);
            final String name = player.getName().getString();
            if (data.access.isPresent() && data.access.get().player.equals(name)) {
                data.access = Optional.empty();
            } else {
                final String nonce;
                if (Settings.get().debugCardAccess instanceof Settings.DebugCardAccess.Whitelist wl) {
                    final Optional<String> n = wl.nonce(name);
                    if (n.isPresent()) {
                        nonce = n.get();
                    } else {
                        player.sendSystemMessage(Component.literal("§cYou are not whitelisted to use debug card"));
                        player.swing(InteractionHand.MAIN_HAND);
                        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
                    }
                } else {
                    nonce = "";
                }
                data.access = Optional.of(new li.cil.oc.server.component.DebugCard.AccessContext(name, nonce));
            }
            data.saveData(stack);
            player.swing(InteractionHand.MAIN_HAND);
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }
}
