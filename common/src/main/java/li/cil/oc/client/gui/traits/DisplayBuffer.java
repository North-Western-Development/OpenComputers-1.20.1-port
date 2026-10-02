package li.cil.oc.client.gui.traits;

import li.cil.oc.util.RenderState;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Stateful trait for screens displaying a text buffer. Implementors (which
 * extend a vanilla {@code Screen}) hold the {@code scale} state and expose it
 * via {@link #scale()} / {@link #setScale(double)}.
 */
public interface DisplayBuffer {
    int bufferX();

    int bufferY();

    int bufferColumns();

    int bufferRows();

    double scale();

    void setScale(double value);

    default void drawBufferLayer(GuiGraphics graphics) {
        setScale(changeSize(bufferColumns(), bufferRows()));

        RenderState.checkError(getClass().getName() + ".drawBufferLayer: entering (aka: wasntme)");

        graphics.pose().pushPose();
        drawBuffer(graphics);
        graphics.pose().popPose();

        RenderState.checkError(getClass().getName() + ".drawBufferLayer: buffer layer");
    }

    void drawBuffer(GuiGraphics graphics);

    double changeSize(double w, double h);
}
