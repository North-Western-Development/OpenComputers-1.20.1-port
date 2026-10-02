package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.platform.ComponentPlatform;
import li.cil.oc.server.component.traits.InventoryAnalytics;
import li.cil.oc.server.component.traits.InventoryWorldControlMk2;
import li.cil.oc.server.component.traits.ItemInventoryControl;
import li.cil.oc.server.component.traits.WorldInventoryAnalytics;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public final class UpgradeInventoryController {
    private UpgradeInventoryController() {
    }

    public interface Common extends DeviceInfo {
        Map<String, String> COMMON_DEVICE_INFO = Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Inventory controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceAttribute.Product, "Item Cataloguer R1"
        );

        @Override
        default Map<String, String> getDeviceInfo() {
            return COMMON_DEVICE_INFO;
        }
    }

    // Same as WorldAware.fakePlayer; spelled out because the classes below inherit
    // fakePlayer() both abstractly (InventoryAware) and as a default (WorldAware).
    static Player fakePlayerAt(ServerLevel world, BlockPosition position) {
        final ServerPlayer player = ComponentPlatform.fakePlayer(world, Settings.get().fakePlayerProfile);
        player.setPos(position.x + 0.5, position.y + 0.5, position.z + 0.5);
        return player;
    }

    public static class Adapter extends AbstractManagedEnvironment implements WorldInventoryAnalytics, Common {
        public final EnvironmentHost host;

        public Adapter(EnvironmentHost host) {
            this.host = host;
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("inventory_controller", Visibility.Network).
                create());
        }

        @Override
        public Component node() {
            return (Component) super.node();
        }

        // ----------------------------------------------------------------------- //

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return ExtendedArguments.checkSideAny(args, n);
        }
    }

    public static class Drone extends AbstractManagedEnvironment implements InventoryAnalytics, InventoryWorldControlMk2, WorldInventoryAnalytics, ItemInventoryControl, Common {
        public final Agent host;

        public Drone(Agent host) {
            this.host = host;
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("inventory_controller", Visibility.Neighbors).
                create());
        }

        @Override
        public Component node() {
            return (Component) super.node();
        }

        // ----------------------------------------------------------------------- //

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Player fakePlayer() {
            return fakePlayerAt((ServerLevel) world(), position());
        }

        @Override
        public Container inventory() {
            return host.mainInventory();
        }

        @Override
        public int selectedSlot() {
            return host.selectedSlot();
        }

        @Override
        public void setSelectedSlot(int value) {
            host.setSelectedSlot(value);
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return ExtendedArguments.checkSideAny(args, n);
        }
    }

    public static class Robot extends AbstractManagedEnvironment implements InventoryAnalytics, InventoryWorldControlMk2, WorldInventoryAnalytics, ItemInventoryControl, Common {
        public final li.cil.oc.common.tileentity.Robot host;

        public Robot(li.cil.oc.common.tileentity.Robot host) {
            this.host = host;
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("inventory_controller", Visibility.Neighbors).
                create());
        }

        @Override
        public Component node() {
            return (Component) super.node();
        }

        // ----------------------------------------------------------------------- //

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Player fakePlayer() {
            return fakePlayerAt((ServerLevel) world(), position());
        }

        @Override
        public Container inventory() {
            return host.mainInventory();
        }

        @Override
        public int selectedSlot() {
            return host.selectedSlot();
        }

        @Override
        public void setSelectedSlot(int value) {
            host.setSelectedSlot(value);
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return host.toGlobal(ExtendedArguments.checkSideForAction(args, n));
        }

        @Callback(doc = "function():boolean -- Swaps the equipped tool with the content of the currently selected inventory slot.")
        public Object[] equip(Context context, Arguments args) {
            if (inventory().getContainerSize() > 0) {
                final ItemStack equipped = host.getItem(0);
                final ItemStack selected = inventory().getItem(selectedSlot());
                host.setItem(0, selected);
                inventory().setItem(selectedSlot(), equipped);
                return ResultWrapper.result(true);
            }
            else return ResultWrapper.result(false);
        }
    }
}
