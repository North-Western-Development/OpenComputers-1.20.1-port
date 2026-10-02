package li.cil.oc.common.component.traits;

import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.util.PackedColor;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Optional;

/**
 * Holds GPU video RAM buffers (Scala trait {@code VideoRamDevice}).
 * <p>
 * Stateful trait: implementers provide the backing map through {@link #internalBuffers()},
 * e.g. {@code private final Map<Integer, GpuTextBuffer> internalBuffers = new HashMap<>();}.
 */
public interface VideoRamDevice {
    int RESERVED_SCREEN_INDEX = 0;

    /**
     * Backing storage of the buffers, by buffer index. Must be mutable.
     */
    Map<Integer, GpuTextBuffer> internalBuffers();

    /**
     * Accessor form of {@link #RESERVED_SCREEN_INDEX} (Scala {@code val} in a trait).
     */
    default int RESERVED_SCREEN_INDEX() {
        return RESERVED_SCREEN_INDEX;
    }

    default boolean isEmpty() {
        return internalBuffers().isEmpty();
    }

    default void onBufferRamDestroy(int id) {
    }

    default int[] bufferIndexes() {
        return internalBuffers().keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    default boolean addBuffer(GpuTextBuffer ram) {
        final boolean preexists = internalBuffers().containsKey(ram.id);
        internalBuffers().put(ram.id, ram);
        return preexists;
    }

    default int removeBuffers(int[] ids) {
        int count = 0;
        for (int id : ids) {
            if (internalBuffers().remove(id) != null) {
                onBufferRamDestroy(id);
                count += 1;
            }
        }
        return count;
    }

    default int removeAllBuffers() {
        return removeBuffers(bufferIndexes());
    }

    default void loadBuffer(String address, int id, CompoundTag nbt) {
        final li.cil.oc.util.TextBuffer src = new li.cil.oc.util.TextBuffer(1, 1, PackedColor.SingleBitFormat.INSTANCE);
        src.loadData(nbt);
        addBuffer(GpuTextBuffer.wrap(address, id, src));
    }

    default Optional<GpuTextBuffer> getBuffer(int id) {
        return Optional.ofNullable(internalBuffers().get(id));
    }

    default int nextAvailableBufferIndex() {
        int index = RESERVED_SCREEN_INDEX + 1;
        while (internalBuffers().containsKey(index)) {
            index += 1;
        }
        return index;
    }

    default int calculateUsedMemory() {
        int sum = 0;
        for (GpuTextBuffer buffer : internalBuffers().values()) {
            sum += buffer.data.width * buffer.data.height;
        }
        return sum;
    }
}
