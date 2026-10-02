package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.server.PacketSender;
import li.cil.oc.server.component.traits.InventoryTransfer;
import li.cil.oc.server.component.traits.WorldInventoryAnalytics;
import li.cil.oc.server.component.traits.WorldTankAnalytics;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import net.minecraft.core.Direction;

import java.util.Map;
import java.util.Optional;

public final class Transposer {
    private Transposer() {
    }

    public abstract static class Common extends AbstractManagedEnvironment implements WorldInventoryAnalytics, WorldTankAnalytics, InventoryTransfer, DeviceInfo {
        protected Common() {
            setNode(Network.newNode(this, Visibility.Network).
                withComponent("transposer").
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
                    DeviceAttribute.Class, DeviceClass.Generic,
                    DeviceAttribute.Description, "Transposer",
                    DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                    DeviceAttribute.Product, "TP4k-iX"
                );
            }
            return deviceInfo;
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return ExtendedArguments.checkSideAny(args, n);
        }

        @Override
        public Optional<String> onTransferContents() {
            if (node().tryChangeBuffer(-Settings.get().transposerCost)) return Optional.empty();
            else return Optional.of("not enough energy");
        }
    }

    public static class Block extends Common {
        public final li.cil.oc.common.tileentity.Transposer host;

        public Block(li.cil.oc.common.tileentity.Transposer host) {
            this.host = host;
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Optional<String> onTransferContents() {
            final Optional<String> result = super.onTransferContents();
            if (result.isEmpty()) PacketSender.sendTransposerActivity(host);
            return result;
        }
    }

    public static class Upgrade extends Common {
        public final EnvironmentHost host;

        public Upgrade(EnvironmentHost host) {
            this.host = host;
            node().setVisibility(Visibility.Neighbors);
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }
    }
}
