package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.component.traits.TankInventoryControl;
import li.cil.oc.server.component.traits.WorldTankAnalytics;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public final class UpgradeTankController {
    private UpgradeTankController() {
    }

    public interface Common extends DeviceInfo {
        Map<String, String> COMMON_DEVICE_INFO = Map.of(
            DeviceAttribute.Class, DeviceClass.Generic,
            DeviceAttribute.Description, "Tank controller",
            DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceAttribute.Product, "FlowCheckDX"
        );

        @Override
        default Map<String, String> getDeviceInfo() {
            return COMMON_DEVICE_INFO;
        }
    }

    public static class Adapter extends AbstractManagedEnvironment implements WorldTankAnalytics, Common {
        public final EnvironmentHost host;

        public Adapter(EnvironmentHost host) {
            this.host = host;
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("tank_controller", Visibility.Network).
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

    public static class Drone extends AbstractManagedEnvironment implements TankInventoryControl, WorldTankAnalytics, Common {
        public final Agent host;

        public Drone(Agent host) {
            this.host = host;
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("tank_controller", Visibility.Neighbors).
                create());
        }

        @Override
        public Component node() {
            return (Component) super.node();
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Player fakePlayer() {
            return UpgradeInventoryController.fakePlayerAt((ServerLevel) world(), position());
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
        public MultiTank tank() {
            return host.tank();
        }

        @Override
        public int selectedTank() {
            return host.selectedTank();
        }

        @Override
        public void setSelectedTank(int value) {
            host.setSelectedTank(value);
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return ExtendedArguments.checkSideAny(args, n);
        }
    }

    public static class Robot extends AbstractManagedEnvironment implements TankInventoryControl, WorldTankAnalytics, Common {
        public final li.cil.oc.common.tileentity.Robot host;

        public Robot(li.cil.oc.common.tileentity.Robot host) {
            this.host = host;
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("tank_controller", Visibility.Neighbors).
                create());
        }

        @Override
        public Component node() {
            return (Component) super.node();
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Player fakePlayer() {
            return UpgradeInventoryController.fakePlayerAt((ServerLevel) world(), position());
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
        public MultiTank tank() {
            return host.tank();
        }

        @Override
        public int selectedTank() {
            return host.selectedTank();
        }

        @Override
        public void setSelectedTank(int value) {
            host.setSelectedTank(value);
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return host.toGlobal(ExtendedArguments.checkSideForAction(args, n));
        }
    }
}
