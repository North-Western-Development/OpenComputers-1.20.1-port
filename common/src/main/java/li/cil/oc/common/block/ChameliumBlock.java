package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ChameliumBlock extends SimpleBlock {
    public static final EnumProperty<DyeColor> Color = EnumProperty.create("color", DyeColor.class);

    public ChameliumBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(Color, DyeColor.BLACK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Color);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        stack.setDamageValue(state.getValue(Color).getId());
        return stack;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(Color, DyeColor.byId(ctx.getItemInHand().getDamageValue()));
    }

    // Note: fillItemCategory is gone; the creative tab entry (black, damage 0) is the default stack.
}
