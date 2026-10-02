package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static li.cil.oc.util.ResultWrapper.result;

public class Drone extends AbstractManagedEnvironment implements Agent, DeviceInfo {
    public final li.cil.oc.common.entity.Drone agent;

    public final ComponentConnector node;

    public Drone(li.cil.oc.common.entity.Drone agent) {
        this.agent = agent;
        this.node = (ComponentConnector) Network.newNode(this, Visibility.Network).
                withComponent("drone").
                withConnector(Settings.get().bufferDrone).
                create();
        setNode(node);
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    @Override
    public li.cil.oc.common.entity.Drone agent() {
        return agent;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.System);
            info.put(DeviceAttribute.Description, "Drone");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "Overwatcher");
            info.put(DeviceAttribute.Capacity, Integer.toString(agent.inventorySize()));
            deviceInfo = info;
        }
        return deviceInfo;
    }

    @Override
    public Direction checkSideForAction(Arguments args, int n) {
        return ExtendedArguments.checkSideAny(args, n);
    }

    @Override
    public List<ItemEntity> suckableItems(Direction side) {
        final List<ItemEntity> items = new ArrayList<>(entitiesInBlock(ItemEntity.class, position()));
        items.addAll(Agent.super.suckableItems(side));
        return items;
    }

    @Override
    public void onSuckCollect(ItemEntity entity) {
        if (InventoryUtils.insertIntoInventory(entity.getItem(), InventoryUtils.asItemHandler(inventory()), 64, false, Optional.of(insertionSlots()))) {
            final Level world = world();
            world.playSound(agent.player(), agent.getX(), agent.getY(), agent.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2f, ((world.random.nextFloat() - world.random.nextFloat()) * 0.7f + 1) * 2);
        }
    }

    @Override
    public void onWorldInteraction(Context context, double duration) {
        Agent.super.onWorldInteraction(context, duration * 2);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():string -- Get the status text currently being displayed in the GUI.")
    public Object[] getStatusText(Context context, Arguments args) {
        return result(agent.statusText());
    }

    @Callback(doc = "function(value:string):string -- Set the status text to display in the GUI, returns new value.")
    public Object[] setStatusText(Context context, Arguments args) {
        agent.setStatusText(args.checkString(0));
        context.pause(0.1);
        return result(agent.statusText());
    }

    @Callback(doc = "function():number -- Get the current color of the flap lights as an integer encoded RGB value (0xRRGGBB).")
    public Object[] getLightColor(Context context, Arguments args) {
        return result(agent.lightColor());
    }

    @Callback(doc = "function(value:number):number -- Set the color of the flap lights to the specified integer encoded RGB value (0xRRGGBB).")
    public Object[] setLightColor(Context context, Arguments args) {
        agent.setLightColor(args.checkInteger(0));
        context.pause(0.1);
        return result(agent.lightColor());
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(dx:number, dy:number, dz:number) -- Change the target position by the specified offset.")
    public Object[] move(Context context, Arguments args) {
        final float dx = (float) args.checkDouble(0);
        final float dy = (float) args.checkDouble(1);
        final float dz = (float) args.checkDouble(2);
        agent.setTargetX(agent.targetX() + dx);
        agent.setTargetY(agent.targetY() + dy);
        agent.setTargetZ(agent.targetZ() + dz);
        return null;
    }

    @Callback(doc = "function():number -- Get the current distance to the target position.")
    public Object[] getOffset(Context context, Arguments args) {
        return result(agent.position().distanceTo(agent.getTarget()));
    }

    @Callback(doc = "function():number -- Get the current velocity in m/s.")
    public Object[] getVelocity(Context context, Arguments args) {
        return result(agent.getDeltaMovement().length() * 20); // per second
    }

    @Callback(doc = "function():number -- Get the maximum velocity, in m/s.")
    public Object[] getV1elocity(Context context, Arguments args) {
        return result(agent.maxVelocity * 20); // per second
    }

    @Callback(doc = "function():number -- Get the currently set acceleration.")
    public Object[] getAcceleration(Context context, Arguments args) {
        return result(agent.targetAcceleration() * 20); // per second
    }

    @Callback(doc = "function(value:number):number -- Try to set the acceleration to the specified value and return the new acceleration.")
    public Object[] setAcceleration(Context context, Arguments args) {
        agent.setTargetAcceleration((float) (args.checkDouble(0) / 20.0));
        return result(agent.targetAcceleration() * 20);
    }
}
