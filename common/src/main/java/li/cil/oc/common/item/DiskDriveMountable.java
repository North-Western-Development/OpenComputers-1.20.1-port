package li.cil.oc.common.item;

import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.inventory.DiskDriveMountableInventory;
import li.cil.oc.common.item.traits.SimpleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DiskDriveMountable extends SimpleItem {
    public DiskDriveMountable(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide && player instanceof ServerPlayer srvPlr) {
            ContainerTypes.openDiskDriveGui(srvPlr, new DiskDriveMountableInventory() {
                @Override
                public ItemStack container() {
                    return stack;
                }

                @Override
                public boolean stillValid(Player player) {
                    return player == srvPlr;
                }
            });
        }
        player.swing(InteractionHand.MAIN_HAND);
        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(world.isClientSide), stack);
    }
}
