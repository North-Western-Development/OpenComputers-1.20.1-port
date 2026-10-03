package li.cil.oc.integration.appeng;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.externalstorage.GenericStackInv;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.util.ExtendedArguments;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

import static li.cil.oc.util.ResultWrapper.result;

/**
 * Configuration access for AE2 parts (and the interface block).
 * <p>
 * AE2 15 config inventories are generic ({@link GenericStackInv} of {@link GenericStack}s, items or
 * fluids) instead of item handlers named "config". Reading a slot returns an item stack table or a
 * fluid table (amount in mB); writing still takes item stacks from a database upgrade.
 */
public interface PartEnvironmentBase extends ManagedEnvironment {
    IPartHost host();

    // function(side:number[, slot:number]):table
    default Object[] getPartConfig(Context context, Arguments args, Predicate<IPart> filter) {
        final Direction side = ExtendedArguments.checkSideAny(args, 0);
        final IPart part = host().getPart(side);
        if (filter.test(part) && part instanceof IConfigInvHost configHost) {
            return getConfig(configHost.getConfig(), args, 1);
        }
        return result(null, "no matching part");
    }

    // function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean
    default Object[] setPartConfig(Context context, Arguments args, Predicate<IPart> filter) {
        final Direction side = ExtendedArguments.checkSideAny(args, 0);
        final IPart part = host().getPart(side);
        if (filter.test(part) && part instanceof IConfigInvHost configHost) {
            return setConfig(node(), context, configHost.getConfig(), args, 1);
        }
        return result(null, "no matching part");
    }

    // ----------------------------------------------------------------------- //

    static int optConfigSlot(Arguments args, GenericStackInv config, int index) {
        if (index >= args.count() || args.optAny(index, null) == null) return 0;
        final int slot = args.checkInteger(index) - 1;
        if (slot < 0 || slot >= config.size()) throw new IllegalArgumentException("invalid slot");
        return slot;
    }

    /** function([slot:number]):table, arguments starting at {@code offset}. */
    static Object[] getConfig(GenericStackInv config, Arguments args, int offset) {
        final int slot = optConfigSlot(args, config, offset);
        final GenericStack stack = config.getStack(slot);
        return result(stack != null ? AEUtil.toStack(stack.what(), stack.amount()) : null);
    }

    /** function([slot:number][, database:address, entry:number[, size:number]]):boolean, arguments starting at {@code offset}. */
    static Object[] setConfig(Node node, Context context, GenericStackInv config, Arguments args, int offset) {
        final boolean noSlot = args.isString(offset);
        final int slot = noSlot ? 0 : optConfigSlot(args, config, offset);
        GenericStack stack = null;
        if (args.count() > offset + 1) {
            final int base = noSlot ? offset : offset + 1;
            final String address = args.checkString(base);
            final int entry = args.checkInteger(base + 1);
            final int size = args.optInteger(base + 2, 1);
            final Node n = node.network().node(address);
            if (!(n instanceof Component component)) throw new IllegalArgumentException("no such component");
            if (!(component.host() instanceof Database database)) throw new IllegalArgumentException("not a database");
            final ItemStack dbStack = database.getStackInSlot(entry - 1);
            if (dbStack != null && size >= 1 && !dbStack.isEmpty()) {
                // Note: AEItemKey.of is overloaded with Fabric-only types, which common can't see.
                final GenericStack key = GenericStack.fromItemStack(dbStack);
                if (key != null) {
                    final long amount = key.what() instanceof AEItemKey ? Math.min(size, dbStack.getMaxStackSize()) : key.amount();
                    stack = new GenericStack(key.what(), amount);
                }
            }
        }
        config.setStack(slot, stack);
        context.pause(0.5);
        return result(true);
    }
}
