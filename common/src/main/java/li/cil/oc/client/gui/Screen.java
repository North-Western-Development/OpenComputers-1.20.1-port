package li.cil.oc.client.gui;

import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.gui.traits.InputBuffer;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.gui.BufferRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.function.Supplier;

public class Screen extends net.minecraft.client.gui.screens.Screen implements InputBuffer {
    public final TextBuffer buffer;
    public final boolean hasMouse;
    public final Supplier<Boolean> hasKeyboardCallback;
    public final Supplier<Boolean> hasPower;

    private final InputBuffer.State inputBufferState = new InputBuffer.State();

    private double scale = 0.0;

    private final int bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin;

    private boolean didClick = false;

    private int x = 0, y = 0;

    private int innerWidth = 0, innerHeight = 0;

    private int mx = -1, my = -1;

    public Screen(TextBuffer buffer, boolean hasMouse, Supplier<Boolean> hasKeyboardCallback, Supplier<Boolean> hasPower) {
        super(Component.empty());
        this.buffer = buffer;
        this.hasMouse = hasMouse;
        this.hasKeyboardCallback = hasKeyboardCallback;
        this.hasPower = hasPower;
    }

    // ----------------------------------------------------------------------- //
    // InputBuffer / DisplayBuffer wiring.

    @Override
    public InputBuffer.State inputBufferState() {
        return inputBufferState;
    }

    @Override
    public TextBuffer buffer() {
        return buffer;
    }

    @Override
    public boolean hasKeyboard() {
        return hasKeyboardCallback.get();
    }

    @Override
    public int bufferX() {
        return 8 + x;
    }

    @Override
    public int bufferY() {
        return 8 + y;
    }

    @Override
    public double scale() {
        return scale;
    }

    @Override
    public void setScale(double value) {
        scale = value;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        inputBufferTick();
    }

    @Override
    public void removed() {
        super.removed();
        inputBufferRemoved();
    }

    @Override
    public boolean charTyped(char codePt, int mods) {
        return handleCharTyped(codePt, mods) || super.charTyped(codePt, mods);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        return handleKeyPressed(keyCode, scanCode, mods) || super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int mods) {
        return handleKeyReleased(keyCode, scanCode, mods) || super.keyReleased(keyCode, scanCode, mods);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (hasMouse) {
            final Optional<double[]> coordinates = toBufferCoordinates(mouseX, mouseY);
            if (coordinates.isPresent()) {
                buffer.mouseScroll(coordinates.get()[0], coordinates.get()[1], (int) Math.signum(scroll), null);
                return true;
            }
            // Ignore when out of bounds.
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasMouse) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                clickOrDrag(mouseX, mouseY, button);
                return true;
            }
        }
        return handleMouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (hasMouse) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                clickOrDrag(mouseX, mouseY, button);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (hasMouse) {
            if (didClick) {
                final Optional<double[]> coordinates = toBufferCoordinates(mouseX, mouseY);
                if (coordinates.isPresent()) buffer.mouseUp(coordinates.get()[0], coordinates.get()[1], button, null);
                else buffer.mouseUp(-1.0, -1.0, button, null);
            }
            final boolean hasClicked = didClick;
            didClick = false;
            mx = -1;
            my = -1;
            if (hasClicked) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void clickOrDrag(double mouseX, double mouseY, int button) {
        final Optional<double[]> coordinates = toBufferCoordinates(mouseX, mouseY);
        if (coordinates.isPresent()) {
            final double bx = coordinates.get()[0];
            final double by = coordinates.get()[1];
            if ((int) bx != mx || (int) (by * 2) != my) {
                if (mx >= 0 && my >= 0) buffer.mouseDrag(bx, by, button, null);
                else buffer.mouseDown(bx, by, button, null);
                didClick = true;
                mx = (int) bx;
                my = (int) (by * 2); // for high precision mode, sends some unnecessary packets when not using it, but eh
            }
        }
    }

    private Optional<double[]> toBufferCoordinates(double mouseX, double mouseY) {
        final double bx = (mouseX - x - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderWidth();
        final double by = (mouseY - y - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderHeight();
        final int bw = buffer.getViewportWidth();
        final int bh = buffer.getViewportHeight();
        if (bx >= 0 && by >= 0 && bx < bw && by < bh) return Optional.of(new double[]{bx, by});
        return Optional.empty();
    }

    @Override
    protected void init() {
        super.init();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        super.render(graphics, mouseX, mouseY, dt);
        drawBufferLayer(graphics);
    }

    @Override
    public void drawBuffer(GuiGraphics graphics) {
        graphics.pose().translate(x, y, 0);
        BufferRenderer.drawBackground(graphics.pose(), innerWidth, innerHeight);
        if (hasPower.get()) {
            graphics.pose().translate(bufferMargin, bufferMargin, 0);
            graphics.pose().scale((float) scale, (float) scale, 1);
            BufferRenderer.drawText(graphics.pose(), buffer);
        }
    }

    @Override
    public double changeSize(double w, double h) {
        final int bw = buffer.renderWidth();
        final int bh = buffer.renderHeight();
        final double scaleX = Math.min(width / (bw + bufferMargin * 2.0), 1);
        final double scaleY = Math.min(height / (bh + bufferMargin * 2.0), 1);
        final double scale = Math.min(scaleX, scaleY);
        innerWidth = (int) (bw * scale);
        innerHeight = (int) (bh * scale);
        x = (width - (innerWidth + bufferMargin * 2)) / 2;
        y = (height - (innerHeight + bufferMargin * 2)) / 2;
        return scale;
    }
}
