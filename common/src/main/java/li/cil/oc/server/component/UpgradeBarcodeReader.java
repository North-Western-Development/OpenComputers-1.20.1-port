package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public class UpgradeBarcodeReader extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public UpgradeBarcodeReader(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("barcode_reader").
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
                DeviceAttribute.Description, "Barcode reader upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Readerizer Deluxe"
            );
        }
        return deviceInfo;
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof Machine machine && machine.host() instanceof Tablet) {
            final Object[] data = message.data();
            if (data.length == 8 &&
                data[0] instanceof CompoundTag nbt &&
                data[1] instanceof ItemStack &&
                data[2] instanceof Player player &&
                data[3] instanceof BlockPosition blockPos &&
                data[4] instanceof Direction side &&
                data[5] instanceof Float hitX &&
                data[6] instanceof Float hitY &&
                data[7] instanceof Float hitZ) {
                final BlockEntity tileEntity = ExtendedWorld.getBlockEntity(host.world(), blockPos);
                if (tileEntity instanceof Analyzable analyzable) {
                    processNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), nbt);
                }
                else if (tileEntity instanceof SidedEnvironment sided) {
                    processNodes(new Node[]{sided.sidedNode(side)}, nbt);
                }
                else if (tileEntity instanceof Environment environment) {
                    processNodes(new Node[]{environment.node()}, nbt);
                }
                // else: Ignore
            }
        }
    }

    private void processNodes(Node[] nodes, CompoundTag nbt) {
        if (nodes == null) return;
        final ListTag readerNBT = new ListTag();

        for (Node node : nodes) {
            if (node == null) continue;
            final CompoundTag nodeNBT = new CompoundTag();
            if (node instanceof Component component) {
                nodeNBT.putString("type", component.name());
            }

            final String address = node.address();
            if (address != null && !address.isEmpty()) {
                nodeNBT.putString("address", node.address());
            }

            readerNBT.add(nodeNBT);
        }

        nbt.put("analyzed", readerNBT);
    }
}
