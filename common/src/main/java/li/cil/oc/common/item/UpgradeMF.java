package li.cil.oc.common.item;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.common.item.traits.ItemTier;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class UpgradeMF extends SimpleItem implements ItemTier {
    public UpgradeMF(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, Player player, Level world, BlockPos pos, Direction side, float hitX, float hitY, float hitZ, InteractionHand hand) {
        if (!player.level().isClientSide && player.isCrouching()) {
            final CompoundTag data = stack.getOrCreateTag();
            data.putString(Settings.namespace + "dimension", world.dimension().location().toString());
            data.putIntArray(Settings.namespace + "coord", new int[]{pos.getX(), pos.getY(), pos.getZ(), side.ordinal()});
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return super.onItemUseFirst(stack, player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    @Override
    protected void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        final CompoundTag data = stack.getTag();
        tooltip.add(Component.literal(Localization.Tooltip.MFULinked(data != null && data.contains(Settings.namespace + "coord"))).setStyle(Tooltip.DefaultStyle));
    }
}
