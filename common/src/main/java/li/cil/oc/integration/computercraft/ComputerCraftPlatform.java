package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.PeripheralMethod;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Loader-specific parts of the CC: Tweaked integration. Implemented in
 * {@code li.cil.oc.integration.computercraft.forge.ComputerCraftPlatformImpl} (capabilities,
 * {@code ForgeComputerCraftAPI}) and {@code li.cil.oc.integration.computercraft.fabric.ComputerCraftPlatformImpl}
 * ({@code PeripheralLookup} / {@code MediaLookup} block/item API lookups).
 * <p>
 * Only loaded by the ComputerCraft integration, i.e. when CC: Tweaked is installed.
 */
public final class ComputerCraftPlatform {
    private ComputerCraftPlatform() {
    }

    /**
     * Registers {@link PeripheralProvider} with CC: Tweaked, so that CC computers see OC blocks
     * (relays/access points) as peripherals.
     */
    @ExpectPlatform
    public static void registerPeripheralProvider() {
        throw new AssertionError();
    }

    /**
     * The CC peripheral provided by the block at {@code pos}, looked up from {@code side}
     * (the side of the block being accessed), or null.
     */
    @ExpectPlatform
    public static IPeripheral getPeripheral(Level level, BlockPos pos, Direction side) {
        throw new AssertionError();
    }

    /**
     * The CC media implementation for the stack (disks, treasure disks, computers, ...), or null.
     */
    @ExpectPlatform
    public static IMedia getMedia(ItemStack stack) {
        throw new AssertionError();
    }

    /**
     * CC's method supplier for peripherals ({@code @LuaFunction} methods, generic methods, and
     * {@code IDynamicPeripheral} methods), as used by CC computers on this server.
     */
    @ExpectPlatform
    public static MethodSupplier<PeripheralMethod> peripheralMethods(MinecraftServer server) {
        throw new AssertionError();
    }
}
