package li.cil.oc.server.component.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.transfer.ItemHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.FluidUtils;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;

import java.util.Optional;
import java.util.function.IntSupplier;

import static li.cil.oc.util.ResultWrapper.result;

public interface InventoryTransfer extends WorldAware, SideRestricted {
    // Return empty on success, else Optional.of("failure reason")
    Optional<String> onTransferContents();

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number[, sourceSlot:number[, sinkSlot:number]]]):boolean -- Transfer some items between two inventories.")
    default Object[] transferItem(Context context, Arguments args) {
        final Direction sourceSide = checkSideForAction(args, 0);
        final BlockPosition sourcePos = position().offset(sourceSide);
        final Direction sinkSide = checkSideForAction(args, 1);
        final BlockPosition sinkPos = position().offset(sinkSide);
        final int count = ExtendedArguments.optItemCount(args, 2);

        final Optional<String> failure = onTransferContents();
        if (failure.isPresent()) {
            return result(null, failure.get());
        }
        final IntSupplier extractor;
        if (args.count() > 3) {
            final ItemHandler source = InventoryUtils.inventoryAt(sourcePos, sourceSide.getOpposite()).orElseThrow(() -> new IllegalArgumentException("no inventory"));
            final int sourceSlot = ExtendedArguments.checkSlot(args, source, 3);
            final ItemHandler sink = InventoryUtils.inventoryAt(sinkPos, sinkSide.getOpposite()).orElseThrow(() -> new IllegalArgumentException("no inventory"));
            final int sinkSlot = ExtendedArguments.optSlot(args, sink, 4, -1);

            extractor = InventoryUtils.getTransferBetweenInventoriesSlotsAt(sourcePos, sourceSide.getOpposite(), sourceSlot, sinkPos, Optional.of(sinkSide.getOpposite()), sinkSlot < 0 ? Optional.empty() : Optional.of(sinkSlot), count);
        } else {
            extractor = InventoryUtils.getTransferBetweenInventoriesAt(sourcePos, sourceSide.getOpposite(), sinkPos, Optional.of(sinkSide.getOpposite()), count);
        }

        if (extractor != null) {
            return result(extractor.getAsInt());
        } else return result(null, "no inventory");
    }

    @Callback(doc = "function(sourceSide:number, sinkSide:number[, count:number [, sourceTank:number]]):boolean, number -- Transfer some fluid between two tanks. Returns operation result and filled amount")
    default Object[] transferFluid(Context context, Arguments args) {
        final Direction sourceSide = checkSideForAction(args, 0);
        final BlockPosition sourcePos = position().offset(sourceSide);
        final Direction sinkSide = checkSideForAction(args, 1);
        final BlockPosition sinkPos = position().offset(sinkSide);
        final int count = ExtendedArguments.optFluidCount(args, 2);
        final int sourceTank = args.optInteger(3, -1);

        final Optional<String> failure = onTransferContents();
        if (failure.isPresent()) {
            return result(null, failure.get());
        }
        final long moved = FluidUtils.transferBetweenFluidHandlersAt(sourcePos, sourceSide.getOpposite(), sinkPos, sinkSide.getOpposite(), count, sourceTank);
        if (moved > 0) context.pause(moved / (double) Math.max(Settings.get().transposerFluidTransferRate, 1)); // Up to transposerFluidTransferRate mB per second.
        return result(moved > 0, moved);
    }
}
