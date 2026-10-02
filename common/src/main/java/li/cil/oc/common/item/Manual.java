package li.cil.oc.common.item;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.BlockPosition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class Manual extends SimpleItem {
    public Manual(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY.toString() + "v" + OpenComputers.version()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (world.isClientSide) {
            if (player.isCrouching()) {
                li.cil.oc.api.Manual.reset();
            }
            li.cil.oc.api.Manual.openFor(player);
        }
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        final Level world = player.level();
        final String path = li.cil.oc.api.Manual.pathFor(world, position.toBlockPos());
        if (path != null) {
            if (world.isClientSide) {
                li.cil.oc.api.Manual.openFor(player);
                li.cil.oc.api.Manual.reset();
                li.cil.oc.api.Manual.navigate(path);
            }
            return true;
        }
        return super.onItemUse(stack, player, position, side, hitX, hitY, hitZ);
    }
}
