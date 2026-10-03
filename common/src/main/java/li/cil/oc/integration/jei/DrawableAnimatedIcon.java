package li.cil.oc.integration.jei;

import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Used to simulate an animated texture. Client only.
 *
 * @author Vexatos
 */
public class DrawableAnimatedIcon implements IDrawableAnimated {
    private final ResourceLocation resourceLocation;
    private final int u, v, width, height, textureWidth, textureHeight;
    private final ITickTimer tickTimer;
    private final int uOffset, vOffset;
    private final int paddingTop, paddingBottom, paddingLeft, paddingRight;

    public DrawableAnimatedIcon(ResourceLocation resourceLocation, int u, int v, int width, int height, int textureWidth, int textureHeight,
                                ITickTimer tickTimer, int uOffset, int vOffset) {
        this(resourceLocation, u, v, width, height, textureWidth, textureHeight, tickTimer, uOffset, vOffset, 0, 0, 0, 0);
    }

    public DrawableAnimatedIcon(ResourceLocation resourceLocation, int u, int v, int width, int height, int textureWidth, int textureHeight,
                                ITickTimer tickTimer, int uOffset, int vOffset,
                                int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        this.resourceLocation = resourceLocation;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.tickTimer = tickTimer;
        this.uOffset = uOffset;
        this.vOffset = vOffset;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
    }

    @Override
    public int getWidth() {
        return width + paddingLeft + paddingRight;
    }

    @Override
    public int getHeight() {
        return height + paddingTop + paddingBottom;
    }

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        final int animationValue = tickTimer.getValue();

        final int uOffsetTotal = uOffset * animationValue;
        final int vOffsetTotal = vOffset * animationValue;

        final int x = xOffset + paddingLeft;
        final int y = yOffset + paddingTop;
        graphics.blit(resourceLocation, x, y, u + uOffsetTotal, v + vOffsetTotal, width, height, textureWidth, textureHeight);
    }
}
