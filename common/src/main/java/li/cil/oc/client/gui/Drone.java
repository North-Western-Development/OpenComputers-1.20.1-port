package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.DisplayBuffer;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.client.renderer.TextBufferRenderCache;
import li.cil.oc.client.renderer.font.TextBufferRenderData;
import li.cil.oc.util.PackedColor;
import li.cil.oc.util.RenderState;
import li.cil.oc.util.TextBuffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class Drone extends DynamicGuiContainer<li.cil.oc.common.container.Drone> implements DisplayBuffer {
    protected ImageButton powerButton;

    private final TextBuffer buffer = new TextBuffer(20, 2, new PackedColor.SingleBitFormat(0x33FF33));
    private final TextBufferRenderData bufferRenderer = new TextBufferRenderData() {
        private boolean dirty = true;

        @Override
        public boolean dirty() {
            return dirty;
        }

        @Override
        public void setDirty(boolean value) {
            dirty = value;
        }

        @Override
        public TextBuffer data() {
            return buffer;
        }

        @Override
        public Pair<Integer, Integer> viewport() {
            return buffer.size();
        }
    };

    private double scale = 0.0;

    private static final int inventoryX = 97;
    private static final int inventoryY = 7;

    private final ProgressBar power = addCustomWidget(new ProgressBar(28, 48));

    private static final int selectionSize = 20;
    private static final int selectionsStates = 17;
    private static final float selectionStepV = 1 / (float) selectionsStates;

    public Drone(li.cil.oc.common.container.Drone state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageWidth = 176;
        imageHeight = 148;
    }

    @Override
    public int bufferX() {
        return 9;
    }

    @Override
    public int bufferY() {
        return 9;
    }

    @Override
    public int bufferColumns() {
        return 80;
    }

    @Override
    public int bufferRows() {
        return 16;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled = inventoryContainer.isRunning();
        final List<String> lines = inventoryContainer.statusText().lines().toList();
        boolean dirty = false;
        for (int i = 0; i < lines.size(); i++) {
            if (buffer.set(0, i, lines.get(i), false)) {
                dirty = true;
                break;
            }
        }
        bufferRenderer.setDirty(dirty);
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    protected void init() {
        super.init();
        powerButton = new ImageButton(leftPos + 7, topPos + 45, 18, 18,
                b -> PacketSender.sendDronePower(inventoryContainer, !inventoryContainer.isRunning()),
                Textures.GUI.ButtonPower, true);
        addRenderableWidget(powerButton);
    }

    @Override
    public void drawBuffer(GuiGraphics graphics) {
        graphics.pose().translate(bufferX(), bufferY(), 0);
        RenderState.disableEntityLighting();
        RenderState.makeItBlend();
        graphics.pose().scale((float) scale, (float) scale, 1);
        RenderState.pushAttrib();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(0.5f, 0.5f, 1f, 1f);
        TextBufferRenderCache.render(graphics.pose(), bufferRenderer);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderState.popAttrib();
    }

    @Override
    public double changeSize(double w, double h) {
        return 2.0;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        drawBufferLayer(graphics);
        RenderState.pushAttrib();
        if (isPointInRegion(power.x, power.y, power.width(), power.height(), mouseX - leftPos, mouseY - topPos)) {
            final List<String> tooltip = new ArrayList<>();
            final String format = Localization.Computer.Power() + ": %d%% (%d/%d)";
            tooltip.add(String.format(format,
                    inventoryContainer.globalBuffer() * 100 / Math.max(inventoryContainer.globalBufferSize(), 1),
                    inventoryContainer.globalBuffer(), inventoryContainer.globalBufferSize()));
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        if (powerButton.isMouseOver(mouseX, mouseY)) {
            final String text = inventoryContainer.isRunning() ? Localization.Computer.TurnOff() : Localization.Computer.TurnOn();
            final List<String> tooltip = new ArrayList<>(text.lines().toList());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        RenderState.popAttrib();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Drone, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        power.level = inventoryContainer.globalBuffer() / Math.max((float) inventoryContainer.globalBufferSize(), 1.0f);
        drawWidgets(graphics);
        if (inventoryContainer.otherInventory.getContainerSize() > 0) {
            drawSelection(graphics);
        }

        drawInventorySlots(graphics);
    }

    // No custom slots, we just extend DynamicGuiContainer for the highlighting.
    @Override
    protected void drawSlotBackground(GuiGraphics graphics, int x, int y) {
    }

    private void drawSelection(GuiGraphics graphics) {
        final int slot = inventoryContainer.selectedSlot();
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
