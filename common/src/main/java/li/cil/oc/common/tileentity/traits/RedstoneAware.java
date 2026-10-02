package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.common.EventHandler;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.server.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * The Scala {@code protected[tileentity] val _input / _output} arrays and {@code _isOutputEnabled}
 * live in {@link State} ({@code redstoneAwareState().input} etc.).
 */
public interface RedstoneAware extends RotationAware {
    final class State {
        public final int[] input = filled(6, -1);
        public final int[] output = new int[6];
        public volatile boolean isOutputEnabled = false;
        public volatile boolean shouldUpdateInput = true;

        private static int[] filled(int size, int value) {
            final int[] result = new int[size];
            Arrays.fill(result, value);
            return result;
        }
    }

    /** Provided by {@link TileEntity}. */
    State redstoneAwareState();

    default boolean isOutputEnabled() {
        return redstoneAwareState().isOutputEnabled;
    }

    default RedstoneAware setOutputEnabled(boolean value) {
        final State state = redstoneAwareState();
        if (value != state.isOutputEnabled) {
            state.isOutputEnabled = value;
            if (!value) {
                Arrays.fill(state.output, 0);
            }
            onRedstoneOutputEnabledChanged();
        }
        return this;
    }

    default Optional<Object> getObjectFuzzy(Map<?, ?> map, int key) {
        @SuppressWarnings("unchecked") final Map<Object, Object> refMap = (Map<Object, Object>) map;
        if (refMap.containsKey(key))
            return Optional.ofNullable(refMap.get(key));
        else if (refMap.containsKey((double) key))
            return Optional.ofNullable(refMap.get((double) key));
        else
            return Optional.empty();
    }

    default Optional<Integer> valueToInt(Optional<Object> value) {
        if (value.isPresent() && value.get() instanceof Number num) return Optional.of(num.intValue());
        return Optional.empty();
    }

    default int[] getInput() {
        final int[] input = redstoneAwareState().input;
        final int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) result[i] = Math.max(input[i], 0);
        return result;
    }

    default int getInput(Direction side) {
        return Math.max(redstoneAwareState().input[side.ordinal()], 0);
    }

    default void setInput(Direction side, int newInput) {
        final int[] input = redstoneAwareState().input;
        final int oldInput = input[side.ordinal()];
        input[side.ordinal()] = newInput;
        if (oldInput >= 0 && newInput != oldInput) {
            onRedstoneInputChanged(new RedstoneChangedEventArgs(side, oldInput, newInput));
        }
    }

    default void setInput(int[] values) {
        for (Direction side : Direction.values()) {
            final int value = side.ordinal() < values.length ? values[side.ordinal()] : 0;
            setInput(side, value);
        }
    }

    default int maxInput() {
        int max = 0;
        for (int value : redstoneAwareState().input) max = Math.max(max, value);
        return max;
    }

    default int[] getOutput() {
        final int[] output = redstoneAwareState().output;
        final Direction[] sides = Direction.values();
        final int[] result = new int[sides.length];
        for (Direction side : sides) result[side.ordinal()] = output[toLocal(side).ordinal()];
        return result;
    }

    default int getOutput(Direction side) {
        final int[] output = redstoneAwareState().output;
        final Direction local = toLocal(side);
        if (output != null && local != null && output.length > local.ordinal())
            return output[local.ordinal()];
        else return 0;
    }

    default boolean setOutput(Direction side, int value) {
        if (value == getOutput(side)) return false;
        redstoneAwareState().output[toLocal(side).ordinal()] = value;
        onRedstoneOutputChanged(side);
        return true;
    }

    default boolean setOutput(Map<?, ?> values) {
        boolean changed = false;
        for (Direction side : Direction.values()) {
            final int sideIndex = toLocal(side).ordinal();
            // due to a bug in our jnlua layer, I cannot loop the map
            final Optional<Integer> num = valueToInt(getObjectFuzzy(values, sideIndex));
            if (num.isPresent() && setOutput(side, num.get())) changed = true;
        }
        return changed;
    }

    default void checkRedstoneInputChanged() {
        if (this instanceof Tickable) {
            redstoneAwareState().shouldUpdateInput = isServer();
        } else {
            for (Direction side : Direction.values()) updateRedstoneInput(side);
        }
    }

    default void updateRedstoneInput(Direction side) {
        setInput(side, BundledRedstone.computeInput(position(), side));
    }

    // ----------------------------------------------------------------------- //

    static void onUpdateEntity(RedstoneAware self) {
        if (self.isServer()) {
            final State state = self.redstoneAwareState();
            if (state.shouldUpdateInput) {
                state.shouldUpdateInput = false;
                for (Direction side : Direction.values()) self.updateRedstoneInput(side);
            }
        }
    }

    static void onClearRemoved(RedstoneAware self) {
        if (!(self instanceof Tickable)) {
            EventHandler.scheduleServer(() -> {
                for (Direction side : Direction.values()) self.updateRedstoneInput(side);
            });
        }
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForServer(RedstoneAware self, CompoundTag nbt) {
        final State state = self.redstoneAwareState();
        final int[] input = nbt.getIntArray(Settings.namespace + "rs.input");
        System.arraycopy(input, 0, state.input, 0, Math.min(input.length, state.input.length));
        final int[] output = nbt.getIntArray(Settings.namespace + "rs.output");
        System.arraycopy(output, 0, state.output, 0, Math.min(output.length, state.output.length));
    }

    static void onSaveForServer(RedstoneAware self, CompoundTag nbt) {
        final State state = self.redstoneAwareState();
        nbt.putIntArray(Settings.namespace + "rs.input", state.input);
        nbt.putIntArray(Settings.namespace + "rs.output", state.output);
    }

    static void onLoadForClient(RedstoneAware self, CompoundTag nbt) {
        final State state = self.redstoneAwareState();
        state.isOutputEnabled = nbt.getBoolean("isOutputEnabled");
        final int[] output = nbt.getIntArray("output");
        System.arraycopy(output, 0, state.output, 0, Math.min(output.length, state.output.length));
    }

    static void onSaveForClient(RedstoneAware self, CompoundTag nbt) {
        final State state = self.redstoneAwareState();
        nbt.putBoolean("isOutputEnabled", state.isOutputEnabled);
        nbt.putIntArray("output", state.output);
    }

    // ----------------------------------------------------------------------- //

    default void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
    }

    default void onRedstoneOutputEnabledChanged() {
        final Level level = getLevel();
        if (level != null) {
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            if (isServer()) PacketSender.sendRedstoneState(this);
            else {
                final BlockState state = level.getBlockState(getBlockPos());
                level.sendBlockUpdated(getBlockPos(), state, state, 3);
            }
        }
    }

    default void onRedstoneOutputChanged(Direction side) {
        final Level level = getLevel();
        final BlockPos blockPos = getBlockPos().relative(side);
        level.neighborChanged(blockPos, getBlockState().getBlock(), blockPos);
        level.updateNeighborsAtExceptFromFacing(blockPos, level.getBlockState(blockPos).getBlock(), side.getOpposite());

        if (isServer()) PacketSender.sendRedstoneState(this);
        else {
            final BlockState state = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }
}
