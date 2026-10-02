package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedBlock;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Scala trait (always used as primary superclass) → abstract class. The redstone
 * host is both an {@link EnvironmentHost} and a {@link RedstoneAware}; use
 * {@link #redstoneHost()} for the former view.
 */
public abstract class RedstoneVanilla extends RedstoneSignaller implements DeviceInfo {
    public abstract RedstoneAware redstone();

    public EnvironmentHost redstoneHost() {
        return (EnvironmentHost) redstone();
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Communication,
                DeviceAttribute.Description, "Redstone controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Rs100-V",
                DeviceAttribute.Capacity, "16",
                DeviceAttribute.Width, "1"
            );
        }
        return deviceInfo;
    }

    protected final Direction[] SIDE_RANGE = Direction.values();

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get the redstone input (all sides, or optionally on the specified side)")
    public Object[] getInput(Context context, Arguments args) {
        final Optional<Integer> side = getOptionalSide(args);
        if (side.isPresent()) return ResultWrapper.result(redstone().getInput(Direction.values()[side.get()]));
        else return ResultWrapper.result(valuesToMap(redstone().getInput()));
    }

    @Callback(direct = true, doc = "function([side:number]):number or table -- Get the redstone output (all sides, or optionally on the specified side)")
    public Object[] getOutput(Context context, Arguments args) {
        final Optional<Integer> side = getOptionalSide(args);
        if (side.isPresent()) return ResultWrapper.result(redstone().getOutput(Direction.values()[side.get()]));
        else return ResultWrapper.result(valuesToMap(redstone().getOutput()));
    }

    @Callback(doc = "function([side:number, ]value:number or table):number or table --  Set the redstone output (all sides, or optionally on the specified side). Returns previous values")
    public Object[] setOutput(Context context, Arguments args) {
        final Object ret;
        final boolean changed;
        switch (args.count()) {
            case 2: {
                final Direction side = checkSide(args, 0);
                final int value = args.checkInteger(1);
                ret = redstone().getOutput(side);
                changed = redstone().setOutput(side, value);
                break;
            }
            case 1: {
                final Map<?, ?> value = args.checkTable(0);
                ret = valuesToMap(redstone().getOutput());
                changed = redstone().setOutput(value);
                break;
            }
            default:
                throw new IllegalArgumentException("invalid number of arguments, expected 1 or 2");
        }
        if (changed) {
            if (Settings.get().redstoneDelay > 0)
                context.pause(Settings.get().redstoneDelay);
        }
        return ResultWrapper.result(ret);
    }

    @Callback(direct = true, doc = "function(side:number):number -- Get the comparator input on the specified side.")
    public Object[] getComparatorInput(Context context, Arguments args) {
        final Direction side = checkSide(args, 0);
        final BlockPosition blockPos = BlockPosition.apply(redstoneHost()).offset(side);
        final Level world = redstoneHost().world();
        if (ExtendedWorld.blockExists(world, blockPos)) {
            final Block block = ExtendedWorld.getBlock(world, blockPos);
            if (world.getBlockState(blockPos.toBlockPos()).hasAnalogOutputSignal()) {
                final int comparatorOverride = ExtendedBlock.getComparatorInputOverride(block, blockPos, side.getOpposite());
                return ResultWrapper.result(comparatorOverride);
            }
        }
        return ResultWrapper.result(0);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("redstone.changed".equals(message.name())) {
            final Object[] data = message.data();
            if (data.length == 1 && data[0] instanceof RedstoneChangedEventArgs changedArgs) {
                onRedstoneChanged(changedArgs);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    private Optional<Integer> getOptionalSide(Arguments args) {
        if (args.count() == 1)
            return Optional.of(checkSide(args, 0).ordinal());
        else
            return Optional.empty();
    }

    protected Direction checkSide(Arguments args, int index) {
        final int side = args.checkInteger(index);
        if (side < 0 || side > 5)
            throw new IllegalArgumentException("invalid side");
        return redstone().toGlobal(Direction.from3DDataValue(side));
    }

    private Map<Integer, Integer> valuesToMap(int[] ar) {
        final Map<Integer, Integer> result = new HashMap<>();
        for (Direction side : SIDE_RANGE) {
            if (side.ordinal() < ar.length) result.put(side.ordinal(), ar[side.ordinal()]);
        }
        return result;
    }
}
