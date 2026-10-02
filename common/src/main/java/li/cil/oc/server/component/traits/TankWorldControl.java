package li.cil.oc.server.component.traits;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedArguments.TankProperties;
import li.cil.oc.util.FluidUtils;
import net.minecraft.core.Direction;

import java.util.Optional;

import static li.cil.oc.server.component.traits.TankAware.amountOf;
import static li.cil.oc.server.component.traits.TankAware.capacityOf;
import static li.cil.oc.util.ResultWrapper.result;

public interface TankWorldControl extends TankAware, WorldAware, SideRestricted {
    @Callback(doc = "function(side:number [, tank:number]):boolean -- Compare the fluid in the selected tank with the fluid in the specified tank on the specified side. Returns true if equal.")
    default Object[] compareFluid(Context context, Arguments args) {
        final Direction side = checkSideForAction(args, 0);
        final Optional<FluidStack> stack = fluidInTank(selectedTank());
        if (stack.isPresent()) {
            final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(side), side.getOpposite());
            if (handler.isPresent()) {
                final TankProperties properties = ExtendedArguments.optTankProperties(args, handler.get(), 1, null);
                if (properties != null) {
                    return result(stack.get().isFluidEqual(properties.contents));
                } else {
                    final FluidHandler h = handler.get();
                    for (int i = 0; i < h.getTanks(); i++) {
                        if (stack.get().isFluidEqual(h.getFluidInTank(i))) return result(true);
                    }
                    return result(false);
                }
            } else return result(false);
        } else return result(false);
    }

    @Callback(doc = "function(side:boolean[, amount:number=1000]):boolean, number or string -- Drains the specified amount of fluid from the specified side. Returns the amount drained, or an error message.")
    default Object[] drain(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final int count = Math.max(ExtendedArguments.optFluidCount(args, 1), 0);
        final Optional<FluidHandler> selected = getTank(selectedTank());
        if (selected.isEmpty()) return result(null, "no tank selected");
        final FluidHandler tank = selected.get();
        final long space = capacityOf(tank) - amountOf(tank);
        final long amount = Math.min(count, space);
        if (count < 1 || amount > 0) {
            final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
            if (handler.isPresent()) {
                final FluidStack stack = tank.getFluidInTank(0);
                // Note: Forge's tanks return EMPTY rather than null, so the 1.16 code always took
                // the first branch; check for emptiness to restore the intended "drain anything".
                if (stack != null && !stack.isEmpty()) {
                    final FluidStack drained = handler.get().drain(FluidStack.create(stack, amount), false);
                    if ((drained != null && drained.getAmount() > 0) || amount == 0) {
                        final long filled = drained == null ? 0 : tank.fill(drained, false);
                        return result(true, filled);
                    } else return result(null, "incompatible or no fluid");
                } else {
                    final long transferred = tank.fill(handler.get().drain(amount, false), false);
                    return result(transferred > 0, transferred);
                }
            } else return result(null, "incompatible or no fluid");
        } else return result(null, "tank is full");
    }

    @Callback(doc = "function(side:number[, amount:number=1000]):boolean, number of string -- Eject the specified amount of fluid to the specified side. Returns the amount ejected or an error message.")
    default Object[] fill(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final int count = Math.max(ExtendedArguments.optFluidCount(args, 1), 0);
        final Optional<FluidHandler> selected = getTank(selectedTank());
        if (selected.isEmpty()) return result(null, "no tank selected");
        final FluidHandler tank = selected.get();
        final long amount = Math.min(count, amountOf(tank));
        if (count < 1 || amount > 0) {
            final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
            if (handler.isPresent()) {
                final FluidStack stack = tank.getFluidInTank(0);
                if (stack != null && !stack.isEmpty()) {
                    final long filled = handler.get().fill(FluidStack.create(stack, amount), false);
                    if (filled > 0 || amount == 0) {
                        tank.drain(filled, false);
                        return result(true, filled);
                    } else return result(null, "incompatible or no fluid");
                } else return result(null, "tank is empty");
            } else return result(null, "no space");
        } else return result(null, "tank is empty");
    }
}
