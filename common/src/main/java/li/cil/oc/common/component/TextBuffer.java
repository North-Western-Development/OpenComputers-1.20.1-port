package li.cil.oc.common.component;

import com.google.common.base.Strings;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.CompressedPacketBuilder;
import li.cil.oc.common.PacketBuilder;
import li.cil.oc.common.PacketType;
import li.cil.oc.common.SaveHandler;
import li.cil.oc.common.Tier;
import li.cil.oc.common.component.traits.TextBufferProxy;
import li.cil.oc.common.component.traits.VideoRamDevice;
import li.cil.oc.common.component.traits.VideoRamRasterizer;
import li.cil.oc.common.item.data.NodeData;
import li.cil.oc.server.component.Keyboard;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.SideTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static li.cil.oc.util.ResultWrapper.result;

public class TextBuffer extends AbstractManagedEnvironment implements TextBufferProxy, VideoRamRasterizer, DeviceInfo {
    public final EnvironmentHost host;

    private final ComponentConnector node;

    private Pair<Integer, Integer> maxResolution = Settings.screenResolutionsByTier[Tier.One];

    private li.cil.oc.api.internal.TextBuffer.ColorDepth maxDepth = Settings.screenDepthsByTier[Tier.One];

    private Pair<Double, Double> aspectRatio = Pair.of(1.0, 1.0);

    private double powerConsumptionPerTick = Settings.get().screenCost;

    private boolean precisionMode = false;

    // For client side only.
    private boolean isRendering = true;

    private boolean isDisplaying = true;

    private boolean hasPower = true;

    private double relativeLitArea = -1.0;

    private PacketBuilder _pendingCommands = null;

    private static final int syncInterval = 100;

    private int syncCooldown = syncInterval;

    private final Map<String, VideoRamDevice> videoRamDevices = new HashMap<>();

    public double fullyLitCost;

    public final Proxy proxy;

    public final li.cil.oc.util.TextBuffer data;

    public Pair<Integer, Integer> viewport;

    private Map<String, String> deviceInfo = null;

