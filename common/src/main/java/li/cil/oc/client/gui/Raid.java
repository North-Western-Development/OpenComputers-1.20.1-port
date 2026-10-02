package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.Textures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class Raid extends DynamicGuiContainer<li.cil.oc.common.container.Raid> {
    public Raid(li.cil.oc.common.container.Raid state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float dt, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1); // Required under Linux.
        graphics.blit(Textures.GUI.Raid, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
