package li.cil.oc.util;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.common.transfer.ItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

import java.util.Arrays;

/**
 * Former implicit extension class for {@link Arguments}; call as
 * {@code ExtendedArguments.method(args, ...)}.
 */
public final class ExtendedArguments {
    private ExtendedArguments() {
    }

    /** One bucket, in millibuckets (formerly {@code FluidAttributes.BUCKET_VOLUME}). */
    public static final int BUCKET_VOLUME = 1000;

    public static int optItemCount(Arguments args, int index, int defaultValue) {
        if (!isDefined(args, index) || !hasValue(args, index)) return defaultValue;
        else return Math.max(0, Math.min(64, args.checkInteger(index)));
    }

    public static int optItemCount(Arguments args, int index) {
        return optItemCount(args, index, 64);
    }

    public static int optFluidCount(Arguments args, int index, int defaultValue) {
        if (!isDefined(args, index) || !hasValue(args, index)) return defaultValue;
        else return Math.max(0, args.checkInteger(index));
    }

    public static int optFluidCount(Arguments args, int index) {
        return optFluidCount(args, index, BUCKET_VOLUME);
    }

    public static int checkSlot(Arguments args, ItemHandler inventory, int n) {
        final int slot = args.checkInteger(n) - 1;
        if (slot < 0 || slot >= inventory.getSlots()) {
            throw new IllegalArgumentException("invalid slot");
        }
        return slot;
    }

    public static int optSlot(Arguments args, ItemHandler inventory, int index, int defaultValue) {
        if (!isDefined(args, index)) return defaultValue;
        else return checkSlot(args, inventory, index);
    }

    public static int checkSlot(Arguments args, Container inventory, int n) {
        return checkSlot(args, InventoryUtils.asItemHandler(inventory), n);
    }

    public static int optSlot(Arguments args, Container inventory, int index, int defaultValue) {
        return optSlot(args, InventoryUtils.asItemHandler(inventory), index, defaultValue);
    }

    public static int checkTank(Arguments args, MultiTank multi, int n) {
        final int tank = args.checkInteger(n) - 1;
        if (tank < 0 || tank >= multi.tankCount()) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return tank;
    }

    public static TankProperties checkTankProperties(Arguments args, FluidHandler handler, int n) {
        final int tank = args.checkInteger(n) - 1;
        if (tank < 0 || tank >= handler.getTanks()) {
            throw new IllegalArgumentException("invalid tank index");
        }
        return new TankProperties(handler.getTankCapacity(tank), handler.getFluidInTank(tank));
    }

    public static TankProperties optTankProperties(Arguments args, FluidHandler handler, int n, TankProperties defaultValue) {
        if (!isDefined(args, n)) return defaultValue;
        else return checkTankProperties(args, handler, n);
    }

    public static Direction checkSideAny(Arguments args, int index) {
        return checkSide(args, index, Direction.values());
    }

    public static Direction optSideAny(Arguments args, int index, Direction defaultValue) {
        if (!isDefined(args, index)) return defaultValue;
        else return checkSideAny(args, index);
    }

    public static Direction checkSideExcept(Arguments args, int index, Direction... invalid) {
        return checkSide(args, index, Arrays.stream(Direction.values()).filter(d -> !Arrays.asList(invalid).contains(d)).toArray(Direction[]::new));
    }

    public static Direction optSideExcept(Arguments args, int index, Direction defaultValue, Direction... invalid) {
        if (!isDefined(args, index)) return defaultValue;
        else return checkSideExcept(args, index, invalid);
    }

    public static Direction checkSideForAction(Arguments args, int index) {
        return checkSide(args, index, Direction.SOUTH, Direction.UP, Direction.DOWN);
    }

    public static Direction optSideForAction(Arguments args, int index, Direction defaultValue) {
        if (!isDefined(args, index)) return defaultValue;
        else return checkSideForAction(args, index);
    }

    public static Direction checkSideForMovement(Arguments args, int index) {
        return checkSide(args, index, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN);
    }

    public static Direction optSideForMovement(Arguments args, int index, Direction defaultValue) {
        if (!isDefined(args, index)) return defaultValue;
        else return checkSideForMovement(args, index);
    }

    public static Direction checkSideForFace(Arguments args, int index, Direction facing) {
        return checkSideExcept(args, index, facing.getOpposite());
    }

    public static Direction optSideForFace(Arguments args, int index, Direction defaultValue) {
        if (!isDefined(args, index)) return defaultValue;
        else return checkSideForAction(args, index);
    }

    private static Direction checkSide(Arguments args, int index, Direction... allowed) {
        final int side = args.checkInteger(index);
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("invalid side");
        }
        final Direction direction = Direction.from3DDataValue(side);
        if (allowed.length == 0 || Arrays.asList(allowed).contains(direction)) return direction;
        else throw new IllegalArgumentException("unsupported side");
    }

    private static boolean isDefined(Arguments args, int index) {
        return index >= 0 && index < args.count();
    }

    private static boolean hasValue(Arguments args, int index) {
        return args.checkAny(index) != null;
    }

    /**
     * Snapshot of a single tank of a {@link FluidHandler}. Capacity and
     * contents are in millibuckets.
     */
    @Deprecated
    public static class TankProperties {
        public final long capacity;
        public final FluidStack contents;

        public TankProperties(long capacity, FluidStack contents) {
            this.capacity = capacity;
            this.contents = contents;
        }
    }
}
