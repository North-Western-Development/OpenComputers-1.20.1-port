package li.cil.oc.common.platform.forge;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.common.transfer.ItemFluidHandler;
import li.cil.oc.common.transfer.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public final class PlatformHooksImpl {
    private PlatformHooksImpl() {
    }

    // ----------------------------------------------------------------------- //

    @Nullable
    private static <T> T find(@Nullable ICapabilityProvider provider, Capability<T> capability, @Nullable Direction side) {
        if (provider == null) return null;
        return provider.getCapability(capability, side).resolve().orElse(null);
    }

    @Nullable
    private static BlockEntity blockEntity(Level level, BlockPos pos) {
        return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

    @Nullable
    public static ItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        final IItemHandler handler = find(blockEntity(level, pos), ForgeCapabilities.ITEM_HANDLER, side);
        return handler == null ? null : new ForgeItemHandler(handler);
    }

    @Nullable
    public static ItemHandler getItemHandler(Entity entity, @Nullable Direction side) {
        final IItemHandler handler = find(entity, ForgeCapabilities.ITEM_HANDLER, side);
        return handler == null ? null : new ForgeItemHandler(handler);
    }

    @Nullable
    public static ItemHandler getItemHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        final IItemHandler handler = find(stack, ForgeCapabilities.ITEM_HANDLER, null);
        return handler == null ? null : new ForgeItemHandler(handler);
    }

    @Nullable
    public static FluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        final IFluidHandler handler = find(blockEntity(level, pos), ForgeCapabilities.FLUID_HANDLER, side);
        return handler == null ? null : new ForgeFluidHandler(handler);
    }

    @Nullable
    public static ItemFluidHandler getFluidHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        final ItemStack copy = stack.copy();
        final IFluidHandlerItem handler = find(copy, ForgeCapabilities.FLUID_HANDLER_ITEM, null);
        return handler == null ? null : new ForgeItemFluidHandler(handler);
    }

    @Nullable
    public static EnergyHandler getEnergyHandler(Level level, BlockPos pos, @Nullable Direction side) {
        final IEnergyStorage storage = find(blockEntity(level, pos), ForgeCapabilities.ENERGY, side);
        return storage == null ? null : new ForgeEnergyHandler(storage);
    }

    @Nullable
    public static EnergyHandler getEnergyHandler(ItemStack stack) {
        if (stack.isEmpty()) return null;
        final IEnergyStorage storage = find(stack, ForgeCapabilities.ENERGY, null);
        return storage == null ? null : new ForgeEnergyHandler(storage);
    }

    // ----------------------------------------------------------------------- //

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer || player instanceof li.cil.oc.server.agent.Player;
    }

    public static boolean canBreakBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        final BlockState state = level.getBlockState(pos);
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player));
    }

    public static boolean canPlaceBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return !ForgeEventFactory.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, pos), Direction.UP);
    }

    public static double getBlockReach(Player player) {
        return player.getBlockReach();
    }

    // ----------------------------------------------------------------------- //

    public static int getBurnTime(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, null);
    }

    public static ItemStack getCraftingRemainder(ItemStack stack) {
        return stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
    }

    // ----------------------------------------------------------------------- //

    private record ForgeItemHandler(IItemHandler handler) implements ItemHandler {
        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }
    }

    private static class ForgeFluidHandler implements FluidHandler {
        protected final IFluidHandler handler;

        ForgeFluidHandler(IFluidHandler handler) {
            this.handler = handler;
        }

        private static IFluidHandler.FluidAction action(boolean simulate) {
            return simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
        }

        // Forge fluid amounts are already millibuckets, as are Architectury's on Forge.

        @Override
        public int getTanks() {
            return handler.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStackHooksForge.fromForge(handler.getFluidInTank(tank));
        }

        @Override
        public long getTankCapacity(int tank) {
            return handler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return handler.isFluidValid(tank, FluidStackHooksForge.toForge(stack));
        }

        @Override
        public long fill(FluidStack resource, boolean simulate) {
            return handler.fill(FluidStackHooksForge.toForge(resource), action(simulate));
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            return FluidStackHooksForge.fromForge(handler.drain(FluidStackHooksForge.toForge(resource), action(simulate)));
        }

        @Override
        public FluidStack drain(long maxDrain, boolean simulate) {
            return FluidStackHooksForge.fromForge(handler.drain((int) Math.min(Integer.MAX_VALUE, maxDrain), action(simulate)));
        }
    }

    private static final class ForgeItemFluidHandler extends ForgeFluidHandler implements ItemFluidHandler {
        ForgeItemFluidHandler(IFluidHandlerItem handler) {
            super(handler);
        }

        @Override
        public ItemStack getContainer() {
            return ((IFluidHandlerItem) handler).getContainer();
        }
    }

    private record ForgeEnergyHandler(IEnergyStorage storage) implements EnergyHandler {
        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            return storage.receiveEnergy((int) Math.min(Integer.MAX_VALUE, maxReceive), simulate);
        }

        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            return storage.extractEnergy((int) Math.min(Integer.MAX_VALUE, maxExtract), simulate);
        }

        @Override
        public long getEnergyStored() {
            return storage.getEnergyStored();
        }

        @Override
        public long getMaxEnergyStored() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public boolean canReceive() {
            return storage.canReceive();
        }
    }
}
