package li.cil.oc.client.gui.traits;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Base class for simple, non-container OC windows (formerly a trait on Screen;
 * every implementer used it as its primary superclass).
 */
public abstract class Window extends Screen {
    public int leftPos = 0;
    public int topPos = 0;
    public int imageWidth = 0;
    public int imageHeight = 0;

    protected Window(Component title) {
        super(title);
    }

    public int windowWidth() {
        return 176;
    }

    public int windowHeight() {
        return 166;
    }

    public abstract ResourceLocation backgroundImage();

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        imageWidth = windowWidth();
        imageHeight = windowHeight();
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        // The texture has the size of the window (1.16.5's AbstractGui.blit took the texture
        // height before the width, hence the "intentionally backwards" arguments there).
        graphics.blit(backgroundImage(), leftPos, topPos, 0, 0, imageWidth, imageHeight, windowWidth(), windowHeight());

        super.render(graphics, mouseX, mouseY, dt);
    }
}
