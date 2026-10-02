package li.cil.oc.common.platform.fabric;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.common.transfer.ContainerItemHandler;
import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.common.transfer.ItemFluidHandler;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.fabric.TeamRebornEnergyCompat;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class PlatformHooksImpl {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private PlatformHooksImpl() {
    }

    // ----------------------------------------------------------------------- //

    @Nullable
    public static ItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        if (!level.isLoaded(pos)) return null;
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            // Exact slot semantics for vanilla-style inventories.
            return new ContainerItemHandler(container, side);
        }
        final Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new StorageItemHandler(storage);
    }

    @Nullable
    public static ItemHandler getItemHandler(Entity entity, @Nullable Direction side) {
        if (entity instanceof Player player) {
            return new ContainerItemHandler(player.getInventory());
        }
        if (entity instanceof Container container) {
            return new ContainerItemHandler(container, side);
        }
        return null;
    }

    @Nullable
    public static ItemHandler getItemHandler(ItemStack stack) {
        // Fabric has no standard item-in-item storage lookup.
        return null;
    }

    @Nullable
    public static FluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        if (!level.isLoaded(pos)) return null;
        final Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, side);
        return storage == null ? null : new StorageFluidHandler(storage);
    }

    @Nullable
    public static ItemFluidHandler getFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        final ContainerItemContext context = ContainerItemContext.withInitial(stack.copy());
        final Storage<FluidVariant> storage = context.find(FluidStorage.ITEM);
        return storage == null ? null : new StorageItemFluidHandler(storage, context);
    }

    @Nullable
    public static EnergyHandler getEnergyHandler(Level level, BlockPos pos, @Nullable Direction side) {
        if (!FabricLoader.getInstance().isModLoaded("team_reborn_energy") || !level.isLoaded(pos)) return null;
        return TeamRebornEnergyCompat.find(level, pos, side);
    }

    @Nullable
    public static EnergyHandler getEnergyHandler(ItemStack stack) {
        if (!FabricLoader.getInstance().isModLoaded("team_reborn_energy") || stack.isEmpty()) return null;
        return TeamRebornEnergyCompat.find(stack);
    }

    // ----------------------------------------------------------------------- //

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    public static boolean canBreakBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        final BlockState state = level.getBlockState(pos);
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }

    public static boolean canPlaceBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return true;
    }

    public static double getBlockReach(Player player) {
        return player.isCreative() ? 5.0 : 4.5;
    }

    // ----------------------------------------------------------------------- //

    public static int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        final Integer time = FuelRegistry.INSTANCE.get(stack.getItem());
        return time == null ? 0 : time;
    }

    public static ItemStack getCraftingRemainder(ItemStack stack) {
        return stack.getRecipeRemainder();
    }

    // ----------------------------------------------------------------------- //

    /**
     * Slot view over a Transfer API storage. Slotted storages map 1:1; other
     * storages expose their current views as pseudo-slots.
     */
    private static final class StorageItemHandler implements ItemHandler {
        private final Storage<ItemVariant> storage;

        StorageItemHandler(Storage<ItemVariant> storage) {
            this.storage = storage;
        }

        private List<StorageView<ItemVariant>> views() {
            final List<StorageView<ItemVariant>> result = new ArrayList<>();
            if (storage instanceof SlottedStorage<ItemVariant> slotted) {
                result.addAll(slotted.getSlots());
            } else {
                for (StorageView<ItemVariant> view : storage) {
                    result.add(view);
                }
            }
            return result;
        }

        @Nullable
        private StorageView<ItemVariant> view(int slot) {
            if (slot < 0) return null;
            if (storage instanceof SlottedStorage<ItemVariant> slotted) {
                return slot < slotted.getSlotCount() ? slotted.getSlot(slot) : null;
            }
            final List<StorageView<ItemVariant>> views = views();
            return slot < views.size() ? views.get(slot) : null;
        }

        @Override
        public int getSlots() {
            if (storage instanceof SlottedStorage<ItemVariant> slotted) return slotted.getSlotCount();
            return views().size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            final StorageView<ItemVariant> view = view(slot);
            if (view == null || view.isResourceBlank()) return ItemStack.EMPTY;
            return view.getResource().toStack((int) Math.min(Integer.MAX_VALUE, view.getAmount()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            final StorageView<ItemVariant> view = view(slot);
            @SuppressWarnings("unchecked")
            final Storage<ItemVariant> target = view instanceof SingleSlotStorage<?> single ? (Storage<ItemVariant>) single : storage;
            try (Transaction tx = Transaction.openOuter()) {
                final long inserted = target.insert(ItemVariant.of(stack), stack.getCount(), tx);
                if (!simulate) tx.commit();
                final ItemStack remainder = stack.copy();
                remainder.shrink((int) inserted);
                return remainder;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            final StorageView<ItemVariant> view = view(slot);
            if (view == null || view.isResourceBlank() || amount <= 0) return ItemStack.EMPTY;
            final ItemVariant resource = view.getResource();
            try (Transaction tx = Transaction.openOuter()) {
                final long extracted = view.extract(resource, amount, tx);
                if (!simulate) tx.commit();
                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack((int) extracted);
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            final StorageView<ItemVariant> view = view(slot);
            return view == null ? 0 : (int) Math.min(Integer.MAX_VALUE, view.getCapacity());
        }
    }

    private static class StorageFluidHandler implements FluidHandler {
        protected final Storage<FluidVariant> storage;

        StorageFluidHandler(Storage<FluidVariant> storage) {
            this.storage = storage;
        }

        protected void onCommit() {
        }

        private List<StorageView<FluidVariant>> views() {
            final List<StorageView<FluidVariant>> result = new ArrayList<>();
            for (StorageView<FluidVariant> view : storage) {
                result.add(view);
            }
            return result;
        }

        private static FluidStack toStack(FluidVariant variant, long droplets) {
            if (variant.isBlank() || droplets <= 0) return FluidStack.empty();
            return FluidStack.create(variant.getFluid(), droplets / DROPLETS_PER_MB, variant.getNbt());
        }

        private static FluidVariant toVariant(FluidStack stack) {
            return FluidVariant.of(stack.getFluid(), stack.getTag());
        }

        @Override
        public int getTanks() {
            return views().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            final List<StorageView<FluidVariant>> views = views();
            if (tank < 0 || tank >= views.size()) return FluidStack.empty();
            final StorageView<FluidVariant> view = views.get(tank);
            return toStack(view.getResource(), view.getAmount());
        }

        @Override
        public long getTankCapacity(int tank) {
            final List<StorageView<FluidVariant>> views = views();
            if (tank < 0 || tank >= views.size()) return 0;
            return views.get(tank).getCapacity() / DROPLETS_PER_MB;
        }

        @Override
        public long fill(FluidStack resource, boolean simulate) {
            if (resource.isEmpty() || resource.getFluid() == Fluids.EMPTY) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                final long inserted = storage.insert(toVariant(resource), resource.getAmount() * DROPLETS_PER_MB, tx);
                if (!simulate) {
                    tx.commit();
                    onCommit();
                }
                return inserted / DROPLETS_PER_MB;
            }
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            if (resource.isEmpty()) return FluidStack.empty();
            final FluidVariant variant = toVariant(resource);
            try (Transaction tx = Transaction.openOuter()) {
                final long extracted = storage.extract(variant, resource.getAmount() * DROPLETS_PER_MB, tx);
                if (!simulate) {
                    tx.commit();
                    onCommit();
                }
                return toStack(variant, extracted);
            }
        }

        @Override
        public FluidStack drain(long maxDrain, boolean simulate) {
            for (StorageView<FluidVariant> view : views()) {
                if (!view.isResourceBlank() && view.getAmount() > 0) {
                    return drain(toStack(view.getResource(), maxDrain * DROPLETS_PER_MB), simulate);
                }
            }
            return FluidStack.empty();
        }
    }

    private static final class StorageItemFluidHandler extends StorageFluidHandler implements ItemFluidHandler {
        private final ContainerItemContext context;

        StorageItemFluidHandler(Storage<FluidVariant> storage, ContainerItemContext context) {
            super(storage);
            this.context = context;
        }

        @Override
        public ItemStack getContainer() {
            return context.getItemVariant().toStack((int) context.getAmount());
        }
    }
}
