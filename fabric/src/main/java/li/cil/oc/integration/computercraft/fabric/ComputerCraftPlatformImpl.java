package li.cil.oc.integration.computercraft.fabric;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaLookup;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import dan200.computercraft.shared.computer.core.ServerContext;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.computercraft.PeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ComputerCraftPlatformImpl {
    private ComputerCraftPlatformImpl() {
    }

    public static void registerPeripheralProvider() {
        PeripheralLookup.get().registerForBlockEntity(
                (blockEntity, side) -> PeripheralProvider.getPeripheral(blockEntity.getLevel(), blockEntity.getBlockPos(), side),
                TileEntityTypes.RELAY.get());
    }

    public static IPeripheral getPeripheral(Level level, BlockPos pos, Direction side) {
        // Port note: PeripheralLookup finds all registered block peripherals (CC's own blocks and
        // other mods'), but not CC's generic inventory/fluid/energy peripherals; OC has its own
        // drivers for those.
        return PeripheralLookup.get().find(level, pos, side);
    }

    public static IMedia getMedia(ItemStack stack) {
        return stack.isEmpty() ? null : MediaLookup.get().find(stack, null);
    }

    public static MethodSupplier<PeripheralMethod> peripheralMethods(MinecraftServer server) {
        return ServerContext.get(server).peripheralMethods();
    }
}
