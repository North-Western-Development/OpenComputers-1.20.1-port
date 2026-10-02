package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Assembler extends SimpleBlock implements PowerAcceptor, StateAware, GUI {
    public final VoxelShape blockShape;

    public Assembler(Properties props) {
        super(props);
        VoxelShape bottom = Block.box(0, 0, 0, 16, 7, 16);
        VoxelShape mid = Block.box(2, 7, 2, 14, 9, 14);
        VoxelShape top = Block.box(0, 9, 0, 16, 16, 16);
        blockShape = Shapes.or(top, bottom, mid);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().assemblerRate;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return blockShape;
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Assembler te) {
            ContainerTypes.openAssemblerGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.ASSEMBLER.get().create(pos, state);
    }
}
