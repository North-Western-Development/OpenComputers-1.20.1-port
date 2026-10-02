package li.cil.oc.forge;

import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import li.cil.oc.OpenComputers;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.transfer.ContainerItemHandler;
import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.common.transfer.EnergyHandlerProvider;
import li.cil.oc.common.transfer.FluidHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Exposes OpenComputers' block entities to other Forge mods through capabilities:
 * inventories (vanilla {@link Container}) as {@code ITEM_HANDLER}, tanks
 * ({@link FluidHandler}) as {@code FLUID_HANDLER} and power acceptors as
 * {@code ENERGY}. Chargeable OC items get an {@code ENERGY} capability.
 */
public final class ForgeCapabilityProviders {
    private static final ResourceLocation ITEMS = new ResourceLocation(OpenComputers.ID, "items");
    private static final ResourceLocation ENERGY = new ResourceLocation(OpenComputers.ID, "energy");
    private static final ResourceLocation FLUIDS = new ResourceLocation(OpenComputers.ID, "fluids");

    private ForgeCapabilityProviders() {
    }

    @SubscribeEvent
    public static void onAttachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        final BlockEntity blockEntity = event.getObject();
        if (!isOurs(blockEntity)) {
            return;
        }

        if (blockEntity instanceof Container container) {
            attach(event, ITEMS, new SidedProvider<>(ForgeCapabilities.ITEM_HANDLER, side -> new ItemHandlerAdapter(new ContainerItemHandler(container, side))));
        }

        if (blockEntity instanceof EnergyHandlerProvider energy) {
            attach(event, ENERGY, new SidedProvider<>(ForgeCapabilities.ENERGY, side -> {
                final EnergyHandler handler = energy.getEnergyHandler(side);
                return handler == null ? null : new EnergyStorageAdapter(handler);
            }));
        }

        if (blockEntity instanceof FluidHandler fluids) {
            final FluidHandlerAdapter adapter = new FluidHandlerAdapter(fluids);
            attach(event, FLUIDS, new SidedProvider<>(ForgeCapabilities.FLUID_HANDLER, side -> adapter));
        }
    }

    @SubscribeEvent
    public static void onAttachItemStackCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        final ItemStack stack = event.getObject();
        if (stack.getItem() instanceof Chargeable chargeable) {
            final EnergyStorageAdapter adapter = new EnergyStorageAdapter(new Chargeable.Provider(stack, chargeable));
            event.addCapability(Chargeable.KEY, new SidedProvider<>(ForgeCapabilities.ENERGY, side -> adapter));
        }
    }

    private static void attach(AttachCapabilitiesEvent<?> event, ResourceLocation key, SidedProvider<?> provider) {
        event.addCapability(key, provider);
        // Called when the block entity is removed (invalidateCaps).
        event.addListener(provider::invalidate);
    }

    private static boolean isOurs(BlockEntity blockEntity) {
        return blockEntity.getClass().getName().startsWith("li.cil.oc.");
    }

    private interface SideFactory<T> {
        @Nullable
        T create(@Nullable Direction side);
    }

    private static final class SidedProvider<T> implements ICapabilityProvider {
        private final Capability<T> capability;
        private final SideFactory<T> factory;
        private final Map<Direction, LazyOptional<T>> bySide = new EnumMap<>(Direction.class);
        private LazyOptional<T> unsided;

        private SidedProvider(Capability<T> capability, SideFactory<T> factory) {
            this.capability = capability;
            this.factory = factory;
        }

        @Override
        public <C> @NotNull LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
            if (cap != capability) {
                return LazyOptional.empty();
            }
            final LazyOptional<T> result;
            if (side == null) {
                if (unsided == null) unsided = create(null);
                result = unsided;
            } else {
                result = bySide.computeIfAbsent(side, this::create);
            }
            return result.cast();
        }

        private void invalidate() {
            if (unsided != null) unsided.invalidate();
            bySide.values().forEach(LazyOptional::invalidate);
            unsided = null;
            bySide.clear();
        }

        private LazyOptional<T> create(@Nullable Direction side) {
            final T value = factory.create(side);
            return value == null ? LazyOptional.empty() : LazyOptional.of(() -> value);
        }
    }

    private record ItemHandlerAdapter(ContainerItemHandler handler) implements IItemHandlerModifiable {
        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            handler.getContainer().setItem(slot, stack);
        }
    }

    private record EnergyStorageAdapter(EnergyHandler handler) implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return (int) handler.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return (int) handler.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, handler.getEnergyStored());
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, handler.getMaxEnergyStored());
        }

        @Override
        public boolean canExtract() {
            return handler.canExtract();
        }

        @Override
        public boolean canReceive() {
            return handler.canReceive();
        }
    }

    /**
     * Forge view of an OC {@link FluidHandler}. Both use millibuckets, as does
     * Architectury's {@code FluidStack} on Forge.
     */
    private record FluidHandlerAdapter(FluidHandler handler) implements IFluidHandler {
        private static int clamp(long value) {
            return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
        }

        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return FluidStackHooksForge.toForge(handler.getFluidInTank(tank));
        }

        @Override
        public int getTankCapacity(int tank) {
            return clamp(handler.getTankCapacity(tank));
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return handler.isFluidValid(tank, FluidStackHooksForge.fromForge(stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return 0;
            return clamp(handler.fill(FluidStackHooksForge.fromForge(resource), action.simulate()));
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            return FluidStackHooksForge.toForge(handler.drain(FluidStackHooksForge.fromForge(resource), action.simulate()));
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            return FluidStackHooksForge.toForge(handler.drain(maxDrain, action.simulate()));
        }
    }
}
