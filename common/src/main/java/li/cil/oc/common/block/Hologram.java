package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class Hologram extends SimpleBlock {
    public final int tier;

    public final VoxelShape shape = Shapes.box(0, 0, 0, 1, 0.5, 1);

    public Hologram(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        addLines(tooltip, Tooltip.get(getClass().getSimpleName().toLowerCase() + tier));
    }

    // ----------------------------------------------------------------------- //

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.tileentity.Hologram(TileEntityTypes.HOLOGRAM.get(), pos, state, tier);
    }
}
