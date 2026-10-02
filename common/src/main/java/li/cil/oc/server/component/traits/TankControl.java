package li.cil.oc.server.component.traits;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.util.ExtendedArguments;

import java.util.Optional;

import static li.cil.oc.server.component.traits.TankAware.amountOf;
import static li.cil.oc.server.component.traits.TankAware.capacityOf;
import static li.cil.oc.util.ResultWrapper.result;

public interface TankControl extends TankAware {
    @Callback(doc = "function():number -- The number of tanks installed in the device.")
    default Object[] tankCount(Context context, Arguments args) {
        return result(tank().tankCount());
    }

    @Callback(doc = "function([index:number]):number -- Select a tank and/or get the number of the currently selected tank.")
    default Object[] selectTank(Context context, Arguments args) {
        if (args.count() > 0 && args.checkAny(0) != null) {
            setSelectedTank(ExtendedArguments.checkTank(args, tank(), 0));
        }
        return result(selectedTank() + 1);
    }

    @Callback(direct = true, doc = "function([index:number]):number -- Get the fluid amount in the specified or selected tank.")
    default Object[] tankLevel(Context context, Arguments args) {
        final int index =
                (args.count() > 0 && args.checkAny(0) != null) ? ExtendedArguments.checkTank(args, tank(), 0)
                        : selectedTank();
        return result(fluidInTank(index).map(FluidStack::getAmount).orElse(0L));
    }

    @Callback(direct = true, doc = "function([index:number]):number -- Get the remaining fluid capacity in the specified or selected tank.")
    default Object[] tankSpace(Context context, Arguments args) {
        final int index =
                (args.count() > 0 && args.checkAny(0) != null) ? ExtendedArguments.checkTank(args, tank(), 0)
                        : selectedTank();
        return result(getTank(index).map(tank -> capacityOf(tank) - amountOf(tank)).orElse(0L));
    }

    @Callback(doc = "function(index:number):boolean -- Compares the fluids in the selected and the specified tank. Returns true if equal.")
    default Object[] compareFluidTo(Context context, Arguments args) {
        final int index = ExtendedArguments.checkTank(args, tank(), 0);
        final Optional<FluidStack> stackA = fluidInTank(selectedTank());
        final Optional<FluidStack> stackB = fluidInTank(index);
        if (stackA.isPresent() && stackB.isPresent()) return result(haveSameFluidType(stackA.get(), stackB.get()));
        else return result(stackA.isEmpty() && stackB.isEmpty());
    }

    @Callback(doc = "function(index:number[, count:number=1000]):boolean -- Move the specified amount of fluid from the selected tank into the specified tank.")
    default Object[] transferFluidTo(Context context, Arguments args) {
        final int index = ExtendedArguments.checkTank(args, tank(), 0);
        final int count = ExtendedArguments.optFluidCount(args, 1);
        if (index == selectedTank() || count == 0) {
            return result(true);
        }
        final Optional<FluidHandler> fromTank = getTank(selectedTank());
        final Optional<FluidHandler> toTank = getTank(index);
        if (fromTank.isPresent() && toTank.isPresent()) {
            final FluidHandler from = fromTank.get();
            final FluidHandler to = toTank.get();
            final FluidStack drained = from.drain(count, true);
            final long transferred = to.fill(drained, false);
            if (transferred > 0) {
                from.drain(transferred, false);
                return result(true);
            } else if (count >= amountOf(from) && capacityOf(to) >= amountOf(from) && capacityOf(from) >= amountOf(to)) {
                // Swap.
                final FluidStack tmp = to.drain(amountOf(to), false);
                to.fill(from.drain(amountOf(from), false), false);
                from.fill(tmp, false);
                return result(true);
            } else return result(null, "incompatible or no fluid");
        } else return result(null, "invalid index");
    }
}
