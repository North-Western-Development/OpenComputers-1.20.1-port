package li.cil.oc.server.component;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.DatabaseAccess;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ItemUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class UpgradeDatabase extends AbstractManagedEnvironment implements Database, DeviceInfo {
    public final Container data;

    public UpgradeDatabase(Container data) {
        this.data = data;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("database").
            create());
    }

    @Override
    public Component node() {
        return (Component) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Object catalogue",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "iCatalogue (patent pending)",
                DeviceAttribute.Capacity, String.valueOf(size())
            );
        }
        return deviceInfo;
    }

    @Override
    public int size() {
        return data.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        final ItemStack stack = data.getItem(slot);
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        data.setItem(slot, stack);
    }

    @Override
    public int findStackWithHash(String needle) {
        return indexOf(needle, 0);
    }

    @Callback(doc = "function(slot:number):table -- Get the representation of the item stack stored in the specified slot.")
    public Object[] get(Context context, Arguments args) {
        return ResultWrapper.result(data.getItem(ExtendedArguments.checkSlot(args, data, 0)));
    }

    @Callback(doc = "function(slot:number):string -- Computes a hash value for the item stack in the specified slot.")
    public Object[] computeHash(Context context, Arguments args) {
        final ItemStack stack = data.getItem(ExtendedArguments.checkSlot(args, data, 0));
        if (stack != null) {
            final HashCode hash = Hashing.sha256().hashBytes(ItemUtils.saveStack(stack));
            return ResultWrapper.result(hash.toString());
        }
        else return null;
    }

    @Callback(doc = "function(hash:string):number -- Get the index of an item stack with the specified hash. Returns a negative value if no such stack was found.")
    public Object[] indexOf(Context context, Arguments args) {
        return ResultWrapper.result(indexOf(args.checkString(0), 1));
    }

    @Callback(doc = "function(slot:number):boolean -- Clears the specified slot. Returns true if there was something in the slot before.")
    public Object[] clear(Context context, Arguments args) {
        final int slot = ExtendedArguments.checkSlot(args, data, 0);
        final boolean nonEmpty = data.getItem(slot) != ItemStack.EMPTY; // zero size stacks
        data.setItem(slot, ItemStack.EMPTY);
        return ResultWrapper.result(nonEmpty);
    }

    @Callback(doc = "function(fromSlot:number, toSlot:number[, address:string]):boolean -- Copies an entry to another slot, optionally to another database. Returns true if something was overwritten.")
    public Object[] copy(Context context, Arguments args) {
        final int fromSlot = ExtendedArguments.checkSlot(args, data, 0);
        final ItemStack entry = data.getItem(fromSlot);
        if (args.count() > 2) return DatabaseAccess.withDatabase(node(), args.checkString(2), database -> set(args, database.data, entry));
        else return set(args, data, entry);
    }

    private static Object[] set(Arguments args, Container inventory, ItemStack entry) {
        final int toSlot = ExtendedArguments.checkSlot(args, inventory, 1);
        final boolean nonEmpty = inventory.getItem(toSlot) != ItemStack.EMPTY; // zero size stacks
        inventory.setItem(toSlot, entry.copy());
        return ResultWrapper.result(nonEmpty);
    }

    @Callback(doc = "function(address:string):number -- Copies the data stored in this database to another database with the specified address.")
    public Object[] clone(Context context, Arguments args) {
        return DatabaseAccess.withDatabase(node(), args.checkString(0), database -> {
            final int numberToCopy = Math.min(data.getContainerSize(), database.data.getContainerSize());
            for (int slot = 0; slot < numberToCopy; slot++) {
                database.data.setItem(slot, data.getItem(slot).copy());
            }
            context.pause(0.25);
            return ResultWrapper.result(numberToCopy);
        });
    }

    private int indexOf(String needle, int offset) {
        for (int slot = 0; slot < data.getContainerSize(); slot++) {
            final ItemStack stack = data.getItem(slot);
            if (stack != null) {
                final HashCode hash = Hashing.sha256().hashBytes(ItemUtils.saveStack(stack));
                if (hash.toString().equals(needle)) return slot + offset;
            }
        }
        return -1;
    }
}
