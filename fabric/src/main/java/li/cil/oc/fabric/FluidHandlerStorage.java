package li.cil.oc.fabric;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.common.transfer.FluidHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Transfer API view of an OC {@link FluidHandler} (which works in millibuckets).
 * <p>
 * OC's handlers are not transactional, so changes are applied when the
 * outermost transaction commits; within a transaction we only simulate, based
 * on the accumulated pending amounts (like {@code TeamRebornEnergyCompat.ToTeamReborn}).
 * Amounts that are not a multiple of one millibucket are rounded down.
 */
final class FluidHandlerStorage extends SnapshotParticipant<FluidHandlerStorage.Pending> implements Storage<FluidVariant> {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    record Pending(Map<FluidVariant, Long> fills, Map<FluidVariant, Long> drains) {
        Pending copy() {
            return new Pending(new HashMap<>(fills), new HashMap<>(drains));
        }
    }

    private final FluidHandler handler;
    private Pending pending = new Pending(new HashMap<>(), new HashMap<>());

    FluidHandlerStorage(FluidHandler handler) {
        this.handler = handler;
    }

    private static FluidStack toStack(FluidVariant variant, long millibuckets) {
        return FluidStack.create(variant.getFluid(), millibuckets, variant.copyNbt());
    }

    private static FluidVariant toVariant(FluidStack stack) {
        return stack.isEmpty() ? FluidVariant.blank() : FluidVariant.of(stack.getFluid(), stack.getTag());
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        final long mb = maxAmount / DROPLETS_PER_MB;
        if (mb <= 0) return 0;
        final long already = pending.fills().getOrDefault(resource, 0L);
        final long accepted = Math.min(mb, handler.fill(toStack(resource, already + mb), true) - already);
        if (accepted <= 0) return 0;
        updateSnapshots(transaction);
        pending.fills().put(resource, already + accepted);
        return accepted * DROPLETS_PER_MB;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        final long mb = maxAmount / DROPLETS_PER_MB;
        if (mb <= 0) return 0;
        final long already = pending.drains().getOrDefault(resource, 0L);
        final FluidStack drained = handler.drain(toStack(resource, already + mb), true);
        if (drained.isEmpty() || !toVariant(drained).equals(resource)) return 0;
        final long extracted = Math.min(mb, drained.getAmount() - already);
        if (extracted <= 0) return 0;
        updateSnapshots(transaction);
        pending.drains().put(resource, already + extracted);
        return extracted * DROPLETS_PER_MB;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        final List<StorageView<FluidVariant>> views = new ArrayList<>();
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            views.add(new TankView(tank));
        }
        return views.iterator();
    }

    @Override
    protected Pending createSnapshot() {
        return pending.copy();
    }

    @Override
    protected void readSnapshot(Pending snapshot) {
        pending = snapshot;
    }

    @Override
    protected void onFinalCommit() {
        final Pending committed = pending;
        pending = new Pending(new HashMap<>(), new HashMap<>());
        committed.fills().forEach((variant, mb) -> handler.fill(toStack(variant, mb), false));
        committed.drains().forEach((variant, mb) -> handler.drain(toStack(variant, mb), false));
    }

    private final class TankView implements StorageView<FluidVariant> {
        private final int tank;

        TankView(int tank) {
            this.tank = tank;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || !resource.equals(getResource())) return 0;
            return FluidHandlerStorage.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            return toVariant(handler.getFluidInTank(tank));
        }

        @Override
        public long getAmount() {
            final FluidStack stack = handler.getFluidInTank(tank);
            return stack.isEmpty() ? 0 : stack.getAmount() * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            return handler.getTankCapacity(tank) * DROPLETS_PER_MB;
        }
    }
}
