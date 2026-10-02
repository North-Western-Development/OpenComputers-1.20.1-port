package li.cil.oc.common.component;

import li.cil.oc.common.component.traits.VideoRamRasterizer;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * Client side handling of GPU video RAM packets (was declared in GpuTextBuffer.scala).
 */
public final class ClientGpuTextBufferHandler {
    private ClientGpuTextBufferHandler() {
    }

    public static void bitblt(li.cil.oc.api.internal.TextBuffer dst, int col, int row, int w, int h, String owner, int srcId, int fromCol, int fromRow) {
        if (dst instanceof VideoRamRasterizer videoDevice) {
            final Optional<GpuTextBuffer> buffer = videoDevice.getBuffer(owner, srcId);
            // ignore if missing - got a bitblt for a missing buffer
            buffer.ifPresent(b -> GpuTextBuffer.bitblt(dst, col, row, w, h, b, fromCol, fromRow));
        }
        // else ignore - weird packet handler called this, should only happen for video ram aware devices
    }

    public static boolean removeBuffer(li.cil.oc.api.internal.TextBuffer buffer, String owner, int id) {
        if (buffer instanceof VideoRamRasterizer screen) return screen.removeBuffer(owner, id);
        return false; // ignore, not compatible with bitblts
    }

    public static boolean loadBuffer(li.cil.oc.api.internal.TextBuffer buffer, String owner, int id, CompoundTag nbt) {
        if (buffer instanceof VideoRamRasterizer screen) return screen.loadBuffer(owner, id, nbt);
        return false; // ignore, not compatible with bitblts
    }
}
