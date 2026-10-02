package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * Scala trait (always used as primary superclass) → abstract class.
 */
public abstract class RedstoneBundled extends RedstoneVanilla {
    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Communication,
                DeviceAttribute.Description, "Advanced redstone controller",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Rb800-M",
                DeviceAttribute.Capacity, "65536",
                DeviceAttribute.Width, "16"
            );
        }
        return deviceInfo;
    }

    private static final int COLOR_COUNT = 16;

    // ----------------------------------------------------------------------- //

    @Override
    public abstract BundledRedstoneAware redstone();

    private Map<Integer, Integer> colorsToMap(int[] ar) {
        final Map<Integer, Integer> result = new HashMap<>();
        for (int color = 0; color < COLOR_COUNT; color++) {
            if (color < ar.length) result.put(color, ar[color]);
        }
        return result;
    }

    private Map<Integer, Map<Integer, Integer>> sidesToMap(int[][] ar) {
        final Map<Integer, Map<Integer, Integer>> result = new HashMap<>();
        for (Direction side : SIDE_RANGE) {
            if (side.ordinal() < ar.length && ar[side.ordinal()].length > 0) {
                result.put(side.ordinal(), colorsToMap(ar[side.ordinal()]));
            }
        }
        return result;
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of inputs")
    public Object[] getBundledInput(Context context, Arguments args) {
        switch (args.count()) {
            case 2:
                return ResultWrapper.result(redstone().getBundledInput(checkSide(args, 0), checkColor(args, 1)));
            case 1:
                return ResultWrapper.result(colorsToMap(redstone().getBundledInput(checkSide(args, 0))));
            case 0:
                return ResultWrapper.result(sidesToMap(redstone().getBundledInput()));
            default:
                throw new IllegalArgumentException("too many arguments, expected 0, 1, or 2");
        }
    }

    @Callback(direct = true, doc = "function([side:number[, color:number]]):number or table -- Fewer params returns set of outputs")
    public Object[] getBundledOutput(Context context, Arguments args) {
        switch (args.count()) {
            case 2:
                return ResultWrapper.result(redstone().getBundledOutput(checkSide(args, 0), checkColor(args, 1)));
            case 1:
                return ResultWrapper.result(colorsToMap(redstone().getBundledOutput(checkSide(args, 0))));
            case 0:
                return ResultWrapper.result(sidesToMap(redstone().getBundledOutput()));
            default:
                throw new IllegalArgumentException("too many arguments, expected 0, 1, or 2");
        }
    }

    @Callback(doc = "function([side:number[, color:number,]] value:number or table):number or table --  Fewer params to assign set of outputs. Returns previous values")
    public Object[] setBundledOutput(Context context, Arguments args) {
        final Object ret;
        final boolean changed;
        switch (args.count()) {
            case 3: {
                final Direction side = checkSide(args, 0);
                final int color = checkColor(args, 1);
                final int value = args.checkInteger(2);
                ret = redstone().getBundledOutput(side, color);
                changed = redstone().setBundledOutput(side, color, value);
                break;
            }
            case 2: {
                final Direction side = checkSide(args, 0);
                final Map<?, ?> value = args.checkTable(1);
                ret = redstone().getBundledOutput(side);
                changed = redstone().setBundledOutput(side, value);
                break;
            }
            case 1: {
                final Map<?, ?> value = args.checkTable(0);
                ret = redstone().getBundledOutput();
                changed = redstone().setBundledOutput(value);
                break;
            }
            default:
                throw new IllegalArgumentException("invalid number of arguments, expected 1, 2, or 3");
        }
        if (changed) {
            if (Settings.get().redstoneDelay > 0)
                context.pause(Settings.get().redstoneDelay);
        }
        return ResultWrapper.result(ret);
    }

    // ----------------------------------------------------------------------- //

    private int checkColor(Arguments args, int index) {
        final int color = args.checkInteger(index);
        if (color < 0 || color >= COLOR_COUNT)
            throw new IllegalArgumentException("invalid color");
        return color;
    }
}
