package li.cil.oc.common.component.traits;

import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.util.PackedColor;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Something that can display GPU video RAM buffers (screens), Scala trait {@code VideoRamRasterizer}.
 * <p>
 * Stateful trait: implementers provide the backing map through {@link #videoRamDevices()},
 * e.g. {@code private final Map<String, VideoRamDevice> videoRamDevices = new HashMap<>();}.
 */
public interface VideoRamRasterizer {
    final class VirtualRamDevice implements VideoRamDevice {
        public final String owner;
        private final Map<Integer, GpuTextBuffer> internalBuffers = new HashMap<>();

        public VirtualRamDevice(String owner) {
            this.owner = owner;
        }

        @Override
        public Map<Integer, GpuTextBuffer> internalBuffers() {
            return internalBuffers;
        }
    }

    /**
     * Backing storage: the buffers of each owning GPU, by GPU address. Must be mutable.
     * (Scala: {@code private val internalBuffers}.)
     */
    Map<String, VideoRamDevice> videoRamDevices();

    void onBufferRamInit(GpuTextBuffer ram);

    void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow);

    void onBufferRamDestroy(GpuTextBuffer ram);

    default boolean addBuffer(GpuTextBuffer ram) {
        final VideoRamDevice gpu = videoRamDevices().computeIfAbsent(ram.owner, VirtualRamDevice::new);
        final boolean preexists = gpu.addBuffer(ram);
        if (!preexists || ram.dirty) {
            onBufferRamInit(ram);
        }
        return preexists;
    }

    default boolean removeBuffer(String owner, int id) {
        final VideoRamDevice gpu = videoRamDevices().get(owner);
        if (gpu != null) {
            final Optional<GpuTextBuffer> ram = gpu.getBuffer(id);
            if (ram.isPresent()) {
                onBufferRamDestroy(ram.get());
                return gpu.removeBuffers(new int[]{id}) == 1;
            }
        }
        return false;
    }

    default int removeAllBuffers(String owner) {
        int count = 0;
        final VideoRamDevice gpu = videoRamDevices().get(owner);
        if (gpu != null) {
            final int[] ids = gpu.bufferIndexes();
            for (int id : ids) {
                if (removeBuffer(owner, id)) {
                    count += 1;
                }
            }
        }
        return count;
    }

    default int removeAllBuffers() {
        int count = 0;
        for (String owner : new ArrayList<>(videoRamDevices().keySet())) {
            count += removeAllBuffers(owner);
        }
        return count;
    }

    default boolean loadBuffer(String owner, int id, CompoundTag nbt) {
        final li.cil.oc.util.TextBuffer src = new li.cil.oc.util.TextBuffer(1, 1, PackedColor.SingleBitFormat.INSTANCE);
        src.loadData(nbt);
        return addBuffer(GpuTextBuffer.wrap(owner, id, src));
    }

    default Optional<GpuTextBuffer> getBuffer(String owner, int id) {
        final VideoRamDevice gpu = videoRamDevices().get(owner);
        return gpu != null ? gpu.getBuffer(id) : Optional.empty();
    }
}
