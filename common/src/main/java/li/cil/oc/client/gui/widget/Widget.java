package li.cil.oc.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

@Deprecated
public abstract class Widget {
    public WidgetContainer owner;

    public abstract int x();

    public abstract int y();

    public abstract int width();

    public abstract int height();

    public abstract void draw(GuiGraphics graphics);
}
