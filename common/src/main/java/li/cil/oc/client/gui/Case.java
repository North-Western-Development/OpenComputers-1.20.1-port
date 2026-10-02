package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class Case extends DynamicGuiContainer<li.cil.oc.common.container.Case> {
    protected ImageButton powerButton;

    public Case(li.cil.oc.common.container.Case state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        powerButton.toggled = inventoryContainer.isRunning();
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    protected void init() {
        super.init();
        powerButton = new ImageButton(leftPos + 70, topPos + 33, 18, 18,
                b -> PacketSender.sendComputerPower(inventoryContainer, !inventoryContainer.isRunning()),
                Textures.GUI.ButtonPower, true);
        addRenderableWidget(powerButton);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        if (powerButton.isMouseOver(mouseX, mouseY)) {
            final List<String> tooltip = new ArrayList<>();
            final String text = inventoryContainer.isRunning() ? Localization.Computer.TurnOff() : Localization.Computer.TurnOn();
            tooltip.addAll(text.lines().toList());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Computer, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
