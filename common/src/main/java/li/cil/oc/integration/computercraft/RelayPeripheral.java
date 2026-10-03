package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.common.tileentity.Relay;
import net.minecraft.core.Direction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Exposes a relay / access point to CC computers as a (wired) modem peripheral. Packets passing
 * through the relay are forwarded to attached CC computers as {@code modem_message} events, and CC
 * computers can send packets into the OC network ({@code transmit}) and call OC components
 * reachable from the relay ({@code callRemote} and friends, like CC's wired modem).
 */
public final class RelayPeripheral implements IDynamicPeripheral {
    /** Packet source addresses used for packets sent by CC computers: {@code cc<id>_<attachmentName>}. */
    private static final Pattern CC_ADDRESS = Pattern.compile("^cc\\d+_");

    @FunctionalInterface
    private interface RelayMethod {
        MethodResult call(IComputerAccess computer, ILuaContext context, Object[] arguments) throws Exception;
    }

    private final Relay relay;
    private final Map<String, RelayMethod> methods = new HashMap<>();
    private final String[] methodNames;

    public RelayPeripheral(Relay relay) {
        this.relay = relay;

        // Generic modem methods.
        methods.put("open", (computer, context, arguments) -> {
            final int port = checkPort(arguments, 0);
            synchronized (relay.openPorts) {
                final Set<Integer> ports = openPorts(computer);
                if (ports.size() >= 128)
                    throw new IllegalArgumentException("too many open channels");
                return MethodResult.of(ports.add(port));
            }
        });
        methods.put("isOpen", (computer, context, arguments) -> {
            final int port = checkPort(arguments, 0);
            synchronized (relay.openPorts) {
                return MethodResult.of(openPorts(computer).contains(port));
            }
        });
        methods.put("close", (computer, context, arguments) -> {
            final int port = checkPort(arguments, 0);
            synchronized (relay.openPorts) {
                return MethodResult.of(openPorts(computer).remove(port));
            }
        });
        methods.put("closeAll", (computer, context, arguments) -> {
            synchronized (relay.openPorts) {
                openPorts(computer).clear();
            }
            return MethodResult.of();
        });
        methods.put("transmit", (computer, context, arguments) -> {
            final int sendPort = checkPort(arguments, 0);
            final int answerPort = checkPort(arguments, 1);
            final Object[] data = new Object[Math.max(0, arguments.length - 2) + 1];
            System.arraycopy(arguments, 2, data, 0, data.length - 1);
            data[data.length - 1] = answerPort;
            final String source = addressOf(computer);
            // Networking happens on the server thread.
            return context.executeMainThreadTask(() -> {
                try {
                    final Packet packet = li.cil.oc.api.Network.newPacket(source, null, sendPort, data);
                    return new Object[]{relay.tryEnqueuePacket(Optional.empty(), packet)};
                } catch (IllegalArgumentException e) {
                    throw new LuaException(e.getMessage());
                }
            });
        });
        methods.put("isWireless", (computer, context, arguments) ->
                // Let's pretend we're always wired, to allow accessing OC components
                // as remote peripherals when using an Access Point, too...
                MethodResult.of(false));

        // Undocumented modem messages (wired modem remote peripheral access).
        methods.put("callRemote", (computer, context, arguments) -> {
            final String address = checkString(arguments, 0);
            final String method = checkString(arguments, 1);
            final Object[] args = Arrays.copyOfRange(arguments, 2, arguments.length);
            // Component callbacks expect to run on the server thread.
            return context.executeMainThreadTask(() -> {
                final Component component = findComponent(address);
                if (component == null) return null;
                try {
                    return component.invoke(method, new CCContext(computer), args);
                } catch (LuaException e) {
                    throw e;
                } catch (Throwable t) {
                    throw new LuaException(t.getMessage() != null ? t.getMessage() : t.toString());
                }
            });
        });
        methods.put("getMethodsRemote", (computer, context, arguments) -> {
            final String address = checkString(arguments, 0);
            final Component component = findComponent(address);
            if (component == null) return MethodResult.of();
            final Map<Integer, String> result = new LinkedHashMap<>();
            int i = 1;
            for (String name : component.methods()) result.put(i++, name);
            return MethodResult.of(result);
        });
        methods.put("getNamesRemote", (computer, context, arguments) -> {
            final Map<Integer, String> result = new LinkedHashMap<>();
            int i = 1;
            for (Component component : visibleComponents()) result.put(i++, component.address());
            return MethodResult.of(result);
        });
        methods.put("getTypeRemote", (computer, context, arguments) -> {
            final String address = checkString(arguments, 0);
            final Component component = findComponent(address);
            return component != null ? MethodResult.of(component.name()) : MethodResult.of();
        });
        methods.put("isPresentRemote", (computer, context, arguments) -> {
            final String address = checkString(arguments, 0);
            return MethodResult.of(findComponent(address) != null);
        });

        // OC specific.
        methods.put("isAccessPoint", (computer, context, arguments) -> MethodResult.of(relay.isWirelessEnabled()));
        methods.put("isTunnel", (computer, context, arguments) -> MethodResult.of(relay.isLinkedEnabled));
        methods.put("maxPacketSize", (computer, context, arguments) -> MethodResult.of(Settings.get().maxNetworkPacketSize));

        methodNames = methods.keySet().stream().sorted().toArray(String[]::new);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public String getType() {
        return "modem";
    }

    @Override
    public void attach(IComputerAccess computer) {
        synchronized (relay.openPorts) {
            if (!relay.computers.contains(computer)) relay.computers.add(computer);
            relay.openPorts.put(computer, new HashSet<>());
        }
    }

    @Override
    public void detach(IComputerAccess computer) {
        synchronized (relay.openPorts) {
            relay.computers.remove(computer);
            relay.openPorts.remove(computer);
        }
    }

    @Override
    public Object getTarget() {
        return relay;
    }

    @Override
    public String[] getMethodNames() {
        return methodNames;
    }

    @Override
    public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments) throws LuaException {
        if (method < 0 || method >= methodNames.length) throw new LuaException("no such method");
        try {
            return methods.get(methodNames[method]).call(computer, context, arguments.getAll());
        } catch (LuaException e) {
            throw e;
        } catch (Throwable t) {
            if (!(t instanceof IllegalArgumentException)) {
                OpenComputers.log.warn("Error in ComputerCraft relay peripheral call.", t);
            }
            throw new LuaException(t.getMessage() != null ? t.getMessage() : t.toString());
        }
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof RelayPeripheral peripheral && peripheral.relay == relay;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RelayPeripheral peripheral && peripheral.relay == relay;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(relay);
    }

    // ----------------------------------------------------------------------- //

    /**
     * Forwards a packet passing through the relay to attached CC computers that have the
     * packet's port open, as {@code modem_message(side, channel, replyChannel, ...)}.
     */
    static void onRelayPacket(Relay relay, Packet packet) {
        final List<IComputerAccess> computers = new ArrayList<>();
        synchronized (relay.openPorts) {
            for (Object computer : relay.computers) {
                if (computer instanceof IComputerAccess access) {
                    final Set<Integer> ports = relay.openPorts.get(access);
                    if (ports != null && ports.contains(packet.port())) computers.add(access);
                }
            }
        }
        if (computers.isEmpty()) return;

        final Object[] data = packet.data();
        final int answerPort;
        final Object[] payload;
        if (CC_ADDRESS.matcher(packet.source()).find() && data.length > 0 && data[data.length - 1] instanceof Number number) {
            // Sent by a CC computer via transmit(): the reply channel is the last value.
            answerPort = number.intValue();
            payload = Arrays.copyOf(data, data.length - 1);
        } else {
            // Sent by an OC machine; there is no reply channel, so reply on the same port.
            answerPort = packet.port();
            payload = data;
        }

        for (IComputerAccess computer : computers) {
            final String address = addressOf(computer);
            if (address.equals(packet.source())) continue;
            if (packet.destination() != null && !packet.destination().equals(address)) continue;
            final Object[] event = new Object[3 + payload.length];
            event[0] = computer.getAttachmentName();
            event[1] = packet.port();
            event[2] = answerPort;
            for (int i = 0; i < payload.length; i++) {
                event[3 + i] = payload[i] instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : payload[i];
            }
            computer.queueEvent("modem_message", event);
        }
    }

    private static String addressOf(IComputerAccess computer) {
        return "cc" + computer.getID() + "_" + computer.getAttachmentName();
    }

    private Set<Integer> openPorts(IComputerAccess computer) {
        return relay.openPorts.computeIfAbsent(computer, k -> new HashSet<>());
    }

    private static int checkPort(Object[] args, int index) {
        if (args.length <= index || !(args[index] instanceof Number number))
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number expected)");
        final int port = number.intValue();
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (number in [0, 65535] expected)");
        return port;
    }

    private static String checkString(Object[] args, int index) {
        if (args.length <= index || !(args[index] instanceof String string))
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (string expected)");
        return string;
    }

    private Component findComponent(String address) {
        for (Component component : visibleComponents()) {
            if (address.equals(component.address())) return component;
        }
        return null;
    }

    private List<Component> visibleComponents() {
        final List<Component> result = new ArrayList<>();
        for (Direction side : Direction.values()) {
            final Node node = relay.sidedNode(side);
            if (node == null || node.network() == null) continue;
            for (Node reachable : node.reachableNodes()) {
                if (reachable instanceof Component component && component.canBeSeenFrom(node) && !result.contains(component)) {
                    result.add(component);
                }
            }
        }
        return result;
    }

    /** Context passed to OC components called by a CC computer. */
    private final class CCContext implements Context {
        private final IComputerAccess computer;

        CCContext(IComputerAccess computer) {
            this.computer = computer;
        }

        @Override
        public Node node() {
            return relay.node();
        }

        @Override
        public boolean canInteract(String player) {
            return true;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public boolean start() {
            return false;
        }

        @Override
        public boolean pause(double seconds) {
            return false;
        }

        @Override
        public boolean stop() {
            return false;
        }

        @Override
        public void consumeCallBudget(double callCost) {
        }

        @Override
        public boolean signal(String name, Object... args) {
            computer.queueEvent(name, args);
            return true;
        }
    }
}
