package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.common.container.ComponentSlot;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Assembler extends DynamicGuiContainer<li.cil.oc.common.container.Assembler> {
    public Optional<Triple<Boolean, Component, Component[]>> info = Optional.empty();

    protected ImageButton runButton;

    private final ProgressBar progress = addCustomWidget(new ProgressBar(28, 92));

    public Assembler(li.cil.oc.common.container.Assembler state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageWidth = 176;
        imageHeight = 192;

        for (Slot slot : menu.slots) {
            if (slot instanceof ComponentSlot component) {
                component.changeListener = Optional.of(this::onSlotChanged);
            }
        }
    }

    private void onSlotChanged(Slot slot) {
        runButton.active = canBuild();
        runButton.toggled = !runButton.active;
        info = validate();
    }

    private Optional<Triple<Boolean, Component, Component[]>> validate() {
        return AssemblerTemplates.select(inventoryContainer.getSlot(0).getItem()).map(t -> t.validate(inventoryContainer.otherInventory));
    }

    private boolean canBuild() {
        return !inventoryContainer.isAssembling() && validate().map(Triple::getLeft).orElse(false);
    }

    @Override
    protected void init() {
        super.init();
        runButton = new ImageButton(leftPos + 7, topPos + 89, 18, 18, b -> {
            if (canBuild()) PacketSender.sendRobotAssemblerStart(inventoryContainer);
        }, Textures.GUI.ButtonRun, true);
        addRenderableWidget(runButton);
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
        RenderState.pushAttrib();
        if (!inventoryContainer.isAssembling()) {
            final String message;
            if (!inventoryContainer.getSlot(0).hasItem()) {
                message = Localization.Assembler.InsertTemplate();
            } else if (info.isPresent() && info.get().getMiddle() != null) {
                message = info.get().getMiddle().getString();
            } else if (inventoryContainer.getSlot(0).hasItem()) {
                message = Localization.Assembler.CollectResult();
            } else {
                message = "";
            }
            graphics.drawString(font, message, 30, 94, 0x404040, false);
            if (runButton.isMouseOver(mouseX, mouseY)) {
                final List<String> tooltip = new ArrayList<>();
                tooltip.add(Localization.Assembler.Run());
                info.ifPresent(i -> {
                    final boolean valid = i.getLeft();
                    final Component[] warnings = i.getRight();
                    if (valid && warnings.length > 0) {
                        for (Component warning : warnings) tooltip.add(warning.getString());
                    }
                });
                copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
            }
        } else if (isPointInRegion(progress.x, progress.y, progress.width(), progress.height(), mouseX - leftPos, mouseY - topPos)) {
            final List<String> tooltip = new ArrayList<>();
            final String timeRemaining = formatTime(inventoryContainer.assemblyRemainingTime());
            tooltip.add(Localization.Assembler.Progress(inventoryContainer.assemblyProgress(), timeRemaining));
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
        RenderState.popAttrib();
    }

    private static String formatTime(int seconds) {
        // Assembly times should not / rarely exceed one hour, so this is good enough.
        if (seconds < 60) return String.format("0:%02d", seconds);
        else return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1); // Required under Linux.
        graphics.blit(Textures.GUI.RobotAssembler, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (inventoryContainer.isAssembling()) progress.level = inventoryContainer.assemblyProgress() / 100.0;
        else progress.level = 0;
        drawWidgets(graphics);
        drawInventorySlots(graphics);
    }

    @Override
    protected void drawDisabledSlot(GuiGraphics graphics, ComponentSlot slot) {
    }
}
