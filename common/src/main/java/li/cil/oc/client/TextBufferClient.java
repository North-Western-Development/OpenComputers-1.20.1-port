package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import li.cil.oc.common.component.GpuTextBuffer;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.util.BlockPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client side of {@link TextBuffer}: the client proxy (rendering, input
 * forwarding) and the registry of client buffers. Kept out of the common class
 * so a dedicated server never resolves client classes.
 */
public final class TextBufferClient {
    private TextBufferClient() {
    }

    public static TextBuffer.Proxy createProxy(TextBuffer owner) {
        return new ClientProxy(owner);
    }

    public static int charRenderWidth() {
        return TextBufferRenderCache.renderer.charRenderWidth();
    }

    public static int charRenderHeight() {
        return TextBufferRenderCache.renderer.charRenderHeight();
    }

    public static final List<TextBuffer> clientBuffers = new ArrayList<>();

    private static boolean clientHooksRegistered = false;

    /**
     * Replaces the Forge {@code ChunkEvent.Unload} / {@code WorldEvent.Unload} subscriptions:
     * once per client tick, buffers whose level was unloaded or whose chunk is no longer
     * loaded are dropped. Registered lazily on the first client buffer.
     */
    private static synchronized void registerClientHooks() {
        if (clientHooksRegistered) return;
        clientHooksRegistered = true;
        ClientTickEvent.CLIENT_POST.register(minecraft -> pruneClientBuffers(minecraft.level));
    }

    private static void pruneClientBuffers(Level currentLevel) {
        synchronized (clientBuffers) {
            final Iterator<TextBuffer> it = clientBuffers.iterator();
            while (it.hasNext()) {
                final TextBuffer t = it.next();
                final Level world = t.host.world();
                boolean keep = currentLevel != null && world == currentLevel;
                if (keep) {
                    final BlockPosition blockPos = BlockPosition.apply(t.host);
                    keep = world.getChunkSource().hasChunk(blockPos.x >> 4, blockPos.z >> 4);
                }
                if (!keep) {
                    ComponentTracker.INSTANCE.remove(world, t);
                    it.remove();
                }
            }
        }
    }

    public static void registerClientBuffer(TextBuffer t) {
        registerClientHooks();
        PacketSender.sendTextBufferInit(t.proxy.nodeAddress);
        ComponentTracker.INSTANCE.add(t.host.world(), t.proxy.nodeAddress, t);
        synchronized (clientBuffers) {
            clientBuffers.add(t);
        }
    }

    public static class ClientProxy extends TextBuffer.Proxy {
        public final TextBuffer owner;

        public final TextBufferRenderData renderer;

        public ClientProxy(TextBuffer owner) {
            this.owner = owner;
            this.renderer = new TextBufferRenderData() {
                @Override
                public boolean dirty() {
                    return ClientProxy.this.dirty;
                }

                @Override
                public void setDirty(boolean value) {
                    ClientProxy.this.dirty = value;
                }

                @Override
                public li.cil.oc.util.TextBuffer data() {
                    return ClientProxy.this.owner.data;
                }

                @Override
                public Pair<Integer, Integer> viewport() {
                    return ClientProxy.this.owner.viewport;
                }
            };
        }

        @Override
        public TextBuffer owner() {
            return owner;
        }

        @Override
        public boolean render(PoseStack stack) {
            final boolean wasDirty = dirty;
            TextBufferRenderCache.render(stack, renderer);
            return wasDirty;
        }

        @Override
        public void onBufferColorChange() {
            setChanged();
        }

        @Override
        public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
            super.onBufferCopy(col, row, w, h, tx, ty);
            setChanged();
        }

        @Override
        public void onBufferDepthChange(li.cil.oc.api.internal.TextBuffer.ColorDepth depth) {
            setChanged();
        }

        @Override
        public void onBufferFill(int col, int row, int w, int h, char c) {
            super.onBufferFill(col, row, w, h, c);
            setChanged();
        }

        @Override
        public void onBufferPaletteChange(int index) {
            setChanged();
        }

        @Override
        public void onBufferResolutionChange(int w, int h) {
            super.onBufferResolutionChange(w, h);
            setChanged();
        }

        @Override
        public void onBufferViewportResolutionChange(int w, int h) {
            super.onBufferViewportResolutionChange(w, h);
            setChanged();
        }

        @Override
        public void onBufferSet(int col, int row, String s, boolean vertical) {
            super.onBufferSet(col, row, s, vertical);
            setChanged();
        }

        @Override
        public void onBufferBitBlt(int col, int row, int w, int h, GpuTextBuffer ram, int fromCol, int fromRow) {
            super.onBufferBitBlt(col, row, w, h, ram, fromCol, fromRow);
            setChanged();
        }

        @Override
        public void keyDown(char character, int code, Player player) {
            debug("{type = keyDown, char = " + character + ", code = " + code + "}");
            PacketSender.sendKeyDown(nodeAddress, character, code);
        }

        @Override
        public void keyUp(char character, int code, Player player) {
            debug("{type = keyUp, char = " + character + ", code = " + code + "}");
            PacketSender.sendKeyUp(nodeAddress, character, code);
        }

        @Override
        public void textInput(int codePt, Player player) {
            debug("{type = textInput, codePt = " + codePt + "}");
            PacketSender.sendTextInput(nodeAddress, codePt);
        }

        @Override
        public void clipboard(String value, Player player) {
            debug("{type = clipboard}");
            PacketSender.sendClipboard(nodeAddress, value);
        }

        @Override
        public void mouseDown(double x, double y, int button, Player player) {
            debug("{type = mouseDown, x = " + x + ", y = " + y + ", button = " + button + "}");
            PacketSender.sendMouseClick(nodeAddress, x, y, false, button);
        }

        @Override
        public void mouseDrag(double x, double y, int button, Player player) {
            debug("{type = mouseDrag, x = " + x + ", y = " + y + ", button = " + button + "}");
            PacketSender.sendMouseClick(nodeAddress, x, y, true, button);
        }

        @Override
        public void mouseUp(double x, double y, int button, Player player) {
            debug("{type = mouseUp, x = " + x + ", y = " + y + ", button = " + button + "}");
            PacketSender.sendMouseUp(nodeAddress, x, y, button);
        }

        @Override
        public void mouseScroll(double x, double y, int delta, Player player) {
            debug("{type = mouseScroll, x = " + x + ", y = " + y + ", delta = " + delta + "}");
            PacketSender.sendMouseScroll(nodeAddress, x, y, delta);
        }

        @Override
        public void copyToAnalyzer(int line, Player player) {
            PacketSender.sendCopyToAnalyzer(nodeAddress, line);
        }

        private ItemInfo debugger;

        private void debug(String message) {
            if (debugger == null) debugger = Items.get(Constants.ItemName.Debugger);
            final Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null && Items.get(mc.player.getItemInHand(InteractionHand.MAIN_HAND)) == debugger) {
                OpenComputers.log.info("[NETWORK DEBUGGER] Sending packet to node " + nodeAddress + ": " + message);
            }
        }
    }
}
