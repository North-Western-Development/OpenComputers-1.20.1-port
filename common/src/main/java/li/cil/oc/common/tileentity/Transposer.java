package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Transposer extends TileEntity implements Environment {
    public final li.cil.oc.server.component.Transposer.Block transposer = new li.cil.oc.server.component.Transposer.Block(this);

    // Used on client side to check whether to render activity indicators.
    public long lastOperation = 0L;

    public Transposer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Node node() {
        return transposer.node();
    }

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        transposer.loadData(nbt);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        transposer.saveData(nbt);
    }
}
