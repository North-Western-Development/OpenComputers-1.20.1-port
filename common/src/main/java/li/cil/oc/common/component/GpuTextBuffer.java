package li.cil.oc.common.component;

import li.cil.oc.api.internal.TextBuffer.ColorDepth;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.component.traits.TextBufferProxy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class GpuTextBuffer implements TextBufferProxy {
    public final String owner;
    public final int id;
    public final li.cil.oc.util.TextBuffer data;

    public boolean dirty = true;

    public GpuTextBuffer(String owner, int id, li.cil.oc.util.TextBuffer data) {
        this.owner = owner;
        this.id = id;
        this.data = data;
    }

    @Override
    public li.cil.oc.util.TextBuffer data() {
        return data;
    }

    // the gpu ram does not join nor is searchable to the network
    // this field is required because the api TextBuffer is an Environment
    @Override
    public Node node() {
        // Scala threw java.io.InvalidObjectException (checked) here.
        throw new UnsupportedOperationException("GpuTextBuffers do not have nodes");
    }

    @Override
    public int getMaximumWidth() {
        return data.width;
    }

    @Override
    public int getMaximumHeight() {
        return data.height;
    }

    // Note: swapped like on 1.16.5.
    @Override
    public int getViewportWidth() {
        return data.height;
    }

    @Override
    public int getViewportHeight() {
        return data.width;
    }

    @Override
    public void onBufferSet(int col, int row, String s, boolean vertical) {
        dirty = true;
    }

    @Override
    public void onBufferColorChange() {
        dirty = true;
    }

    @Override
    public void onBufferCopy(int col, int row, int w, int h, int tx, int ty) {
        dirty = true;
    }

    @Override
    public void onBufferFill(int col, int row, int w, int h, char c) {
        dirty = true;
    }

    @Override
    public void loadData(CompoundTag nbt) {
        // the data is initially dirty because other devices don't know about it yet
        data.loadData(nbt);
        dirty = true;
    }

    @Override
    public void saveData(CompoundTag nbt) {
        data.saveData(nbt);
        dirty = false;
    }

    @Override
    public void setEnergyCostPerTick(double value) {
    }

    @Override
    public double getEnergyCostPerTick() {
        return 0;
    }

    @Override
    public void setPowerState(boolean value) {
    }

    @Override
    public boolean getPowerState() {
        return false;
    }

    @Override
    public void setMaximumResolution(int width, int height) {
    }

    @Override
    public void setAspectRatio(double width, double height) {
    }

    @Override
    public double getAspectRatio() {
        return 1;
    }

    @Override
    public boolean setResolution(int width, int height) {
        return false;
    }

    @Override
    public boolean setViewport(int width, int height) {
        return false;
    }

    @Override
    public void setMaximumColorDepth(ColorDepth depth) {
    }

    @Override
    public ColorDepth getMaximumColorDepth() {
        return data.format().depth();
    }

    @Override
    public boolean renderText(Object stack) {
        return false;
    }

    @Override
    public int renderWidth() {
        return 0;
    }

    @Override
    public int renderHeight() {
        return 0;
    }

    @Override
    public void setRenderingEnabled(boolean enabled) {
    }

    @Override
    public boolean isRenderingEnabled() {
        return false;
    }

    @Override
    public void keyDown(char character, int code, Player player) {
    }

    @Override
    public void keyUp(char character, int code, Player player) {
    }

    @Override
    public void textInput(int codePt, Player player) {
    }

    @Override
    public void clipboard(String value, Player player) {
    }

    @Override
    public void mouseDown(double x, double y, int button, Player player) {
    }

    @Override
    public void mouseDrag(double x, double y, int button, Player player) {
    }

    @Override
    public void mouseUp(double x, double y, int button, Player player) {
    }

    @Override
    public void mouseScroll(double x, double y, int delta, Player player) {
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }

    // ----------------------------------------------------------------------- //
    // Companion object.

    public static GpuTextBuffer wrap(String owner, int id, li.cil.oc.util.TextBuffer data) {
        return new GpuTextBuffer(owner, id, data);
    }

    public static void bitblt(li.cil.oc.api.internal.TextBuffer dst, int col, int row, int w, int h, li.cil.oc.api.internal.TextBuffer src, int fromCol, int fromRow) {
        final int x = col - 1;
        final int y = row - 1;
        final int fx = fromCol - 1;
        final int fy = fromRow - 1;
        int adjustedDstX = x;
        int adjustedDstY = y;
        int adjustedWidth = w;
        int adjustedHeight = h;
        int adjustedSourceX = fx;
        int adjustedSourceY = fy;

        if (x < 0) {
            adjustedWidth += x;
            adjustedSourceX -= x;
            adjustedDstX = 0;
        }

        if (y < 0) {
            adjustedHeight += y;
            adjustedSourceY -= y;
            adjustedDstY = 0;
        }

        if (adjustedSourceX < 0) {
            adjustedWidth += adjustedSourceX;
            adjustedDstX -= adjustedSourceX;
            adjustedSourceX = 0;
        }

        if (adjustedSourceY < 0) {
            adjustedHeight += adjustedSourceY;
            adjustedDstY -= adjustedSourceY;
            adjustedSourceY = 0;
        }

        adjustedWidth -= Math.max((adjustedDstX + adjustedWidth) - dst.getWidth(), 0);
        adjustedWidth -= Math.max((adjustedSourceX + adjustedWidth) - src.getWidth(), 0);

        adjustedHeight -= Math.max((adjustedDstY + adjustedHeight) - dst.getHeight(), 0);
        adjustedHeight -= Math.max((adjustedSourceY + adjustedHeight) - src.getHeight(), 0);

        // anything left?
        if (adjustedWidth <= 0 || adjustedHeight <= 0) {
            return;
        }

        if (dst instanceof TextBuffer dstScreen) {
            if (src instanceof GpuTextBuffer srcGpu) {
                write_vram_to_screen(dstScreen, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcGpu, adjustedSourceX, adjustedSourceY);
            } else {
                throw new UnsupportedOperationException("Source buffer does not support bitblt operations to a screen");
            }
        } else if (dst instanceof GpuTextBuffer dstGpu) {
            if (src instanceof TextBufferProxy srcProxy) {
                write_to_vram(dstGpu, adjustedDstX, adjustedDstY, adjustedWidth, adjustedHeight, srcProxy, adjustedSourceX, adjustedSourceY);
            } else {
                throw new UnsupportedOperationException("Source buffer does not support bitblt operations");
            }
        } else {
            throw new UnsupportedOperationException("Destination buffer does not support bitblt operations");
        }
    }

    public static boolean write_vram_to_screen(TextBuffer dstScreen, int x, int y, int w, int h, GpuTextBuffer srcRam, int fx, int fy) {
        if (dstScreen.data.rawcopy(x + 1, y + 1, w, h, srcRam.data, fx + 1, fy + 1)) {
            // rawcopy returns true only if data was modified
            dstScreen.addBuffer(srcRam);
            dstScreen.onBufferBitBlt(x + 1, y + 1, w, h, srcRam, fx + 1, fy + 1);
            return true;
        } else return false;
    }

    public static boolean write_to_vram(GpuTextBuffer dstRam, int x, int y, int w, int h, TextBufferProxy src, int fx, int fy) {
        return dstRam.data.rawcopy(x + 1, y + 1, w, h, src.data(), fx + 1, fy + 1);
    }
}
