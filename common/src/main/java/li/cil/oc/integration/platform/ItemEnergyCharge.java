package li.cil.oc.integration.platform;

import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.EnergyHandler;
import li.cil.oc.integration.util.Power;
import net.minecraft.world.item.ItemStack;

/**
 * Charging of items that expose a platform energy storage (Forge Energy /
 * Team Reborn Energy). Registered through {@code api.IMC.registerItemCharge}.
 * Formerly {@code integration.minecraftforge.EventHandlerMinecraftForge.canCharge/charge}.
 */
public final class ItemEnergyCharge {
    private ItemEnergyCharge() {
    }

    public static boolean canCharge(ItemStack stack) {
        final EnergyHandler storage = PlatformHooks.getEnergyHandler(stack);
        return storage != null && storage.canReceive();
    }

    public static double charge(ItemStack stack, double amount, boolean simulate) {
        final EnergyHandler storage = PlatformHooks.getEnergyHandler(stack);
        if (storage != null) {
            return amount - Power.fromFE(storage.receiveEnergy(Power.toFE(amount), simulate));
        }
        return amount;
    }
}
