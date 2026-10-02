package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.util.ExtendedNBT;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public class UpgradeGenerator extends AbstractManagedEnvironment implements DeviceInfo {
    public final Agent host;

    /** Formerly a StackOption; {@link ItemStack#EMPTY} means empty. */
    public ItemStack inventory = ItemStack.EMPTY;

    public int remainingTicks = 0;

    public UpgradeGenerator(Agent host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("generator", Visibility.Neighbors).
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Generator",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Portagen 2.0 (Rev. 3)",
                DeviceAttribute.Capacity, "1"
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function([count:number]):boolean -- Tries to insert fuel from the selected slot into the generator's queue.")
    public Object[] insert(Context context, Arguments args) {
        final int count = args.optInteger(0, 64);
        final ItemStack stack = host.mainInventory().getItem(host.selectedSlot());
        if (stack.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "selected slot is empty");
        if (PlatformHooks.getBurnTime(stack) <= 0) {
            return ResultWrapper.result(ResultWrapper.unit, "selected slot does not contain fuel");
        }
        final ItemStack container = PlatformHooks.getCraftingRemainder(stack);
        final ItemStack inQueue;
        if (!inventory.isEmpty() && inventory.getCount() > 0) {
            if (!ItemStack.isSameItemSameTags(inventory, stack)) {
                return ResultWrapper.result(ResultWrapper.unit, "different fuel type already queued");
            }
            inQueue = inventory;
        }
        else inQueue = ItemStack.EMPTY;
        final int space = inQueue.isEmpty() ? stack.getMaxStackSize() : inQueue.getMaxStackSize() - inQueue.getCount();
        if (space == 0) {
            return ResultWrapper.result(ResultWrapper.unit, "queue is full");
        }
        final ItemStack previousSelectedFuel = stack.copy();
        final int insertLimit = Math.min(stack.getCount(), Math.min(space, count));
        final ItemStack fuelToInsert = stack.split(insertLimit);

        // remove the fuel from the inventory
        if (stack.getCount() == 0) {
            host.mainInventory().setItem(host.selectedSlot(), ItemStack.EMPTY);
        }
        else {
            host.mainInventory().setItem(host.selectedSlot(), stack);
        }

        // add empty containers to inventory
        if (!container.isEmpty()) {
            container.grow(fuelToInsert.getCount() - 1);
            if (!host.player().getInventory().add(container)) {
                // no containers could be placed in inventory, give back the fuel
                host.mainInventory().setItem(host.selectedSlot(), previousSelectedFuel);
                return ResultWrapper.result(false, "no space in inventory for fuel containers");
            }
            else if (container.getCount() > 0) {
                // not all the containers could be inserted in the inventory
                host.player().spawnAtLocation(container.copy(), -0.25f);
            }
        }

        // could be zero
        fuelToInsert.grow(inQueue.getCount());
        inventory = fuelToInsert;

        return ResultWrapper.result(true, insertLimit);
    }

    @Callback(doc = "function():number -- Get the size of the item stack in the generator's queue.")
    public Object[] count(Context context, Arguments args) {
        if (!inventory.isEmpty()) return ResultWrapper.result(inventory.getCount(), inventory.getItem().getName(inventory).getString());
        else return ResultWrapper.result(0);
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to remove items from the generator's queue.")
    public Object[] remove(Context context, Arguments args) {
        final int count = args.optInteger(0, Integer.MAX_VALUE);
        if (count <= 0) {
            return ResultWrapper.result(true); // it is allowed to remove zero
        }
        final ItemStack inQueue = !inventory.isEmpty() && inventory.getCount() > 0 ? inventory : ItemStack.EMPTY;
        if (inQueue.isEmpty()) {
            return ResultWrapper.result(false, "queue is empty");
        }
        final ItemStack previousSelectedItem = host.mainInventory().getItem(host.selectedSlot()).copy();
        final ItemStack requiredContainer = PlatformHooks.getCraftingRemainder(inQueue);
        final ItemStack emptyContainer;
        if (!requiredContainer.isEmpty() && requiredContainer.getCount() > 0) {
            if (!previousSelectedItem.isEmpty() &&
                previousSelectedItem.getItem() == requiredContainer.getItem() &&
                ItemStack.isSameItemSameTags(previousSelectedItem, requiredContainer)) {
                emptyContainer = previousSelectedItem.copy();
            }
            else return ResultWrapper.result(false, "removing this fuel requires the appropriate container in the selected slot");
        }
        else emptyContainer = ItemStack.EMPTY; // nothing to do, nothing required

        final int removeLimit = Math.min(inQueue.getCount(), emptyContainer.isEmpty() ? count : emptyContainer.getCount());

        // backup in case of failure
        final ItemStack previousQueue = inQueue.copy();
        final ItemStack forUser = inQueue.split(removeLimit);
        if (!emptyContainer.isEmpty()) {
            emptyContainer.split(removeLimit);
            if (emptyContainer.isEmpty()) {
                host.mainInventory().setItem(host.selectedSlot(), ItemStack.EMPTY);
            }
            else {
                host.mainInventory().removeItem(host.selectedSlot(), removeLimit);
            }
        }
        // add splits the input stack by reference
        if (!host.player().getInventory().add(forUser)) {
            // returns false if NO items were inserted
            host.mainInventory().setItem(host.selectedSlot(), previousSelectedItem);
            inventory = previousQueue;
            return ResultWrapper.result(false, "no inventory space available for fuel");
        }
        else {
            final int actualRemoval = removeLimit - forUser.getCount();
            previousQueue.shrink(actualRemoval); // reduce it by how much was given to the user
            inventory = previousQueue;
            return ResultWrapper.result(true, actualRemoval);
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (remainingTicks <= 0 && !inventory.isEmpty()) {
            final ItemStack stack = inventory;
            remainingTicks = PlatformHooks.getBurnTime(stack);
            if (remainingTicks > 0) {
                updateClient();
                stack.shrink(1);
                if (stack.getCount() <= 0) {
                    // do not put container in inventory (we left the container when fuel was inserted)
                    inventory = ItemStack.EMPTY;
                }
            }
        }
        if (remainingTicks > 0) {
            remainingTicks -= 1;
            if (remainingTicks == 0 && inventory.isEmpty()) {
                updateClient();
            }
            node().changeBuffer(Settings.get().generatorEfficiency);
        }
    }

    private void updateClient() {
        if (host instanceof Robot robot) {
            robot.synchronizeSlot(robot.componentSlot(node().address()));
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            if (!inventory.isEmpty()) {
                final Level world = host.world();
                final ItemEntity entity = new ItemEntity(world, host.xPosition(), host.yPosition(), host.zPosition(), inventory.copy());
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.04, 0));
                entity.setPickUpDelay(5);
                world.addFreshEntity(entity);
                inventory = ItemStack.EMPTY;
            }
            remainingTicks = 0;
        }
    }

    private static final String InventoryTag = "inventory";
    private static final String RemainingTicksTag = "remainingTicks";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        inventory = ItemStack.of(nbt.getCompound("inventory"));
        if (nbt.contains(InventoryTag)) {
            inventory = ItemStack.of(nbt.getCompound(InventoryTag));
        }
        remainingTicks = nbt.getInt(RemainingTicksTag);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        if (!inventory.isEmpty()) {
            ExtendedNBT.setNewCompoundTag(nbt, InventoryTag, inventory::save);
        }
        if (remainingTicks > 0) {
            nbt.putInt(RemainingTicksTag, remainingTicks);
        }
    }
}
