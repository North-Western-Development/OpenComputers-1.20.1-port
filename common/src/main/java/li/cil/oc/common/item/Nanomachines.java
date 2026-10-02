package li.cil.oc.common.item;

import com.google.common.base.Strings;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.common.item.data.NanomachineData;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class Nanomachines extends SimpleItem {
    public Nanomachines(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        if (stack.hasTag()) {
            final NanomachineData data = new NanomachineData(stack);
            if (!Strings.isNullOrEmpty(data.uuid)) {
                tooltip.add(Component.literal("§8" + data.uuid.substring(0, 13) + "...§7"));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        player.startUsingItem(player.getItemInHand(InteractionHand.MAIN_HAND) == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!world.isClientSide) {
                final NanomachineData data = new NanomachineData(stack);
                // Re-install to get new address, make sure we're configured.
                li.cil.oc.api.Nanomachines.uninstallController(player);
                final Controller installed = li.cil.oc.api.Nanomachines.installController(player);
                if (installed instanceof ControllerImpl controller) {
                    if (data.configuration.isPresent()) {
                        if (!Strings.isNullOrEmpty(data.uuid)) {
                            controller.uuid = data.uuid;
                        }
                        controller.configuration.loadData(data.configuration.get());
                    } else {
                        controller.reconfigure();
                    }
                } else {
                    installed.reconfigure(); // Huh.
                }
            }
            stack.shrink(1);
            if (stack.getCount() > 0) return stack;
            else return ItemStack.EMPTY;
        }
        return stack;
    }
}
