package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class Redstone extends RedstoneAware {
    public Redstone(Properties props) {
        super(props);
    }

    @Override
    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        super.tooltipTail(stack, world, tooltip, advanced);
        // TODO(port): integration - ProjectRed bundled redstone tooltip ("redstonecard.ProjectRed").
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.REDSTONE_IO.get().create(pos, state);
    }
}
