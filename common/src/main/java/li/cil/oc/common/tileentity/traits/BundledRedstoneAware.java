package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * The Scala {@code _bundledInput / _rednetInput / _bundledOutput} arrays live in {@link State}.
 */
public interface BundledRedstoneAware extends RedstoneAware {
    final class State {
        public final int[][] bundledInput = filled(-1);
        public final int[][] rednetInput = filled(-1);
        public final int[][] bundledOutput = filled(0);

        private static int[][] filled(int value) {
            final int[][] result = new int[6][16];
            for (int[] row : result) Arrays.fill(row, value);
            return result;
        }
    }

    String BundledInputTag = Settings.namespace + "rs.bundledInput";
    String BundledOutputTag = Settings.namespace + "rs.bundledOutput";
    String RednetInputTag = Settings.namespace + "rs.rednetInput";

    /** Provided by {@link TileEntity}. */
    State bundledRedstoneAwareState();

    // ----------------------------------------------------------------------- //

    @Override
    default RedstoneAware setOutputEnabled(boolean value) {
        if (value != redstoneAwareState().isOutputEnabled) {
            if (!value) {
                for (int[] row : bundledRedstoneAwareState().bundledOutput) {
                    Arrays.fill(row, 0);
                }
            }
        }
        return RedstoneAware.super.setOutputEnabled(value);
    }

    default int[][] getBundledInput() {
        final State state = bundledRedstoneAwareState();
        final int[][] result = new int[6][16];
        for (int side = 0; side < 6; side++) {
            for (int color = 0; color < 16; color++) {
                result[side][color] = Math.max(Math.max(state.bundledInput[side][color], state.rednetInput[side][color]), 0);
            }
        }
        return result;
    }

    private static int checkSide(Direction side) {
        final int index = side.ordinal();
        if (index >= 6) throw new IndexOutOfBoundsException("Bad side " + side);
        return index;
    }

    private static int checkColor(int color) {
        if (color < 0 || color >= 16) throw new IndexOutOfBoundsException("Bad color " + color);
        return color;
    }

    default int[] getBundledInput(Direction side) {
        final State state = bundledRedstoneAwareState();
        final int sideIndex = checkSide(side);
        final int[] bundled = state.bundledInput[sideIndex];
        final int[] rednet = state.rednetInput[sideIndex];
        final int[] result = new int[Math.min(bundled.length, rednet.length)];
        for (int i = 0; i < result.length; i++) result[i] = Math.max(Math.max(bundled[i], rednet[i]), 0);
        return result;
    }

    default int getBundledInput(Direction side, int color) {
        final State state = bundledRedstoneAwareState();
        final int sideIndex = checkSide(side);
        final int colorIndex = checkColor(color);
        return Math.max(Math.max(state.bundledInput[sideIndex][colorIndex], state.rednetInput[sideIndex][colorIndex]), 0);
    }

    default void setBundledInput(Direction side, int color, int newValue) {
        updateInput(bundledRedstoneAwareState().bundledInput, side, color, newValue);
    }

    default void setBundledInput(Direction side, int[] newBundledInput) {
        for (int color = 0; color < 16; color++) {
            final int value = newBundledInput == null || color >= newBundledInput.length ? 0 : newBundledInput[color];
            setBundledInput(side, color, value);
        }
    }

    default void setRednetInput(Direction side, int color, int value) {
        updateInput(bundledRedstoneAwareState().rednetInput, side, color, value);
    }

    default void updateInput(int[][] inputs, Direction side, int color, int newValue) {
        final int sideIndex = checkSide(side);
        final int colorIndex = checkColor(color);
        final int oldValue = inputs[sideIndex][colorIndex];
        if (oldValue != newValue) {
            inputs[sideIndex][colorIndex] = newValue;
            if (oldValue != -1) {
                onRedstoneInputChanged(new RedstoneChangedEventArgs(side, oldValue, newValue, colorIndex));
            }
        }
    }

    // Note: returns the bundled *input* in the original code as well.
    default int[][] getBundledOutput() {
        return bundledRedstoneAwareState().bundledInput;
    }

    default int[] getBundledOutput(Direction side) {
        return bundledRedstoneAwareState().bundledOutput[checkSide(toLocal(side))];
    }

    default int getBundledOutput(Direction side, int color) {
        return getBundledOutput(side)[checkColor(color)];
    }

    default boolean setBundledOutput(Direction side, int color, int value) {
        if (value != getBundledOutput(side, color)) {
            bundledRedstoneAwareState().bundledOutput[checkSide(toLocal(side))][checkColor(color)] = value;
            onRedstoneOutputChanged(side);
            return true;
        } else return false;
    }

    default boolean setBundledOutput(Direction side, Map<?, ?> values) {
        final int sideIndex = toLocal(side).ordinal();
        boolean changed = false;
        for (int color = 0; color < 16; color++) {
            // due to a bug in our jnlua layer, I cannot loop the map
            final Optional<Integer> newValue = valueToInt(getObjectFuzzy(values, color));
            if (newValue.isPresent()) {
                if (newValue.get() != getBundledOutput(side, color)) {
                    bundledRedstoneAwareState().bundledOutput[sideIndex][color] = newValue.get();
                    changed = true;
                }
            }
        }
        if (changed) {
            onRedstoneOutputChanged(side);
        }
        return changed;
    }

    default boolean setBundledOutput(Map<?, ?> values) {
        boolean changed = false;
        for (Direction side : Direction.values()) {
            final int sideIndex = toLocal(side).ordinal();
            // due to a bug in our jnlua layer, I cannot loop the map
            final Optional<Object> child = getObjectFuzzy(values, sideIndex);
            if (child.isPresent() && child.get() instanceof Map<?, ?> childMap && setBundledOutput(side, childMap)) {
                changed = true;
            }
        }
        return changed;
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void updateRedstoneInput(Direction side) {
        RedstoneAware.super.updateRedstoneInput(side);
        setBundledInput(side, BundledRedstone.computeBundledInput(position(), side));
    }

    // ----------------------------------------------------------------------- //

    private static void readArrays(CompoundTag nbt, String tag, int[][] target) {
        final IntArrayTag[] list = ExtendedNBT.toTagArray(nbt.getList(tag, Tag.TAG_INT_ARRAY), IntArrayTag.class);
        for (int index = 0; index < list.length && index < target.length; index++) {
            final int[] input = list[index].getAsIntArray();
            final int safeLength = Math.min(input.length, target[index].length);
            System.arraycopy(input, 0, target[index], 0, safeLength);
        }
    }

    static void onLoadForServer(BundledRedstoneAware self, CompoundTag nbt) {
        final State state = self.bundledRedstoneAwareState();
        readArrays(nbt, BundledInputTag, state.bundledInput);
        readArrays(nbt, BundledOutputTag, state.bundledOutput);
        readArrays(nbt, RednetInputTag, state.rednetInput);
    }

    static void onSaveForServer(BundledRedstoneAware self, CompoundTag nbt) {
        final State state = self.bundledRedstoneAwareState();
        ExtendedNBT.setNewTagList(nbt, BundledInputTag, ExtendedNBT.intArrayIterableToNbt(Arrays.asList(state.bundledInput)));
        ExtendedNBT.setNewTagList(nbt, BundledOutputTag, ExtendedNBT.intArrayIterableToNbt(Arrays.asList(state.bundledOutput)));
        ExtendedNBT.setNewTagList(nbt, RednetInputTag, ExtendedNBT.intArrayIterableToNbt(Arrays.asList(state.rednetInput)));
    }
}
