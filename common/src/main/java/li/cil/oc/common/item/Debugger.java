package li.cil.oc.common.item;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.item.traits.SimpleItem;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Debugger extends SimpleItem {
    public Debugger(Properties props) {
        super(props);
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, BlockPosition position, Direction side, float hitX, float hitY, float hitZ) {
        final Level world = position.world.get();
        if (PlatformHooks.isFakePlayer(player)) return false; // Nope
        if (player instanceof ServerPlayer) {
            final BlockEntity blockEntity = ExtendedWorld.getBlockEntity(world, position);
            if (blockEntity instanceof SidedEnvironment host) {
                if (!world.isClientSide) {
                    reconnect(new Node[]{host.sidedNode(side)});
                }
                return true;
            } else if (blockEntity instanceof Environment host) {
                if (!world.isClientSide) {
                    reconnect(new Node[]{host.node()});
                }
                return true;
            } else {
                if (!world.isClientSide) {
                    node().remove();
                }
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------- //
    // Formerly the companion `object Debugger extends Environment`.

    public static Node node() {
        return NetworkDebugger.INSTANCE.node;
    }

    public static void reconnect(Node[] nodes) {
        NetworkDebugger.INSTANCE.reconnect(nodes);
    }

    public static final class NetworkDebugger implements Environment {
        public static final NetworkDebugger INSTANCE = new NetworkDebugger();

        public Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network).create();

        private NetworkDebugger() {
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(Node node) {
            OpenComputers.log.info("[NETWORK DEBUGGER] New node in network: " + nodeInfo(node));
        }

        @Override
        public void onDisconnect(Node node) {
            OpenComputers.log.info("[NETWORK DEBUGGER] Node removed from network: " + nodeInfo(node));
        }

        @Override
        public void onMessage(Message message) {
            OpenComputers.log.info("[NETWORK DEBUGGER] Received message: " + messageInfo(message) + ".");
        }

        public void reconnect(Node[] nodes) {
            node.remove();
            li.cil.oc.api.Network.joinNewNetwork(node);
            for (Node other : nodes) {
                if (other != null) this.node.connect(other);
            }
        }

        private static String nodeInfo(Node node) {
            final String extra;
            if (node instanceof ComponentConnector componentConnector) {
                extra = componentInfo(componentConnector) + connectorInfo(componentConnector);
            } else if (node instanceof Component component) {
                extra = componentInfo(component);
            } else if (node instanceof Connector connector) {
                extra = connectorInfo(connector);
            } else {
                extra = "()"; // Scala appended Unit's toString here.
            }
            return "{address = " + node.address() + ", reachability = " + node.reachability().name() + extra + "}";
        }

        private static String componentInfo(Component component) {
            return ", type = component, name = " + component.name() + ", visibility = " + component.visibility().name();
        }

        private static String connectorInfo(Connector connector) {
            return ", type = connector, buffer = " + connector.localBuffer() + ", bufferSize = " + connector.localBufferSize();
        }

        private static String messageInfo(Message message) {
            return "{name = " + message.name() + ", source = " + nodeInfo(message.source()) + ", data = [" +
                Arrays.stream(message.data()).map(String::valueOf).collect(Collectors.joining(", ")) + "]}";
        }
    }
}
