package li.cil.oc.server.component.traits;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.common.transfer.ItemFluidHandler;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.FluidUtils;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.BiFunction;

import static li.cil.oc.util.ResultWrapper.result;

public interface TankInventoryControl extends WorldAware, InventoryAware, TankAware {
    // Declared abstractly by InventoryAware and as a default by WorldAware; resolve the clash.
    @Override
    default net.minecraft.world.entity.player.Player fakePlayer() {
        return WorldAware.super.fakePlayer();
    }

    @Callback(doc = "function([slot:number]):number -- Get the amount of fluid in the tank item in the specified slot or the selected slot.")
    default Object[] getTankLevelInSlot(Context context, Arguments args) {
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> result(fluid.map(FluidStack::getAmount).orElse(0L)));
    }

    @Callback(doc = "function([slot:number]):number -- Get the capacity of the tank item in the specified slot of the robot or the selected slot.")
    default Object[] getTankCapacityInSlot(Context context, Arguments args) {
        return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> result(capacity));
    }

    @Callback(doc = "function([slot:number]):table -- Get a description of the fluid in the tank item in the specified slot or the selected slot.")
    default Object[] getFluidInTankInSlot(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            return withFluidInfo(optSlot(args, 0), (fluid, capacity) -> result(fluid.orElse(null)));
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function([tank:number]):table -- Get a description of the fluid in the tank in the specified slot or the selected slot.")
    default Object[] getFluidInInternalTank(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            return result(Optional.ofNullable(tank().getFluidTank(optTank(args, 0))).map(t -> t.getFluidInTank(0)).orElse(null));
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function([amount:number]):boolean -- Transfers fluid from a tank in the selected inventory slot to the selected tank.")
    default Object[] drain(Context context, Arguments args) {
        final int amount = ExtendedArguments.optFluidCount(args, 0);
        final FluidHandler into = tank().getFluidTank(selectedTank());
        if (into == null) return result(null, "no tank");
        final ItemStack stack = inventory().getItem(selectedSlot());
        if (stack == null) return result(null, "nothing selected");
        final ItemFluidHandler handler = FluidUtils.fluidHandlerOf(stack);
        if (handler == null) return result(null, "item is not a fluid container");
        final FluidStack drained = handler.drain(amount, true);
        final long transferred = into.fill(drained, false);
        if (transferred > 0) {
            handler.drain(transferred, false);
            inventory().setItem(selectedSlot(), handler.getContainer());
            return result(true, transferred);
        } else return result(null, "incompatible or no fluid");
    }

    @Callback(doc = "function([amount:number]):boolean -- Transfers fluid from the selected tank to a tank in the selected inventory slot.")
    default Object[] fill(Context context, Arguments args) {
        final int amount = ExtendedArguments.optFluidCount(args, 0);
        final FluidHandler from = tank().getFluidTank(selectedTank());
        if (from == null) return result(null, "no tank");
        final ItemStack stack = inventory().getItem(selectedSlot());
        if (stack == null) return result(null, "nothing selected");
        final ItemFluidHandler handler = FluidUtils.fluidHandlerOf(stack);
        if (handler == null) return result(null, "item is not a fluid container");
        final FluidStack drained = from.drain(amount, true);
        final long transferred = handler.fill(drained, false);
        if (transferred > 0) {
            from.drain(transferred, false);
            inventory().setItem(selectedSlot(), handler.getContainer());
            return result(true, transferred);
        } else return result(null, "incompatible or no fluid");
    }

    private Object[] withFluidInfo(int slot, BiFunction<Optional<FluidStack>, Long, Object[]> f) {
        final ItemStack stack = inventory().getItem(slot);
        if (stack != null) {
            final ItemFluidHandler handler = FluidUtils.fluidHandlerOf(stack);
            if (handler != null && handler.getTanks() > 0) {
                return f.apply(Optional.ofNullable(handler.getFluidInTank(0)), handler.getTankCapacity(0));
            }
        }
        return result(null, "item is not a fluid container");
    }
}
