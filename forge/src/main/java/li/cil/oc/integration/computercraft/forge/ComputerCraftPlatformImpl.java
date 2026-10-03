package li.cil.oc.integration.computercraft.forge;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.impl.MediaProviders;
import dan200.computercraft.shared.computer.core.ServerContext;
import li.cil.oc.integration.computercraft.PeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public final class ComputerCraftPlatformImpl {
    private ComputerCraftPlatformImpl() {
    }

    // Same capability instance as dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL.
    private static final Capability<IPeripheral> CAPABILITY_PERIPHERAL = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void registerPeripheralProvider() {
        ForgeComputerCraftAPI.registerPeripheralProvider((level, pos, side) -> {
            final IPeripheral peripheral = PeripheralProvider.getPeripheral(level, pos, side);
            return peripheral != null ? LazyOptional.of(() -> peripheral) : LazyOptional.empty();
        });
    }

    public static IPeripheral getPeripheral(Level level, BlockPos pos, Direction side) {
        // Port note: only peripherals exposed as capability are found (all of CC's own blocks and
        // most mods' peripherals). Generic peripherals (CC's inventory/fluid/energy wrappers) and
        // IPeripheralProvider-based ones are not, as OC has its own drivers for those.
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        return blockEntity.getCapability(CAPABILITY_PERIPHERAL, side).orElse(null);
    }

    public static IMedia getMedia(ItemStack stack) {
        // Internal, but the same lookup CC's own disk drive uses (item providers + MediaProviders).
        return MediaProviders.get(stack);
    }

    public static MethodSupplier<PeripheralMethod> peripheralMethods(MinecraftServer server) {
        return ServerContext.get(server).peripheralMethods();
    }
}
