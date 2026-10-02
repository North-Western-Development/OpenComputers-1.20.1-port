package li.cil.oc.client.gui.widget;

import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.GuiUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ProgressBar extends Widget {
    public final int x;
    public final int y;

    public double level = 0.0;

    public ProgressBar(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int width() {
        return 140;
    }

    @Override
    public int height() {
        return 12;
    }

    public ResourceLocation barTexture() {
        return Textures.GUI.Bar;
    }

    @Override
    public void draw(GuiGraphics graphics) {
        if (level > 0) {
            final float u0 = 0;
            final float u1 = (float) level;
            final float v0 = 0;
            final float v1 = 1;
            final int tx = owner.windowX() + x;
            final int ty = owner.windowY() + y;
            final float w = (float) (width() * level);

            GuiUtil.texturedQuad(graphics, barTexture(), tx, ty, tx + w, ty + height(), owner.windowZ(), u0, u1, v0, v1);
        }
    }
}
