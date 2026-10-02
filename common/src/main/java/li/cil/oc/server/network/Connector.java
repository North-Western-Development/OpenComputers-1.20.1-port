package li.cil.oc.server.network;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.NodeData;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

/**
 * Mutable connector node. Port note: the Scala {@code var}s are accessed via
 * {@link #localBuffer()} / {@link #setLocalBuffer(double)},
 * {@link #localBufferSize()} / {@link #setLocalBufferSizeRaw(double)} (raw field
 * write; {@link #setLocalBufferSize(double)} is the API method with
 * distributor bookkeeping) and {@link #distributor()} / {@link #setDistributor(Optional)}.
 */
public interface Connector extends li.cil.oc.api.network.Connector, Node {
    void setLocalBuffer(double value);

    void setLocalBufferSizeRaw(double value);

    Optional<Distributor> distributor();

    void setDistributor(Optional<Distributor> value);

    // ----------------------------------------------------------------------- //

    @Override
    default double globalBuffer() {
        final Optional<Distributor> distributor = distributor();
        return distributor.isPresent() ? distributor.get().globalBuffer() : localBuffer();
    }

    @Override
    default double globalBufferSize() {
        final Optional<Distributor> distributor = distributor();
        return distributor.isPresent() ? distributor.get().globalBufferSize() : localBufferSize();
    }

    // ----------------------------------------------------------------------- //

    @Override
    default double changeBuffer(double delta) {
        if (delta == 0) return 0;
        else if (Settings.get().ignorePower) {
            if (delta < 0) return 0;
            else /* if (delta > 0) */ return delta;
        } else {
            synchronized (this) {
                final Optional<Distributor> distributor = distributor();
                if (distributor.isPresent()) {
                    final Distributor d = distributor.get();
                    synchronized (d) {
                        return d.changeBuffer(change(delta));
                    }
                } else return change(delta);
            }
        }
    }

    private double change(double delta) {
        if (localBufferSize() <= 0) return delta;
        final double oldBuffer = localBuffer();
        setLocalBuffer(localBuffer() + delta);
        final double remaining;
        if (localBuffer() < 0) {
            remaining = localBuffer();
            setLocalBuffer(0);
        } else if (localBuffer() > localBufferSize()) {
            remaining = localBuffer() - localBufferSize();
            setLocalBuffer(localBufferSize());
        } else remaining = 0;
        if (localBuffer() != oldBuffer) {
            distributor().ifPresent(d ->
                    d.setGlobalBuffer(Math.max(0, Math.min(d.globalBufferSize(), d.globalBuffer() - oldBuffer + localBuffer()))));
        }
        return remaining;
    }

    @Override
    default boolean tryChangeBuffer(double delta) {
        if (delta == 0) return true;
        else if (Settings.get().ignorePower) return delta < 0;
        else {
            synchronized (this) {
                final Optional<Distributor> distributor = distributor();
                if (distributor.isPresent()) {
                    final Distributor d = distributor.get();
                    synchronized (d) {
                        if (localBuffer() > localBufferSize()) {
                            d.changeBuffer(localBuffer() - localBufferSize());
                            setLocalBuffer(localBufferSize());
                        }
                        final double newGlobalBuffer = globalBuffer() + delta;
                        return (delta > 0 || newGlobalBuffer >= 0) && (delta < 0 || newGlobalBuffer <= globalBufferSize()) && d.changeBuffer(delta) == 0;
                    }
                } else {
                    final double newLocalBuffer = localBuffer() + delta;
                    if ((delta < 0 && newLocalBuffer < 0) || (delta > 0 && newLocalBuffer > localBufferSize())) {
                        return false;
                    } else {
                        setLocalBuffer(newLocalBuffer);
                        return true;
                    }
                }
            }
        }
    }

    @Override
    default void setLocalBufferSize(double size) {
        final double clampedSize = Math.max(size, 0);
        synchronized (this) {
            final Optional<Distributor> distributor = distributor();
            if (distributor.isPresent()) {
                final Distributor d = distributor.get();
                synchronized (d) {
                    final double oldSize = localBufferSize();
                    // Must apply new size before trying to register with distributor, else
                    // we get ignored if our size is zero.
                    setLocalBufferSizeRaw(clampedSize);
                    if (network() != null) {
                        if (oldSize <= 0 && clampedSize > 0) d.addConnector(this);
                        else if (oldSize > 0 && clampedSize == 0) d.removeConnector(this);
                        else d.setGlobalBufferSize(Math.max(d.globalBufferSize() - oldSize + clampedSize, 0));
                    }
                    final double surplus = Math.max(localBuffer() - clampedSize, 0);
                    changeBuffer(-surplus);
                    d.changeBuffer(surplus);
                }
            } else {
                setLocalBufferSizeRaw(clampedSize);
                setLocalBuffer(Math.min(localBuffer(), localBufferSize()));
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void onDisconnect(li.cil.oc.api.network.Node node) {
        Node.super.onDisconnect(node);
        if (node == this) {
            synchronized (this) {
                setDistributor(Optional.empty());
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    default void loadData(CompoundTag nbt) {
        Node.super.loadData(nbt);
        setLocalBuffer(nbt.getDouble(NodeData.BufferTag));
    }

    @Override
    default void saveData(CompoundTag nbt) {
        Node.super.saveData(nbt);
        nbt.putDouble(NodeData.BufferTag, Math.min(localBuffer(), localBufferSize()));
    }
}
