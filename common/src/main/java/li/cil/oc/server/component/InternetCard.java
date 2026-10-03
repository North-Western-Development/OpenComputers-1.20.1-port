package li.cil.oc.server.component;

import java.net.Inet6Address;
import java.net.Inet4Address;
import net.minecraft.server.MinecraftServer;
import li.cil.oc.util.InternetFilteringRule;
import dev.architectury.utils.GameInstance;
import com.google.common.net.InetAddresses;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.util.ThreadPoolFactory;
import org.apache.commons.lang3.tuple.Triple;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static li.cil.oc.util.ResultWrapper.result;

public class InternetCard extends AbstractManagedEnvironment implements DeviceInfo {
    public final Component node;

    protected Optional<Context> owner = Optional.empty();

    protected final Set<Closable> connections = new HashSet<>();

    public InternetCard() {
        this.node = (Component) Network.newNode(this, Visibility.Network).
                withComponent("internet", Visibility.Neighbors).
                create();
        setNode(node);
    }

    @Override
    public Component node() {
        return node;
    }

    // ----------------------------------------------------------------------- //

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceAttribute.Class, DeviceClass.Communication);
            info.put(DeviceAttribute.Description, "Internet modem");
            info.put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceAttribute.Product, "SuperLink X-D4NK");
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():boolean -- Returns whether HTTP requests can be made (config setting).")
    public Object[] isHttpEnabled(Context context, Arguments args) {
        return result(Settings.get().httpEnabled);
    }

    @Callback(doc = "function(url:string[, postData:string[, headers:table[, method:string]]]):userdata -- Starts an HTTP request. If this returns true, further results will be pushed using `http_response` signals.")
    public synchronized Object[] request(Context context, Arguments args) throws Exception {
        checkOwner(context);
        final String address = args.checkString(0);
        if (!Settings.get().internetAccessAllowed()) {
            return result(null, "internet access is unavailable");
        }
        if (!Settings.get().httpEnabled) {
            return result(null, "http requests are unavailable");
        }
        if (connections.size() >= Settings.get().maxConnections) {
            throw new IOException("too many open connections");
        }
        final Optional<String> post = args.isString(1) ? Optional.of(args.checkString(1)) : Optional.empty();
        final Map<String, String> headers = new HashMap<>();
        if (args.isTable(2)) {
            for (Object entry : args.checkTable(2).entrySet()) {
                final Map.Entry<?, ?> e = (Map.Entry<?, ?>) entry;
                if (e.getKey() instanceof String key && e.getValue() != null) {
                    headers.put(key, e.getValue().toString());
                }
            }
        }
        if (!Settings.get().httpHeadersEnabled && !headers.isEmpty()) {
            return result(null, "http request headers are unavailable");
        }
        final Optional<String> method = args.isString(3) ? Optional.of(args.checkString(3)) : Optional.empty();
        final HTTPRequest request = new HTTPRequest(this, checkAddress(address), post, headers, method);
        connections.add(request);
        return result(request);
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether TCP connections can be made (config setting).")
    public Object[] isTcpEnabled(Context context, Arguments args) {
        return result(Settings.get().tcpEnabled);
    }

    @Callback(doc = "function(address:string[, port:number]):userdata -- Opens a new TCP connection. Returns the handle of the connection.")
    public synchronized Object[] connect(Context context, Arguments args) throws Exception {
        checkOwner(context);
        final String address = args.checkString(0);
        final int port = args.optInteger(1, -1);
        if (!Settings.get().internetAccessAllowed()) {
            return result(null, "internet access is unavailable");
        }
        if (!Settings.get().tcpEnabled) {
            return result(null, "tcp connections are unavailable");
        }
        if (connections.size() >= Settings.get().maxConnections) {
            throw new IOException("too many open connections");
        }
        final URI uri = checkUri(address, port);
        final TCPSocket socket = new TCPSocket(this, uri, port);
        connections.add(socket);
        return result(socket);
    }

    private void checkOwner(Context context) {
        if (owner.isEmpty() || context.node() != owner.get().node()) {
            throw new IllegalArgumentException("can only be used by the owning computer");
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (owner.isEmpty() && node.host() instanceof Context context && node.isNeighborOf(this.node)) {
            owner = Optional.of(context);
        }
    }

    @Override
    public synchronized void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (owner.isPresent() && (node == this.node || node.host() instanceof Context context && context == owner.get())) {
            owner = Optional.empty();
            closeAll();
        }
    }

    @Override
    public synchronized void onMessage(Message message) {
        super.onMessage(message);
        if (message.data().length == 0 &&
                ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) &&
                owner.isPresent() && message.source().address().equals(owner.get().node().address())) {
            closeAll();
        }
    }

    private synchronized void closeAll() {
        for (Closable connection : new ArrayList<>(connections)) {
            connection.close();
        }
        connections.clear();
    }

    // ----------------------------------------------------------------------- //

    private URI checkUri(String address, int port) throws Exception {
        try {
            final URI parsed = new URI(address);
            if (parsed.getHost() != null && (parsed.getPort() > 0 || port > 0)) {
                return parsed;
            }
        } catch (Throwable ignored) {
        }

        final URI simple = new URI("oc://" + address);
        if (simple.getHost() != null) {
            if (simple.getPort() > 0)
                return simple;
            else if (port > 0)
                return new URI(simple + ":" + port);
        }

        throw new IllegalArgumentException("address could not be parsed or no valid port given");
    }

    private URL checkAddress(String address) throws FileNotFoundException {
        final URL url;
        try {
            url = new URL(address);
        } catch (Throwable e) {
            throw new FileNotFoundException("invalid address");
        }
        final String protocol = url.getProtocol();
        if (!protocol.matches("^https?$")) {
            throw new FileNotFoundException("unsupported protocol");
        }
        return url;
    }

    // ----------------------------------------------------------------------- //

    private static final ExecutorService threadPool = ThreadPoolFactory.create("Internet", Settings.get().internetThreads);

    public interface Closable {
        void close();
    }

    public static final class TCPNotifier extends Thread {
        public static final TCPNotifier INSTANCE = new TCPNotifier();

        private Selector selector;
        private final ConcurrentLinkedQueue<Pair> toAccept = new ConcurrentLinkedQueue<>();

        private record Pair(SocketChannel channel, Runnable action) {
        }

        private TCPNotifier() {
            super("OpenComputers-TCPNotifier");
            // Not in the original; keeps this endless loop from blocking JVM shutdown.
            setDaemon(true);
            try {
                selector = Selector.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Pair next;
                    while ((next = toAccept.poll()) != null) {
                        next.channel().register(selector, SelectionKey.OP_READ, next.action());
                    }

                    selector.select();

                    final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    final Set<SelectionKey> readableKeys = new HashSet<>();
                    for (SelectionKey key : selectedKeys) {
                        if (key.isReadable()) {
                            ((Runnable) key.attachment()).run();
                            readableKeys.add(key);
                        }
                    }

                    if (!readableKeys.isEmpty()) {
                        final Selector newSelector = Selector.open();
                        // Move all keys that were not handled, not just the selected ones (#3634).
                        for (SelectionKey key : selector.keys()) {
                            if (!readableKeys.contains(key)) {
                                key.channel().register(newSelector, SelectionKey.OP_READ, key.attachment());
                            }
                        }
                        selector.close();
                        selector = newSelector;
                    }
                } catch (IOException e) {
                    OpenComputers.log.error("Error in TCP selector loop.", e);
                }
            }
        }

        public void add(SocketChannel channel, Runnable action) {
            toAccept.offer(new Pair(channel, action));
            selector.wakeup();
        }
    }

    static {
        TCPNotifier.INSTANCE.start();
    }

    public static class TCPSocket extends AbstractValue implements Closable {
        private Optional<InternetCard> owner = Optional.empty();
        private Future<InetAddress> address = null;
        private SocketChannel channel = null;
        private boolean isAddressResolved = false;
        private final UUID id = UUID.randomUUID();

        public TCPSocket() {
        }

        public TCPSocket(InternetCard owner, URI uri, int port) throws IOException {
            this.owner = Optional.of(owner);
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            address = threadPool.submit(new AddressResolver(uri, port));
        }

        private void setupSelector() {
            if (channel == null) return;
            final SocketChannel ch = channel;
            TCPNotifier.INSTANCE.add(ch, () -> {
                if (owner.isPresent()) {
                    owner.get().node.sendToVisible("computer.signal", "internet_ready", id.toString());
                } else {
                    try {
                        ch.close();
                    } catch (IOException ignored) {
                    }
                }
            });
        }

        @Callback(doc = "function():boolean -- Ensures a socket is connected. Errors if the connection failed.")
        public Object[] finishConnect(Context context, Arguments args) throws Exception {
            final Object[] r;
            synchronized (this) {
                r = result(checkConnected());
            }
            setupSelector();
            return r;
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the socket stream. Returns the read byte array.")
        public synchronized Object[] read(Context context, Arguments args) throws Exception {
            final int n = Math.min(Settings.get().maxReadBuffer, Math.max(0, args.optInteger(0, Integer.MAX_VALUE)));
            if (checkConnected()) {
                final ByteBuffer buffer = ByteBuffer.allocate(n);
                final int read = channel.read(buffer);
                if (read == -1) return result((Object) null);
                else {
                    setupSelector();
                    return result((Object) Arrays.copyOf(buffer.array(), read));
                }
            } else return result((Object) new byte[0]);
        }

        @Callback(doc = "function(data:string):number -- Tries to write data to the socket stream. Returns the number of bytes written.")
        public synchronized Object[] write(Context context, Arguments args) throws Exception {
            if (checkConnected()) {
                final byte[] value = args.checkByteArray(0);
                return result(channel.write(ByteBuffer.wrap(value)));
            } else return result(0);
        }

        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public synchronized Object[] close(Context context, Arguments args) {
            close();
            return null;
        }

        @Callback(direct = true, doc = "function():string -- Returns connection ID.")
        public synchronized Object[] id(Context context, Arguments args) {
            return result(id.toString());
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            close();
        }

        @Override
        public void close() {
            if (owner.isPresent()) {
                final InternetCard card = owner.get();
                synchronized (card) {
                    card.connections.remove(this);
                }
                address.cancel(true);
                try {
                    channel.close();
                } catch (IOException ignored) {
                }
                owner = Optional.empty();
                address = null;
                channel = null;
            }
        }

        private boolean checkConnected() throws IOException {
            if (owner.isEmpty()) throw new IOException("connection lost");
            try {
                if (isAddressResolved) return channel.finishConnect();
                else if (address.isCancelled()) {
                    // I don't think this can ever happen, Justin Case.
                    channel.close();
                    throw new IOException("bad connection descriptor");
                } else if (address.isDone()) {
                    // Check for errors.
                    try {
                        address.get();
                    } catch (ExecutionException e) {
                        throw e.getCause();
                    }
                    isAddressResolved = true;
                    return false;
                } else return false;
            } catch (Throwable t) {
                close();
                return false;
            }
        }

        private class AddressResolver implements Callable<InetAddress> {
            private final URI uri;
            private final int port;

            AddressResolver(URI uri, int port) {
                this.uri = uri;
                this.port = port;
            }

            @Override
            public InetAddress call() throws Exception {
                final InetAddress resolved = InetAddress.getByName(uri.getHost());
                checkLists(resolved, uri.getHost());
                final InetSocketAddress address = new InetSocketAddress(resolved, uri.getPort() != -1 ? uri.getPort() : port);
                channel.connect(address);
                return resolved;
            }
        }
    }

    private static boolean isNAT64Address(Inet6Address addr) {
        final byte[] b = addr.getAddress();
        // 64:ff9b::/96 - NAT64 well-known prefix (RFC 6052)
        return b[0] == 0x00 && b[1] == 0x64 && b[2] == (byte) 0xff && b[3] == (byte) 0x9b &&
                b[4] == 0 && b[5] == 0 && b[6] == 0 && b[7] == 0 &&
                b[8] == 0 && b[9] == 0 && b[10] == 0 && b[11] == 0;
    }

    private static InetAddress extractNAT64EmbeddedAddress(Inet6Address addr) throws UnknownHostException {
        final byte[] b = addr.getAddress();
        return InetAddress.getByAddress(new byte[]{b[12], b[13], b[14], b[15]});
    }

    public static boolean isRequestAllowed(Settings settings, InetAddress inetAddress, String host) {
        if (!settings.internetAccessAllowed()) {
            return false;
        }
        final InternetFilteringRule[] rules = settings.internetFilteringRules;
        if (inetAddress instanceof Inet6Address inet6Address) {
            // If the IP address is an IPv6 address with an embedded IPv4 address, and the IPv4 address is blocked,
            // block this request.
            if (InetAddresses.hasEmbeddedIPv4ClientAddress(inet6Address)) {
                final InetAddress inet4in6Address = InetAddresses.getEmbeddedIPv4ClientAddress(inet6Address);
                if (!InternetFilteringRule.firstMatch(rules, inet4in6Address, host).orElse(true)) {
                    return false;
                }
            }

            // As above, but with NAT64 addresses.
            if (isNAT64Address(inet6Address)) {
                try {
                    final InetAddress inet4in6Address = extractNAT64EmbeddedAddress(inet6Address);
                    if (!InternetFilteringRule.firstMatch(rules, inet4in6Address, host).orElse(true)) {
                        return false;
                    }
                } catch (UnknownHostException e) {
                    return false;
                }
            }

            // Process address as an IPv6 address.
            return InternetFilteringRule.firstMatch(rules, inet6Address, host).orElse(false);
        } else if (inetAddress instanceof Inet4Address) {
            // Process address as an IPv4 address.
            return InternetFilteringRule.firstMatch(rules, inetAddress, host).orElse(false);
        } else {
            // Unrecognized address type - block.
            OpenComputers.log.warn("Internet Card blocked unrecognized address type: " + inetAddress);
            return false;
        }
    }

    public static void checkLists(InetAddress inetAddress, String host) throws FileNotFoundException {
        if (!isRequestAllowed(Settings.get(), inetAddress, host)) {
            throw new FileNotFoundException("address is not allowed");
        }
    }

    public static class HTTPRequest extends AbstractValue implements Closable {
        private Optional<InternetCard> owner = Optional.empty();
        private Optional<Triple<Integer, String, Object>> response = Optional.empty();
        private Future<InputStream> stream = null;
        private final ConcurrentLinkedQueue<Byte> queue = new ConcurrentLinkedQueue<>();
        private Future<?> reader = null;
        private volatile boolean eof = false;

        public HTTPRequest() {
        }

        public HTTPRequest(InternetCard owner, URL url, Optional<String> post, Map<String, String> headers, Optional<String> method) {
            this.owner = Optional.of(owner);
            this.stream = threadPool.submit(new RequestSender(url, post, headers, method));
        }

        @Callback(doc = "function():boolean -- Ensures a response is available. Errors if the connection failed.")
        public synchronized Object[] finishConnect(Context context, Arguments args) throws Exception {
            return result(checkResponse());
        }

        @Callback(direct = true, doc = "function():number, string, table -- Get response code, message and headers.")
        public synchronized Object[] response(Context context, Arguments args) {
            if (response.isPresent()) {
                final Triple<Integer, String, Object> r = response.get();
                return result(r.getLeft(), r.getMiddle(), r.getRight());
            } else return result((Object) null);
        }

        @Callback(doc = "function([n:number]):string -- Tries to read data from the response. Returns the read byte array.")
        public synchronized Object[] read(Context context, Arguments args) throws Exception {
            final int n = Math.min(Settings.get().maxReadBuffer, Math.max(0, args.optInteger(0, Integer.MAX_VALUE)));
            if (checkResponse()) {
                if (eof && queue.isEmpty()) return result((Object) null);
                else {
                    final ByteBuffer buffer = ByteBuffer.allocate(n);
                    int read = 0;
                    while (!queue.isEmpty() && read < n) {
                        buffer.put(queue.poll());
                        read += 1;
                    }
                    if (read == 0) {
                        readMore();
                    }
                    return result((Object) Arrays.copyOf(buffer.array(), read));
                }
            } else return result((Object) new byte[0]);
        }

        @Callback(direct = true, doc = "function() -- Closes an open socket stream.")
        public synchronized Object[] close(Context context, Arguments args) {
            close();
            return null;
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            close();
        }

        @Override
        public void close() {
            if (owner.isPresent()) {
                final InternetCard card = owner.get();
                synchronized (card) {
                    card.connections.remove(this);
                }
                stream.cancel(true);
                if (reader != null) {
                    reader.cancel(true);
                }
                owner = Optional.empty();
                stream = null;
                reader = null;
            }
        }

        private synchronized boolean checkResponse() throws Exception {
            if (owner.isEmpty()) throw new IOException("connection lost");
            if (stream.isDone()) {
                if (reader == null) {
                    // Check for errors.
                    try {
                        stream.get();
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof Exception cause) throw cause;
                        throw new IOException(e.getCause());
                    }
                    readMore();
                }
                return true;
            } else return false;
        }

        private void readMore() {
            if (reader == null || reader.isCancelled() || reader.isDone()) {
                if (!eof) {
                    final Future<InputStream> source = stream;
                    reader = threadPool.submit(() -> {
                        try {
                            final byte[] buffer = new byte[Settings.get().maxReadBuffer];
                            final int count = source.get().read(buffer);
                            if (count < 0) {
                                eof = true;
                            }
                            for (int i = 0; i < count; i++) {
                                queue.add(buffer[i]);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }

        // This one doesn't (see comment in TCP socket), but I like to keep it consistent.
        private class RequestSender implements Callable<InputStream> {
            private final URL url;
            private final Optional<String> post;
            private final Map<String, String> headers;
            private final Optional<String> method;

            RequestSender(URL url, Optional<String> post, Map<String, String> headers, Optional<String> method) {
                this.url = url;
                this.post = post;
                this.headers = headers;
                this.method = method;
            }

            @Override
            public InputStream call() throws Exception {
                try {
                    checkLists(InetAddress.getByName(url.getHost()), url.getHost());
                    // Use the server's proxy (as configured for Minecraft itself), if any.
                    final MinecraftServer server = GameInstance.getServer();
                    final java.net.Proxy proxy = server != null && server.getProxy() != null ? server.getProxy() : java.net.Proxy.NO_PROXY;
                    final URLConnection connection = url.openConnection(proxy);
                    if (connection instanceof HttpURLConnection http) {
                        try {
                            http.setDoInput(true);
                            http.setDoOutput(post.isPresent());
                            http.setRequestMethod(method.orElse(post.isPresent() ? "POST" : "GET"));
                            http.setRequestProperty("User-Agent", Settings.get().httpUserAgent.replace("$version", OpenComputers.version()));
                            headers.forEach(http::setRequestProperty);
                            if (post.isPresent()) {
                                http.setReadTimeout(Settings.get().httpTimeout);

                                final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(http.getOutputStream()));
                                out.write(post.get());
                                out.close();
                            }

                            // Finish the connection. Call getInputStream a second time below to re-throw any exception.
                            // This avoids getResponseCode() waiting for the connection to end in the synchronized block,
                            // and makes the response code / message available for unsuccessful requests, too.
                            try {
                                http.getInputStream();
                            } catch (Exception ignored) {
                            }

                            synchronized (HTTPRequest.this) {
                                response = Optional.of(Triple.of(http.getResponseCode(), http.getResponseMessage(), http.getHeaderFields()));
                            }

                            // TODO: This should allow accessing getErrorStream() for reading unsuccessful HTTP responses' output,
                            // but this would be a breaking change for existing OC code.
                            return http.getInputStream();
                        } catch (Throwable t) {
                            http.disconnect();
                            throw t;
                        }
                    } else throw new IOException("unexpected connection type");
                } catch (UnknownHostException e) {
                    throw new IOException("unknown host: " + Optional.ofNullable(e.getMessage()).orElse(e.toString()));
                } catch (Throwable e) {
                    throw new IOException(Optional.ofNullable(e.getMessage()).orElse(e.toString()));
                }
            }
        }
    }
}
