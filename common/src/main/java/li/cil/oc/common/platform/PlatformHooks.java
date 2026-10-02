package li.cil.oc.common.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
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

import javax.annotation.Nullable;

/**
 * Core loader-specific functionality. Implemented in
 * {@code li.cil.oc.common.platform.forge.PlatformHooksImpl} and
 * {@code li.cil.oc.common.platform.fabric.PlatformHooksImpl}.
 * <p>
 * Every method here is a static {@link ExpectPlatform} stub that the
 * Architectury transformer redirects to the matching static method in the
 * platform implementation class.
 */
public final class PlatformHooks {
    private PlatformHooks() {
    }

    // ----------------------------------------------------------------------- //
    // Item / fluid / energy transfer.

    /** Item storage of the block at {@code pos}, as seen from {@code side} (null = internal / any side). */
    @ExpectPlatform
    @Nullable
    public static ItemHandler getItemHandler(Level level, BlockPos pos, @Nullable Direction side) {
        throw new AssertionError();
    }

    /** Item storage of an entity (e.g. minecart with chest, a drone, a player). */
    @ExpectPlatform
    @Nullable
    public static ItemHandler getItemHandler(Entity entity, @Nullable Direction side) {
        throw new AssertionError();
    }

    /** Item storage inside an item stack (e.g. shulker boxes, backpacks), if the platform exposes one. */
    @ExpectPlatform
    @Nullable
    public static ItemHandler getItemHandler(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static FluidHandler getFluidHandler(Level level, BlockPos pos, @Nullable Direction side) {
        throw new AssertionError();
    }

    /** Fluid storage of an item; operates on a copy of the stack, see {@link ItemFluidHandler#getContainer()}. */
    @ExpectPlatform
    @Nullable
    public static ItemFluidHandler getFluidHandler(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static EnergyHandler getEnergyHandler(Level level, BlockPos pos, @Nullable Direction side) {
        throw new AssertionError();
    }

    /** Energy storage of an item (for chargers / battery upgrades). Operates on the given stack in place. */
    @ExpectPlatform
    @Nullable
    public static EnergyHandler getEnergyHandler(ItemStack stack) {
        throw new AssertionError();
    }

    // ----------------------------------------------------------------------- //
    // Players.

    @ExpectPlatform
    public static boolean isFakePlayer(Player player) {
        throw new AssertionError();
    }

    /**
     * Asks the platform's protection hooks (Forge {@code BlockEvent.BreakEvent},
     * Fabric {@code PlayerBlockBreakEvents.BEFORE}) whether the player may break
     * the block. Used by robots/drones before they dig.
     */
    @ExpectPlatform
    public static boolean canBreakBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        throw new AssertionError();
    }

    /** Forge {@code BlockEvent.EntityPlaceEvent}; always true on Fabric. */
    @ExpectPlatform
    public static boolean canPlaceBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        throw new AssertionError();
    }

    /** Forge's player reach attribute; vanilla's constant elsewhere. */
    @ExpectPlatform
    public static double getBlockReach(Player player) {
        throw new AssertionError();
    }

    // ----------------------------------------------------------------------- //
    // Items.

    /** Furnace burn time in ticks (Forge: {@code ForgeHooks.getBurnTime}, Fabric: {@code FuelRegistry}). */
    @ExpectPlatform
    public static int getBurnTime(ItemStack stack) {
        throw new AssertionError();
    }

    /** Item left over after using the stack in crafting (Forge: {@code getCraftingRemainingItem(stack)}). */
    @ExpectPlatform
    public static ItemStack getCraftingRemainder(ItemStack stack) {
        throw new AssertionError();
    }
}
