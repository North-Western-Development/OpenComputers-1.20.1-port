package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Geolyzer extends TileEntity implements Environment {
    public final li.cil.oc.server.component.Geolyzer geolyzer = new li.cil.oc.server.component.Geolyzer(this);

    public Geolyzer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Node node() {
        return geolyzer.node();
    }

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        geolyzer.loadData(nbt);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        geolyzer.saveData(nbt);
    }
}
