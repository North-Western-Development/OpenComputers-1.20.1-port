package li.cil.oc.fabric;

import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.common.transfer.EnergyHandlerProvider;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.EnergyStorage;

import javax.annotation.Nullable;

/**
 * Bridges between OC's {@link EnergyHandler} and Team Reborn Energy. Only
 * loaded when the {@code team_reborn_energy} mod is present.
 */
public final class TeamRebornEnergyCompat {
    private TeamRebornEnergyCompat() {
    }

    static void registerProviders() {
        EnergyStorage.SIDED.registerFallback((level, pos, state, blockEntity, side) -> {
            if (blockEntity instanceof EnergyHandlerProvider provider && FabricStorageProviders.isOurs(blockEntity)) {
                final EnergyHandler handler = provider.getEnergyHandler(side);
                return handler == null ? null : new ToTeamReborn(handler);
            }
            return null;
        });
    }

    @Nullable
    public static EnergyHandler find(Level level, BlockPos pos, @Nullable Direction side) {
        final EnergyStorage storage = EnergyStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new FromTeamReborn(storage, null);
    }

    @Nullable
    public static EnergyHandler find(ItemStack stack) {
        // The context works on a detached copy; changes are written back to the
        // original stack after each successful non-simulated operation.
        final ContainerItemContext context = ContainerItemContext.withInitial(stack);
        final EnergyStorage storage = EnergyStorage.ITEM.find(stack, context);
        return storage == null ? null : new FromTeamReborn(storage, () -> {
            final ItemStack result = context.getItemVariant().toStack((int) context.getAmount());
            stack.setTag(result.getTag());
        });
    }

    private record FromTeamReborn(EnergyStorage storage, @Nullable Runnable onCommit) implements EnergyHandler {
        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            try (Transaction tx = Transaction.openOuter()) {
                final long inserted = storage.insert(maxReceive, tx);
                if (!simulate) {
                    tx.commit();
                    if (onCommit != null) onCommit.run();
                }
                return inserted;
            }
        }

        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            try (Transaction tx = Transaction.openOuter()) {
                final long extracted = storage.extract(maxExtract, tx);
                if (!simulate) {
                    tx.commit();
                    if (onCommit != null) onCommit.run();
                }
                return extracted;
            }
        }

        @Override
        public long getEnergyStored() {
            return storage.getAmount();
        }

        @Override
        public long getMaxEnergyStored() {
            return storage.getCapacity();
        }

        @Override
        public boolean canExtract() {
            return storage.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return storage.supportsInsertion();
        }
    }

    /**
     * OC's handlers are not transactional, so changes are applied when the
     * outermost transaction commits; within a transaction we only simulate.
     */
    private static final class ToTeamReborn extends SnapshotParticipant<long[]> implements EnergyStorage {
        private final EnergyHandler handler;
        private long pendingInsert;
        private long pendingExtract;

        ToTeamReborn(EnergyHandler handler) {
            this.handler = handler;
        }

        @Override
        public boolean supportsInsertion() {
            return handler.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return handler.canExtract();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            final long accepted = handler.receiveEnergy(pendingInsert + maxAmount, true) - pendingInsert;
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            pendingInsert += accepted;
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            final long extracted = handler.extractEnergy(pendingExtract + maxAmount, true) - pendingExtract;
            if (extracted <= 0) return 0;
            updateSnapshots(transaction);
            pendingExtract += extracted;
            return extracted;
        }

        @Override
        public long getAmount() {
            return handler.getEnergyStored() + pendingInsert - pendingExtract;
        }

        @Override
        public long getCapacity() {
            return handler.getMaxEnergyStored();
        }

        @Override
        protected long[] createSnapshot() {
            return new long[]{pendingInsert, pendingExtract};
        }

        @Override
        protected void readSnapshot(long[] snapshot) {
            pendingInsert = snapshot[0];
            pendingExtract = snapshot[1];
        }

        @Override
        protected void onFinalCommit() {
            if (pendingInsert > 0) handler.receiveEnergy(pendingInsert, false);
            if (pendingExtract > 0) handler.extractEnergy(pendingExtract, false);
            pendingInsert = 0;
            pendingExtract = 0;
        }
    }
}
