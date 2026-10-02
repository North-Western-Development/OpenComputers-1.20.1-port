package li.cil.oc.common.tileentity;

import li.cil.oc.api.network.Node;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MotionSensor extends TileEntity implements Environment, Tickable {
    public final li.cil.oc.server.component.MotionSensor motionSensor = new li.cil.oc.server.component.MotionSensor(this);

    public MotionSensor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Node node() {
        return motionSensor.node();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isServer()) {
            motionSensor.update();
        }
    }

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        motionSensor.loadData(nbt);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        motionSensor.saveData(nbt);
    }
}
