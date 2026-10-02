package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class Capacitor extends TileEntity implements Environment, DeviceInfo {
    // Start with maximum theoretical capacity, gets reduced after validation.
    // This is done so that we don't lose energy while loading.
    public final Connector node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withConnector(maxCapacity()).
        create();

    private Map<String, String> deviceInfo;

    public Capacitor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Battery",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "CapBank3x",
                DeviceAttribute.Capacity, String.valueOf(maxCapacity())
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) {
            final Level level = getLevel();
            for (BlockPos coordinate : indirectNeighbors()) {
                if (level.isLoaded(coordinate) && level.getBlockEntity(coordinate) instanceof Capacitor capacitor) {
                    capacitor.recomputeCapacity();
                }
            }
        }
    }

    @Override
    public void onConnect(Node node) {
        Environment.super.onConnect(node);
        if (node == this.node) {
            recomputeCapacity(true);
        }
    }

    // ----------------------------------------------------------------------- //

    public void recomputeCapacity(boolean updateSecondGradeNeighbors) {
        final Level level = getLevel();
        int direct = 0;
        for (Direction side : Direction.values()) {
            final BlockPos blockPos = getBlockPos().relative(side);
            if (level.isLoaded(blockPos) && level.getBlockEntity(blockPos) instanceof Capacitor) {
                direct += 1;
            }
        }
        int indirect = 0;
        for (BlockPos blockPos : indirectNeighbors()) {
            if (level.isLoaded(blockPos) && level.getBlockEntity(blockPos) instanceof Capacitor capacitor) {
                if (updateSecondGradeNeighbors) {
                    capacitor.recomputeCapacity();
                }
                indirect += 1;
            }
        }
        node.setLocalBufferSize(
            Settings.get().bufferCapacitor +
                Settings.get().bufferCapacitorAdjacencyBonus * direct +
                Settings.get().bufferCapacitorAdjacencyBonus / 2 * indirect);
    }

    public void recomputeCapacity() {
        recomputeCapacity(false);
    }

    private BlockPos[] indirectNeighbors() {
        final Direction[] sides = Direction.values();
        final BlockPos[] result = new BlockPos[sides.length];
        for (Direction side : sides) result[side.ordinal()] = getBlockPos().relative(side, 2);
        return result;
    }

    protected double maxCapacity() {
        return Settings.get().bufferCapacitor + Settings.get().bufferCapacitorAdjacencyBonus * 9;
    }
}
