package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.traits.LockedHotbar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Server extends DynamicGuiContainer<li.cil.oc.common.container.Server> implements LockedHotbar {
    protected ImageButton powerButton;

    public Server(li.cil.oc.common.container.Server state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public ItemStack lockedStack() {
        return inventoryContainer.stack;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (isSlotClickAllowed(slot)) {
            super.slotClicked(slot, slotId, mouseButton, clickType);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        powerButton.visible = !inventoryContainer.isItem;
        powerButton.toggled = inventoryContainer.isRunning;
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    protected void init() {
        super.init();
        powerButton = new ImageButton(leftPos + 48, topPos + 33, 18, 18, b -> {
            if (inventoryContainer.rackSlot >= 0) {
                PacketSender.sendServerPower(inventoryContainer, inventoryContainer.rackSlot, !inventoryContainer.isRunning);
            }
        }, Textures.GUI.ButtonPower, true);
        addRenderableWidget(powerButton);
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        if (powerButton.isMouseOver(mouseX, mouseY)) {
            final List<String> tooltip = new ArrayList<>();
            final String text = inventoryContainer.isRunning ? Localization.Computer.TurnOff() : Localization.Computer.TurnOn();
            tooltip.addAll(text.lines().toList());
            copiedDrawHoveringText(graphics, tooltip, mouseX - leftPos, mouseY - topPos, font);
        }
    }

    @Override
    protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(Textures.GUI.Server, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
