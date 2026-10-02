package li.cil.oc.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class Waypoint extends net.minecraft.client.gui.screens.Screen {
    public final li.cil.oc.common.tileentity.Waypoint waypoint;
    public final int imageWidth = 176;
    public final int imageHeight = 24;
    public int leftPos = 0;
    public int topPos = 0;

    public EditBox textField;

    public Waypoint(li.cil.oc.common.tileentity.Waypoint waypoint) {
        super(Component.empty());
        this.waypoint = waypoint;
    }

    @Override
    public void tick() {
        super.tick();
        textField.tick();
        final BlockPos pos = waypoint.getBlockPos();
        if (minecraft.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        textField = new EditBox(font, leftPos + 7, topPos + 8, 164 - 12, 12, Component.empty()) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int mods) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    final String value = textField.getValue();
                    final String label = value.length() > 32 ? value.substring(0, 32) : value;
                    if (!label.equals(waypoint.label)) {
                        waypoint.label = label;
                        PacketSender.sendWaypointLabel(waypoint);
                        onClose();
                    }
                    return true;
                }
                return super.keyPressed(keyCode, scanCode, mods);
            }
        };
        textField.setMaxLength(32);
        textField.setBordered(false);
        textField.setCanLoseFocus(false);
        textField.setTextColor(0xFFFFFF);
        textField.setValue(waypoint.label);
        addWidget(textField);

        setInitialFocus(textField);
        // Note: KeyboardHandler.setSendRepeatsToGui is gone, repeats always reach screens.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        super.render(graphics, mouseX, mouseY, dt);
        RenderSystem.setShaderColor(1, 1, 1, 1); // Required under Linux.
        graphics.blit(Textures.GUI.Waypoint, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        textField.render(graphics, mouseX, mouseY, dt);
    }
}
