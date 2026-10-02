package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Printer extends SimpleBlock implements StateAware, GUI {
    public final VoxelShape blockShape;

    public Printer(Properties props) {
        super(props);
        VoxelShape base = Block.box(0, 0, 0, 16, 8, 16);
        VoxelShape pillars = Shapes.or(Block.box(0, 8, 0, 3, 13, 3), Block.box(13, 8, 0, 16, 13, 3),
            Block.box(13, 8, 13, 16, 13, 16), Block.box(0, 8, 13, 3, 13, 16));
        VoxelShape ring = Shapes.join(Block.box(0, 13, 0, 16, 16, 16),
            Block.box(3, 13, 3, 13, 16, 13), BooleanOp.ONLY_FIRST);
        blockShape = Shapes.or(base, pillars, ring);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return blockShape;
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Printer te) {
            ContainerTypes.openPrinterGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.PRINTER.get().create(pos, state);
    }
}
