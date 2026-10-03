package li.cil.oc.server.component.traits;

import dev.architectury.fluid.FluidStack;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.FluidHandler;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedArguments.TankProperties;
import li.cil.oc.util.FluidUtils;
import net.minecraft.core.Direction;

import java.util.Optional;

import static li.cil.oc.util.ResultWrapper.result;

public interface WorldTankAnalytics extends WorldAware, SideRestricted {
    @Callback(doc = "function(side:number [, tank:number]):number -- Get the amount of fluid in the tank on the specified side.")
    default Object[] getTankLevel(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);

        final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (handler.isEmpty()) return result(null, "no tank");
        final TankProperties properties = ExtendedArguments.optTankProperties(args, handler.get(), 1, null);
        if (properties != null) {
            return result(properties.contents == null ? 0L : properties.contents.getAmount());
        } else {
            long sum = 0;
            for (int i = 0; i < handler.get().getTanks(); i++) {
                final FluidStack fluid = handler.get().getFluidInTank(i);
                if (fluid != null) sum += fluid.getAmount();
            }
            return result(sum);
        }
    }

    @Callback(doc = "function(side:number [, tank:number]):number -- Get the capacity of the tank on the specified side.")
    default Object[] getTankCapacity(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (handler.isEmpty()) return result(null, "no tank");
        final TankProperties properties = ExtendedArguments.optTankProperties(args, handler.get(), 1, null);
        if (properties != null) {
            return result(properties.capacity);
        } else {
            long max = 0;
            for (int i = 0; i < handler.get().getTanks(); i++) {
                max = Math.max(max, handler.get().getTankCapacity(i));
            }
            return result(max);
        }
    }

    @Callback(doc = "function(side:number [, tank:number]):table -- Get a description of the fluid in the the tank on the specified side.")
    default Object[] getFluidInTank(Context context, Arguments args) {
        if (Settings.get().allowItemStackInspection) {
            final Direction facing = checkSideForAction(args, 0);
            final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
            if (handler.isEmpty()) return result(null, "no tank");
            final TankProperties properties = ExtendedArguments.optTankProperties(args, handler.get(), 1, null);
            if (properties != null) {
                return result(properties);
            } else {
                final FluidHandler h = handler.get();
                final TankProperties[] all = new TankProperties[h.getTanks()];
                for (int i = 0; i < all.length; i++) {
                    all[i] = new TankProperties(h.getTankCapacity(i), h.getFluidInTank(i));
                }
                return result((Object) all);
            }
        } else return result(null, "not enabled in config");
    }

    @Callback(doc = "function(side:number):number -- Get the number of tanks available on the specified side.")
    default Object[] getTankCount(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final Optional<FluidHandler> handler = FluidUtils.fluidHandlerAt(position().offset(facing), facing.getOpposite());
        if (handler.isPresent() && handler.get().getTanks() > 0) return result(handler.get().getTanks());
        return result(null, "no tank");
    }
}
