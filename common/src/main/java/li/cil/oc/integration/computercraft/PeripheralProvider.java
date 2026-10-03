package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.common.tileentity.Relay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Exposes OC blocks as CC peripherals. Registered with CC: Tweaked through
 * {@link ComputerCraftPlatform#registerPeripheralProvider()} (Forge: {@code IPeripheralProvider},
 * Fabric: {@code PeripheralLookup}).
 */
public final class PeripheralProvider {
    private PeripheralProvider() {
    }

    public static void init() {
        ComputerCraftPlatform.registerPeripheralProvider();
        Relay.packetObserver = RelayPeripheral::onRelayPacket;
    }

    public static IPeripheral getPeripheral(Level level, BlockPos pos, Direction side) {
        if (level == null || level.isClientSide) return null;
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof Relay relay) {
            return new RelayPeripheral(relay);
        }
        return null;
    }
}