    public TextBuffer(EnvironmentHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network).
                withComponent("screen").
                withConnector().
                create();
        setNode(node);
        this.fullyLitCost = computeFullyLitCost();
        this.proxy = SideTracker.isClient() ? li.cil.oc.client.TextBufferClient.createProxy(this) : new ServerProxy(this);
        this.data = new li.cil.oc.util.TextBuffer(maxResolution, PackedColor.Depth.format(maxDepth));
        this.viewport = data.size();
    }

    @Override
    public ComponentConnector node() {
        return node;
    }

    @Override
    public li.cil.oc.util.TextBuffer data() {
        return data;
    }

    @Override
    public Map<String, VideoRamDevice> videoRamDevices() {
        return videoRamDevices;
    }

    private PacketBuilder pendingCommands() {
        if (_pendingCommands == null) {
            final PacketBuilder pb = new CompressedPacketBuilder(PacketType.TextBufferMulti);
            pb.writeUTF(node.address());
            _pendingCommands = pb;
        }
        return _pendingCommands;
    }

    // This computes the energy cost (per tick) to keep the screen running if
    // every single "pixel" is lit. This cost increases with higher tiers as
    // their maximum resolution (pixel density) increases. For a basic screen
    // this is simply the configured cost.
    public double computeFullyLitCost() {
        final Pair<Integer, Integer> base = Settings.screenResolutionsByTier[0];
        final int w = base.getLeft();
        final int h = base.getRight();
        final int mw = getMaximumWidth();
        final int mh = getMaximumHeight();
        return powerConsumptionPerTick * (mw * mh) / (w * h);
    }

    public void markInitialized() {
        syncCooldown = -1; // Stop polling for init state.
        relativeLitArea = -1; // Recompute lit area, avoid screens blanking out until something changes.
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            final Map<String, String> info = new HashMap<>();
            info.put(DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Display);
            info.put(DeviceInfo.DeviceAttribute.Description, "Text buffer");
            info.put(DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            info.put(DeviceInfo.DeviceAttribute.Product, "Text Screen V0");
            info.put(DeviceInfo.DeviceAttribute.Capacity, String.valueOf(maxResolution.getLeft() * maxResolution.getRight()));
            info.put(DeviceInfo.DeviceAttribute.Width, new String[]{"1", "4", "8"}[maxDepth.ordinal()]);
            deviceInfo = info;
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (isDisplaying && host.world().getGameTime() % Settings.get().tickFrequency == 0) {
            if (relativeLitArea < 0) {
                // The relative lit area is the number of pixels that are not blank
                // versus the number of pixels in the *current* resolution. This is
                // scaled to multi-block screens, since we only compute this for the
                // origin.
                final int w = getViewportWidth();
                final int h = getViewportHeight();
                float acc = 0f;
                for (int y = 0; y < h; y++) {
                    final char[] line = data.buffer[y];
                    final short[] colors = data.color[y];
                    for (int x = 0; x < w; x++) {
                        final char c = line[x];
                        final short color = colors[x];
                        final int bg = PackedColor.unpackBackground(color, data.format());
                        final int fg = PackedColor.unpackForeground(color, data.format());
                        acc += (c == ' ') ? (bg == 0 ? 0 : 1)
                                : (c == 0x2588) ? (fg == 0 ? 0 : 1)
                                : (fg == 0 && bg == 0 ? 0 : 1);
                    }
                }
                relativeLitArea = acc / (double) (w * h);
            }
            if (node != null) {
                final boolean hadPower = hasPower;
                final double neededPower = relativeLitArea * fullyLitCost * Settings.get().tickFrequency;
                hasPower = node.tryChangeBuffer(-neededPower);
                if (hasPower != hadPower) {
                    li.cil.oc.server.PacketSender.sendTextBufferPowerChange(node.address(), isDisplaying && hasPower, host);
                }
            }
        }

        synchronized (this) {
            if (_pendingCommands != null) {
                final double range = Settings.get().maxWirelessRange[Tier.Two];
                _pendingCommands.sendToPlayersNearHost(host, Optional.of(range * range));
            }
            _pendingCommands = null;
        }

        if (SideTracker.isClient() && syncCooldown > 0) {
            syncCooldown -= 1;
            if (syncCooldown == 0) {
                syncCooldown = syncInterval;
                li.cil.oc.client.PacketSender.sendTextBufferInit(proxy.nodeAddress);
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is currently on.")
    public Object[] isOn(Context computer, Arguments args) {
        return result(isDisplaying);
    }

    @Callback(doc = "function():boolean -- Turns the screen on. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOn(Context computer, Arguments args) {
        final boolean oldPowerState = isDisplaying;
        setPowerState(true);
        return result(isDisplaying != oldPowerState, isDisplaying);
    }

    @Callback(doc = "function():boolean -- Turns off the screen. Returns whether the state changed, and whether it is now on.")
    public Object[] turnOff(Context computer, Arguments args) {
        final boolean oldPowerState = isDisplaying;
        setPowerState(false);
        return result(isDisplaying != oldPowerState, isDisplaying);
    }

    @Callback(direct = true, doc = "function():number, number -- The aspect ratio of the screen. For multi-block screens this is the number of blocks, horizontal and vertical.")
    public Object[] getAspectRatio(Context context, Arguments args) {
        synchronized (this) {
            return result(aspectRatio.getLeft(), aspectRatio.getRight());
        }
    }

    @Callback(doc = "function():table -- The list of keyboards attached to the screen.")
    public Object[] getKeyboards(Context context, Arguments args) {
        context.pause(0.25);
        final List<String> addresses = new ArrayList<>();
        if (host instanceof li.cil.oc.common.tileentity.Screen screen) {
            for (li.cil.oc.common.tileentity.Screen s : screen.screens) {
                collectKeyboards(s.node(), addresses);
            }
        } else {
            collectKeyboards(node, addresses);
        }
        return new Object[]{addresses.toArray(new String[0])};
    }

    private static void collectKeyboards(Node node, List<String> addresses) {
        if (node == null) return;
        for (Node neighbor : node.neighbors()) {
            if (neighbor.host() instanceof Keyboard) addresses.add(neighbor.address());
        }
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the screen is in high precision mode (sub-pixel mouse event positions).")
    public Object[] isPrecise(Context computer, Arguments args) {
        return result(precisionMode);
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Set whether to use high precision mode (sub-pixel mouse event positions).")
    public Object[] setPrecise(Context computer, Arguments args) {
        // Available for T3 screens only... easiest way to check for us is to
        // base it off of the maximum color depth.
        if (maxDepth == Settings.screenDepthsByTier[Tier.Three]) {
            final boolean oldValue = precisionMode;
            precisionMode = args.checkBoolean(0);
            return result(oldValue);
        } else return result(null, "unsupported operation");
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void setEnergyCostPerTick(double value) {
        powerConsumptionPerTick = value;
        fullyLitCost = computeFullyLitCost();
    }

    @Override
    public double getEnergyCostPerTick() {
        return powerConsumptionPerTick;
    }

    @Override
    public void setPowerState(boolean value) {
        if (isDisplaying != value) {
            isDisplaying = value;
            if (isDisplaying) {
                final double neededPower = fullyLitCost * Settings.get().tickFrequency;
                hasPower = node.changeBuffer(-neededPower) == 0;
            }
            li.cil.oc.server.PacketSender.sendTextBufferPowerChange(node.address(), isDisplaying && hasPower, host);
        }
    }

    @Override
    public boolean getPowerState() {
        return isDisplaying;
    }

    @Override
    public void setMaximumResolution(int width, int height) {
        if (width < 1) throw new IllegalArgumentException("width must be larger or equal to one");
        if (height < 1) throw new IllegalArgumentException("height must be larger or equal to one");
        maxResolution = Pair.of(width, height);
        fullyLitCost = computeFullyLitCost();
        // Note: (width, width) like on 1.16.5.
        proxy.onBufferMaxResolutionChange(width, width);
    }

    @Override
    public int getMaximumWidth() {
        return maxResolution.getLeft();
    }

    @Override
    public int getMaximumHeight() {
        return maxResolution.getRight();
    }

    @Override
    public void setAspectRatio(double width, double height) {
        synchronized (this) {
            this.aspectRatio = Pair.of(width, height);
        }
    }

    @Override
    public double getAspectRatio() {
        return aspectRatio.getLeft() / aspectRatio.getRight();
    }

    @Override
    public boolean setResolution(int w, int h) {
        final int mw = maxResolution.getLeft();
        final int mh = maxResolution.getRight();
        // Note: h > mw like on 1.16.5.
        if (w < 1 || h < 1 || w > mw || h > mw || h * w > mw * mh)
            throw new IllegalArgumentException("unsupported resolution");
        // Always send to clients, their state might be dirty.
        proxy.onBufferResolutionChange(w, h);
        // Force set viewport to new resolution. This is partially for
        // backwards compatibility, and partially to enforce a valid one.
        final boolean sizeChanged = data.setSize(w, h);
        final boolean viewportChanged = setViewport(w, h);
        if (sizeChanged || viewportChanged) {
            if (!viewportChanged && node != null) {
                node.sendToReachable("computer.signal", "screen_resized", w, h);
            }
            return true;
        } else return false;
    }

    @Override
    public boolean setViewport(int w, int h) {
        final Pair<Integer, Integer> size = data.size();
        final int mw = size.getLeft();
        final int mh = size.getRight();
        if (w < 1 || h < 1 || w > mw || h > mh)
            throw new IllegalArgumentException("unsupported viewport resolution");
        // Always send to clients, their state might be dirty.
        proxy.onBufferViewportResolutionChange(w, h);
        final int cw = viewport.getLeft();
        final int ch = viewport.getRight();
        if (w != cw || h != ch) {
            viewport = Pair.of(w, h);
            if (node != null) {
                node.sendToReachable("computer.signal", "screen_resized", w, h);
            }
            return true;
        } else return false;
    }

    @Override
    public int getViewportWidth() {
        return viewport.getLeft();
    }

    @Override
    public int getViewportHeight() {
        return viewport.getRight();
    }

    @Override
    public void setMaximumColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        maxDepth = depth;
    }

    @Override
    public li.cil.oc.api.internal.TextBuffer.ColorDepth getMaximumColorDepth() {
        return maxDepth;
    }

    @Override
    public boolean setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
        final boolean colorDepthChanged = TextBufferProxy.super.setColorDepth(depth);
        // Always send to clients, their state might be dirty.
        proxy.onBufferDepthChange(depth);
        return colorDepthChanged;
    }

    @Override
    public void onBufferPaletteChange(int index) {
        proxy.onBufferPaletteChange(index);
    }

    @Override
    public void onBufferColorChange() {
        proxy.onBufferColorChange();
    }

    @Override
    public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
        proxy.onBufferCopy(col, row, w, h, tx, ty);
    }

    @Override
    public void onBufferFill(int col, int row, int w, int h, char c) {
        proxy.onBufferFill(col, row, w, h, c);
    }

    @Override
    public void onBufferSet(int col, int row, String s, boolean vertical) {
        proxy.onBufferSet(col, row, s, vertical);
    }

    @Override
    public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
        proxy.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow);
    }

    @Override
    public void onBufferRamInit(GpuTextBuffer ram) {
        proxy.onBufferRamInit(ram);
    }

    @Override
    public void onBufferRamDestroy(GpuTextBuffer ram) {
        proxy.onBufferRamDestroy(ram);
    }

    @Override
    public void rawSetText(int col, int row, char[][] text) {
        TextBufferProxy.super.rawSetText(col, row, text);
        proxy.onBufferRawSetText(col, row, text);
    }

    @Override
    public void rawSetBackground(int col, int row, int[][] color) {
        TextBufferProxy.super.rawSetBackground(col, row, color);
        // Better for bandwidth to send packed shorts here. Would need a special case for handling on client,
        // though, so let's be wasteful for once...
        proxy.onBufferRawSetBackground(col, row, color);
    }

    @Override
    public void rawSetForeground(int col, int row, int[][] color) {
        TextBufferProxy.super.rawSetForeground(col, row, color);
        // Better for bandwidth to send packed shorts here. Would need a special case for handling on client,
        // though, so let's be wasteful for once...
        proxy.onBufferRawSetForeground(col, row, color);
    }

    // Client only.
    @Override
    public boolean renderText(Object poseStack) {
        return relativeLitArea != 0 && proxy.render((PoseStack) poseStack);
    }

    // Client only.
    @Override
    public int renderWidth() {
        return li.cil.oc.client.TextBufferClient.charRenderWidth() * getViewportWidth();
    }

    // Client only.
    @Override
    public int renderHeight() {
        return li.cil.oc.client.TextBufferClient.charRenderHeight() * getViewportHeight();
    }

    @Override
    public void setRenderingEnabled(boolean enabled) {
        isRendering = enabled;
    }

    @Override
    public boolean isRenderingEnabled() {
        return isRendering;
    }

    @Override
    public void keyDown(char character, int code, Player player) {
        proxy.keyDown(character, code, player);
    }

    @Override
    public void keyUp(char character, int code, Player player) {
        proxy.keyUp(character, code, player);
    }

    @Override
    public void textInput(int codePt, Player player) {
        proxy.textInput(codePt, player);
    }

    @Override
    public void clipboard(String value, Player player) {
        proxy.clipboard(value, player);
    }

    @Override
    public void mouseDown(double x, double y, int button, Player player) {
        proxy.mouseDown(x, y, button, player);
    }

    @Override
    public void mouseDrag(double x, double y, int button, Player player) {
        proxy.mouseDrag(x, y, button, player);
    }

    @Override
    public void mouseUp(double x, double y, int button, Player player) {
        proxy.mouseUp(x, y, button, player);
    }

    @Override
    public void mouseScroll(double x, double y, int delta, Player player) {
        proxy.mouseScroll(x, y, delta, player);
    }

    public void copyToAnalyzer(int line, Player player) {
        proxy.copyToAnalyzer(line, player);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            li.cil.oc.server.ComponentTracker.INSTANCE.add(host.world(), node.address(), this);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            li.cil.oc.server.ComponentTracker.INSTANCE.remove(host.world(), this);
        }
    }

    // ----------------------------------------------------------------------- //

    private String bufferPath() {
        return node.address() + "_buffer";
    }

    private static final String IsOnTag = Settings.namespace + "isOn";
    private static final String HasPowerTag = Settings.namespace + "hasPower";
    private static final String MaxWidthTag = Settings.namespace + "maxWidth";
    private static final String MaxHeightTag = Settings.namespace + "maxHeight";
    private static final String PreciseTag = Settings.namespace + "precise";
    private static final String ViewportWidthTag = Settings.namespace + "viewportWidth";
    private static final String ViewportHeightTag = Settings.namespace + "viewportHeight";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        if (SideTracker.isClient()) {
            if (!Strings.isNullOrEmpty(proxy.nodeAddress)) return; // Only load once.
            proxy.nodeAddress = nbt.getCompound(NodeData.NodeTag).getString(NodeData.AddressTag);
            li.cil.oc.client.TextBufferClient.registerClientBuffer(this);
        } else {
            if (nbt.contains(NodeData.BufferTag)) {
                data.loadData(nbt.getCompound(NodeData.BufferTag));
            } else if (!Strings.isNullOrEmpty(node.address())) {
                data.loadData(SaveHandler.loadNBT(nbt, bufferPath()));
            }
        }

        if (nbt.contains(IsOnTag)) {
            isDisplaying = nbt.getBoolean(IsOnTag);
        }
        if (nbt.contains(HasPowerTag)) {
            hasPower = nbt.getBoolean(HasPowerTag);
        }
        if (nbt.contains(MaxWidthTag) && nbt.contains(MaxHeightTag)) {
            final int maxWidth = nbt.getInt(MaxWidthTag);
            final int maxHeight = nbt.getInt(MaxHeightTag);
            maxResolution = Pair.of(maxWidth, maxHeight);
        }
        precisionMode = nbt.getBoolean(PreciseTag);

        if (nbt.contains(ViewportWidthTag)) {
            final int vpw = nbt.getInt(ViewportWidthTag);
            final int vph = nbt.getInt(ViewportHeightTag);
            viewport = Pair.of(Math.max(Math.min(vpw, data.width), 1), Math.max(Math.min(vph, data.height), 1));
        } else {
            viewport = data.size();
        }
    }

    // Null check for Waila (and other mods that may call this client side).
    @Override
    public void saveData(CompoundTag nbt) {
        if (node == null) return;
        super.saveData(nbt);
        // Happy thread synchronization hack! Here's the problem: GPUs allow direct
        // calls for modifying screens to give a more responsive experience. This
        // causes the following problem: when saving, if the screen is saved first,
        // then the executor runs in parallel and changes the screen *before* the
        // server thread begins saving that computer, the saved computer will think
        // it changed the screen, although the saved screen wasn't. To avoid that we
        // wait for all computers the screen is connected to to finish their current
        // execution and pausing them (which will make them resume in the next tick
        // when their update() runs).
        if (node.network() != null) {
            for (Node other : node.network().nodes()) {
                if (other.host() instanceof li.cil.oc.common.tileentity.traits.Computer computer && !computer.machine().isPaused()) {
                    computer.machine().pause(0.1);
                }
            }
        }

        SaveHandler.scheduleSave(host, nbt, bufferPath(), data::saveData);
        nbt.putBoolean(IsOnTag, isDisplaying);
        nbt.putBoolean(HasPowerTag, hasPower);
        nbt.putInt(MaxWidthTag, maxResolution.getLeft());
        nbt.putInt(MaxHeightTag, maxResolution.getRight());
        nbt.putBoolean(PreciseTag, precisionMode);
        nbt.putInt(ViewportWidthTag, viewport.getLeft());
        nbt.putInt(ViewportHeightTag, viewport.getRight());
    }

    // ----------------------------------------------------------------------- //
    // Companion object.

    public abstract static class Proxy {
        public abstract TextBuffer owner();

        public boolean dirty = false;

        public String nodeAddress = "";

        public void setChanged() {
            dirty = true;
        }

        // Client only.
        public boolean render(PoseStack stack) {
            return false;
        }

        public abstract void onBufferColorChange();

        public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
            owner().relativeLitArea = -1;
        }

        public abstract void onBufferDepthChange(li.cil.oc.api.internal.TextBuffer.ColorDepth depth);

        public void onBufferFill(int col, int row, int w, int h, char c) {
            owner().relativeLitArea = -1;
        }

        public abstract void onBufferPaletteChange(int index);

        public void onBufferResolutionChange(int w, int h) {
            owner().relativeLitArea = -1;
        }

        public void onBufferViewportResolutionChange(int w, int h) {
            owner().relativeLitArea = -1;
        }

        public void onBufferMaxResolutionChange(int w, int h) {
        }

        public void onBufferSet(int col, int row, String s, boolean vertical) {
            owner().relativeLitArea = -1;
        }

        public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
            owner().relativeLitArea = -1;
        }

        public void onBufferRamInit(GpuTextBuffer ram) {
            owner().relativeLitArea = -1;
        }

        public void onBufferRamDestroy(GpuTextBuffer ram) {
            owner().relativeLitArea = -1;
        }

        public void onBufferRawSetText(int col, int row, char[][] text) {
            owner().relativeLitArea = -1;
        }

        public void onBufferRawSetBackground(int col, int row, int[][] color) {
            owner().relativeLitArea = -1;
        }

        public void onBufferRawSetForeground(int col, int row, int[][] color) {
            owner().relativeLitArea = -1;
        }

        public abstract void keyDown(char character, int code, Player player);

        public abstract void keyUp(char character, int code, Player player);

        public abstract void textInput(int codePt, Player player);

        public abstract void clipboard(String value, Player player);

        public abstract void mouseDown(double x, double y, int button, Player player);

        public abstract void mouseDrag(double x, double y, int button, Player player);

        public abstract void mouseUp(double x, double y, int button, Player player);

        public abstract void mouseScroll(double x, double y, int delta, Player player);

        public abstract void copyToAnalyzer(int line, Player player);
    }

    public static class ServerProxy extends Proxy {
        public final TextBuffer owner;

        public ServerProxy(TextBuffer owner) {
            this.owner = owner;
        }

        @Override
        public TextBuffer owner() {
            return owner;
        }

        @Override
        public void onBufferColorChange() {
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferColorChange(owner.pendingCommands(), owner.data.foreground(), owner.data.background());
            }
        }

        @Override
        public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
            super.onBufferCopy(col, row, w, h, tx, ty);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferCopy(owner.pendingCommands(), col, row, w, h, tx, ty);
            }
        }

        @Override
        public void onBufferDepthChange(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferDepthChange(owner.pendingCommands(), depth);
            }
        }

        @Override
        public void onBufferFill(int col, int row, int w, int h, char c) {
            super.onBufferFill(col, row, w, h, c);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferFill(owner.pendingCommands(), col, row, w, h, c);
            }
        }

        @Override
        public void onBufferPaletteChange(int index) {
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferPaletteChange(owner.pendingCommands(), index, owner.getPaletteColor(index));
            }
        }

        @Override
        public void onBufferResolutionChange(int w, int h) {
            super.onBufferResolutionChange(w, h);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferResolutionChange(owner.pendingCommands(), w, h);
            }
        }

        @Override
        public void onBufferViewportResolutionChange(int w, int h) {
            super.onBufferViewportResolutionChange(w, h);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferViewportResolutionChange(owner.pendingCommands(), w, h);
            }
        }

        @Override
        public void onBufferMaxResolutionChange(int w, int h) {
            if (owner.node.network() != null) {
                super.onBufferMaxResolutionChange(w, h);
                owner.host.markChanged();
                synchronized (owner) {
                    li.cil.oc.server.PacketSender.appendTextBufferMaxResolutionChange(owner.pendingCommands(), w, h);
                }
            }
        }

        @Override
        public void onBufferSet(int col, int row, String s, boolean vertical) {
            super.onBufferSet(col, row, s, vertical);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferSet(owner.pendingCommands(), col, row, s, vertical);
            }
        }

        @Override
        public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
            super.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferBitBlt(owner.pendingCommands(), col, row, w, h, ram.owner, ram.id, fromCol, fromRow);
            }
        }

        @Override
        public void onBufferRamInit(GpuTextBuffer ram) {
            super.onBufferRamInit(ram);
            owner.host.markChanged();
            final CompoundTag nbt = new CompoundTag();
            ram.saveData(nbt);
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferRamInit(owner.pendingCommands(), ram.owner, ram.id, nbt);
            }
        }

        @Override
        public void onBufferRamDestroy(GpuTextBuffer ram) {
            super.onBufferRamDestroy(ram);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferRamDestroy(owner.pendingCommands(), ram.owner, ram.id);
            }
        }

        @Override
        public void onBufferRawSetText(int col, int row, char[][] text) {
            super.onBufferRawSetText(col, row, text);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferRawSetText(owner.pendingCommands(), col, row, text);
            }
        }

        @Override
        public void onBufferRawSetBackground(int col, int row, int[][] color) {
            super.onBufferRawSetBackground(col, row, color);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferRawSetBackground(owner.pendingCommands(), col, row, color);
            }
        }

        @Override
        public void onBufferRawSetForeground(int col, int row, int[][] color) {
            super.onBufferRawSetForeground(col, row, color);
            owner.host.markChanged();
            synchronized (owner) {
                li.cil.oc.server.PacketSender.appendTextBufferRawSetForeground(owner.pendingCommands(), col, row, color);
            }
        }

        @Override
        public void keyDown(char character, int code, Player player) {
            sendToKeyboards("keyboard.keyDown", player, character, code);
        }

        @Override
        public void keyUp(char character, int code, Player player) {
            sendToKeyboards("keyboard.keyUp", player, character, code);
        }

        @Override
        public void textInput(int codePt, Player player) {
            sendToKeyboards("keyboard.textInput", player, codePt);
        }

        @Override
        public void clipboard(String value, Player player) {
            sendToKeyboards("keyboard.clipboard", player, value);
        }

        @Override
        public void mouseDown(double x, double y, int button, Player player) {
            sendMouseEvent(player, "touch", x, y, button);
        }

        @Override
        public void mouseDrag(double x, double y, int button, Player player) {
            sendMouseEvent(player, "drag", x, y, button);
        }

        @Override
        public void mouseUp(double x, double y, int button, Player player) {
            sendMouseEvent(player, "drop", x, y, button);
        }

        @Override
        public void mouseScroll(double x, double y, int delta, Player player) {
            sendMouseEvent(player, "scroll", x, y, delta);
        }

        @Override
        public void copyToAnalyzer(int line, Player player) {
            final ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!stack.isEmpty()) {
                stack.removeTagKey(Settings.namespace + "clipboard");

                if (line >= 0 && line < owner.getViewportHeight()) {
                    final String text = new String(owner.data.buffer[line]).trim();
                    if (!Strings.isNullOrEmpty(text)) {
                        stack.getOrCreateTag().putString(Settings.namespace + "clipboard", text);
                    }
                }
            }
        }

        private void sendMouseEvent(Player player, String name, double x, double y, int data) {
            final List<Object> args = new ArrayList<>();

            args.add(player);
            args.add(name);
            if (owner.precisionMode) {
                args.add(x);
                args.add(y);
            } else {
                args.add((int) x + 1);
                args.add((int) y + 1);
            }
            args.add(data);
            if (Settings.get().inputUsername) {
                args.add(player.getName().getString());
            }

            owner.node.sendToReachable("computer.checked_signal", args.toArray());
        }

        private void sendToKeyboards(String name, Object... values) {
            if (owner.host instanceof li.cil.oc.common.tileentity.Screen screen) {
                for (li.cil.oc.common.tileentity.Screen s : screen.screens) {
                    s.node().sendToNeighbors(name, values);
                }
            } else {
                owner.node.sendToNeighbors(name, values);
            }
        }
    }
}
