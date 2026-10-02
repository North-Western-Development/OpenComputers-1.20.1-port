package li.cil.oc.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Stateful trait: implementors hold the widget list and return it from {@link #widgets()}.
 */
@Deprecated
public interface WidgetContainer {
    List<Widget> widgets();

    default <T extends Widget> T addCustomWidget(T widget) {
        widgets().add(widget);
        widget.owner = this;
        return widget;
    }

    default int windowX() {
        return 0;
    }

    default int windowY() {
        return 0;
    }

    default float windowZ() {
        return 0f;
    }

    default void drawWidgets(GuiGraphics graphics) {
        for (Widget widget : widgets()) {
            widget.draw(graphics);
        }
    }
}
