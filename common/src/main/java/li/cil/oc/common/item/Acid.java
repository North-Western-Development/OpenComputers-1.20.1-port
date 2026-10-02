package li.cil.oc.common.item;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class Acid extends SimpleItem {
    public Acid(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        player.startUsingItem(player.getItemInHand(InteractionHand.MAIN_HAND) == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!world.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2000));
                // Remove nanomachines if installed.
                Nanomachines.uninstallController(player);
            }
            stack.shrink(1);
            if (stack.getCount() > 0) return stack;
            else return ItemStack.EMPTY;
        }
        return stack;
    }
}
