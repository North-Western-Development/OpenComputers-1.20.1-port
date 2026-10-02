package li.cil.oc.common.item;

import li.cil.oc.client.ItemClientHooks;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class TexturePicker extends SimpleItem {
    public TexturePicker(Properties props) {
        super(props);
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        final Block block = ExtendedWorld.getBlock(player.level(), position);
        if (block != null) {
            if (player.level().isClientSide) {
                ItemClientHooks.showTextureName(player, position.toBlockPos());
            }
            return true;
        }
        return super.onItemUse(stack, player, position, side, hitX, hitY, hitZ);
    }
}
