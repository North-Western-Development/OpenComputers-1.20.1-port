package li.cil.oc.common.tileentity.traits;

import li.cil.oc.Settings;
import li.cil.oc.server.PacketSender;
import net.minecraft.nbt.CompoundTag;

/**
 * Scala {@code globalBuffer} / {@code globalBuffer_=} are {@link #globalBuffer()} /
 * {@link #setGlobalBuffer(double)} (same for {@code globalBufferSize}).
 */
public interface PowerInformation extends TileEntityTrait {
    final class State {
        public double lastSentRatio = -1.0;
        public int ticksUntilSync = 0;
    }

    String GlobalBufferTag = Settings.namespace + "globalBuffer";
    String GlobalBufferSizeTag = Settings.namespace + "globalBufferSize";

    /** Provided by {@link TileEntity}. */
    State powerInformationState();

    double globalBuffer();

    void setGlobalBuffer(double value);

    double globalBufferSize();

    void setGlobalBufferSize(double value);

    default void updatePowerInformation() {
        final double ratio = globalBufferSize() > 0 ? globalBuffer() / globalBufferSize() : 0;
        if (shouldSync(ratio) || hasChangedSignificantly(ratio)) {
            powerInformationState().lastSentRatio = ratio;
            PacketSender.sendPowerState(this);
        }
    }

    private boolean hasChangedSignificantly(double ratio) {
        final double lastSentRatio = powerInformationState().lastSentRatio;
        return lastSentRatio < 0 || Math.abs(lastSentRatio - ratio) > (5.0 / 100.0);
    }

    private boolean shouldSync(double ratio) {
        final State state = powerInformationState();
        state.ticksUntilSync -= 1;
        if (state.ticksUntilSync <= 0) {
            state.ticksUntilSync = Math.max((int) (100 / Settings.get().tickFrequency), 1);
            return state.lastSentRatio != ratio;
        } else return false;
    }

    // ----------------------------------------------------------------------- //

    static void onLoadForClient(PowerInformation self, CompoundTag nbt) {
        self.setGlobalBuffer(nbt.getDouble(GlobalBufferTag));
        self.setGlobalBufferSize(nbt.getDouble(GlobalBufferSizeTag));
    }

    static void onSaveForClient(PowerInformation self, CompoundTag nbt) {
        self.powerInformationState().lastSentRatio = self.globalBufferSize() > 0 ? self.globalBuffer() / self.globalBufferSize() : 0;
        nbt.putDouble(GlobalBufferTag, self.globalBuffer());
        nbt.putDouble(GlobalBufferSizeTag, self.globalBufferSize());
    }
}
