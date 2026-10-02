package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.client.ComponentTracker;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.InputBuffer;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.gui.BufferRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Robot extends DynamicGuiContainer<li.cil.oc.common.container.Robot> implements InputBuffer {
    private final InputBuffer.State inputBufferState = new InputBuffer.State();

    private final TextBuffer buffer;

    private final boolean hasKeyboard;

    private static final int withScreenHeight = 256;
    private static final int noScreenHeight = 108;

    private final int deltaY;

    protected ImageButton powerButton;

    protected ImageButton scrollButton;

    // Scroll offset for robot inventory.
    private int inventoryOffset = 0;
    public boolean isScrolling = false;

    private double scale = 0.0;

    private static final int slotSize = 18;

    private static final double maxBufferWidth = 240.0;
    private static final double maxBufferHeight = 140.0;

    private static final int inventoryX = 169;
    private final int inventoryY;

    private static final int scrollX = inventoryX + slotSize * 4 + 2;
    private final int scrollY;
    private static final int scrollWidth = 8;
    private static final int scrollHeight = 92;

    private final ProgressBar power;

    private static final int selectionSize = 20;
    private static final int selectionsStates = 17;
    private static final float selectionStepV = 1 / (float) selectionsStates;

    public Robot(li.cil.oc.common.container.Robot state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);

        TextBuffer screenBuffer = null;
        if (inventoryContainer.info.screenBuffer.isPresent()) {
            final Optional<ManagedEnvironment> env = ComponentTracker.INSTANCE.get(Minecraft.getInstance().level, inventoryContainer.info.screenBuffer.get());
            if (env.isPresent() && env.get() instanceof TextBuffer textBuffer) {
                screenBuffer = textBuffer;
            }
        }
        buffer = screenBuffer;
        hasKeyboard = inventoryContainer.info.hasKeyboard;

        deltaY = buffer != null ? 0 : withScreenHeight - noScreenHeight;

        imageWidth = 256;
        imageHeight = 256 - deltaY;

        inventoryY = 155 - deltaY;
        scrollY = inventoryY;

        power = addCustomWidget(new ProgressBar(26, 156 - deltaY));
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
        return hasKeyboard;
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
    public void containerTick() {
        super.containerTick();
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

    private boolean canScroll() {
        return inventoryContainer.info.mainInvSize > 16;
    }

    private int maxOffset() {
        return inventoryContainer.info.mainInvSize / 4 - 4;
    }

    private double bufferRenderWidth() {
        return Math.min(maxBufferWidth, TextBufferRenderCache.renderer.charRenderWidth() * Settings.screenResolutionsByTier[0].getLeft());
    }

    private double bufferRenderHeight() {
        return Math.min(maxBufferHeight, TextBufferRenderCache.renderer.charRenderHeight() * Settings.screenResolutionsByTier[0].getRight());
    }

    @Override
    public int bufferX() {
        return (int) (8 + (maxBufferWidth - bufferRenderWidth()) / 2);
    }

    @Override
    public int bufferY() {
        return (int) (8 + (maxBufferHeight - bufferRenderHeight()) / 2);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled = inventoryContainer.isRunning();
        scrollButton.active = canScroll();
        scrollButton.hoverOverride = isScrolling;
        if (inventoryContainer.info.mainInvSize < 16 + inventoryOffset * 4) {
            if (inventoryOffset != 0) scrollTo(0);
        }
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    protected void init() {
        super.init();
        powerButton = new ImageButton(leftPos + 5, topPos + 153 - deltaY, 18, 18,
                b -> PacketSender.sendRobotPower(inventoryContainer, !inventoryContainer.isRunning()),
                Textures.GUI.ButtonPower, true);
        scrollButton = new ImageButton(leftPos + scrollX + 1, topPos + scrollY + 1, 6, 13, b -> {
        }, Textures.GUI.ButtonScroll);
        addRenderableWidget(powerButton);
        addRenderableWidget(scrollButton);
    }

    @Override
    public void drawBuffer(GuiGraphics graphics) {
        if (buffer != null) {
            graphics.pose().translate(bufferX(), bufferY(), 0);
            graphics.pose().pushPose();
            graphics.pose().translate(-3, -3, 0);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferRenderer.drawBackground(graphics.pose(), (int) bufferRenderWidth(), (int) bufferRenderHeight(), true);
            graphics.pose().popPose();
            final double scaleX = bufferRenderWidth() / buffer.renderWidth();
            final double scaleY = bufferRenderHeight() / buffer.renderHeight();
            final float scale = (float) Math.min(scaleX, scaleY);
            if (scaleX > scale) {
                graphics.pose().translate(buffer.renderWidth() * (scaleX - scale) / 2, 0, 0);
            } else if (scaleY > scale) {
                graphics.pose().translate(0, buffer.renderHeight() * (scaleY - scale) / 2, 0);
            }
            graphics.pose().scale(scale, scale, scale);
            graphics.pose().scale((float) this.scale, (float) this.scale, 1);
            BufferRenderer.drawText(graphics.pose(), buffer);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);

        for (int slot = 0; slot < menu.slots.size(); slot++) {
            drawSlotHighlight(graphics, menu.getSlot(slot));
        }
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        drawBufferLayer(graphics);
        if (isPointInRegion(power.x, power.y, power.width(), power.height(), mouseX - leftPos, mouseY - topPos)) {
            final List<String> tooltip = new ArrayList<>();
            final String format = Localization.Computer.Power() + ": %d%% (%d/%d)";
            tooltip.add(String.format(format,
                    100 * inventoryContainer.globalBuffer() / inventoryContainer.globalBufferSize(),
                    inventoryContainer.globalBuffer(), inventoryContainer.globalBufferSize()));
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        if (powerButton.isMouseOver(mouseX, mouseY)) {
            final String text = inventoryContainer.isRunning() ? Localization.Computer.TurnOff() : Localization.Computer.TurnOn();
            final List<String> tooltip = new ArrayList<>(text.lines().toList());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(buffer != null ? Textures.GUI.Robot : Textures.GUI.RobotNoScreen, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        power.level = inventoryContainer.globalBuffer() / (double) inventoryContainer.globalBufferSize();
        drawWidgets(graphics);
        if (inventoryContainer.info.mainInvSize > 0) {
            drawSelection(graphics);
        }

        drawInventorySlots(graphics);
    }

    // No custom slots, we just extend DynamicGuiContainer for the highlighting.
    @Override
    protected void drawSlotBackground(GuiGraphics graphics, int x, int y) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        final int mx = (int) mouseX;
        final int my = (int) mouseY;
        if (canScroll() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverScrollBar(mx - leftPos, my - topPos)) {
            isScrolling = true;
            scrollMouse(mouseY);
            return true;
        }
        return handleMouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (canScroll() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isScrolling) {
            isScrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isScrolling) {
            scrollMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void scrollMouse(double mouseY) {
        scrollTo((int) Math.round((mouseY - topPos - scrollY + 1 - 6.5) * maxOffset() / (scrollHeight - 13.0)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        final int mx = (int) mouseX - leftPos;
        final int my = (int) mouseY - topPos;
        if (isCoordinateOverInventory(mx, my) || isCoordinateOverScrollBar(mx, my)) {
            if (scroll < 0) scrollDown();
            else scrollUp();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    private boolean isCoordinateOverInventory(int x, int y) {
        return x >= inventoryX && x < inventoryX + slotSize * 4 &&
                y >= inventoryY && y < inventoryY + slotSize * 4;
    }

    private boolean isCoordinateOverScrollBar(int x, int y) {
        return x > scrollX && x < scrollX + scrollWidth &&
                y >= scrollY && y < scrollY + scrollHeight;
    }

    private void scrollUp() {
        scrollTo(inventoryOffset - 1);
    }

    private void scrollDown() {
        scrollTo(inventoryOffset + 1);
    }

    private void scrollTo(int row) {
        inventoryOffset = Math.max(0, Math.min(maxOffset(), row));
        menu.generateSlotsFor(inventoryOffset);
        final int yMin = topPos + scrollY + 1;
        if (maxOffset() > 0) {
            scrollButton.setY(yMin + (scrollHeight - 13) * inventoryOffset / maxOffset());
        } else {
            scrollButton.setY(yMin);
        }
    }

    @Override
    public double changeSize(double w, double h) {
        final double bw = w * TextBufferRenderCache.renderer.charRenderWidth();
        final double bh = h * TextBufferRenderCache.renderer.charRenderHeight();
        final double scaleX = Math.min(bufferRenderWidth() / bw, 1);
        final double scaleY = Math.min(bufferRenderHeight() / bh, 1);
        return Math.min(scaleX, scaleY);
    }

    private void drawSelection(GuiGraphics graphics) {
        final int slot = inventoryContainer.selectedSlot() - inventoryOffset * 4;
        if (slot >= 0 && slot < 16) {
            final float now = System.currentTimeMillis() % 1000 / 1000.0f;
            final float offsetV = (int) (now * selectionsStates) * selectionStepV;
            final int x = leftPos + inventoryX - 1 + (slot % 4) * (selectionSize - 2);
            final int y = topPos + inventoryY - 1 + (slot / 4) * (selectionSize - 2);

            GuiUtil.texturedQuad(graphics, Textures.GUI.RobotSelection, x, y, x + selectionSize, y + selectionSize, 0,
                    0, 1, offsetV, offsetV + selectionStepV);
        }
    }
}
