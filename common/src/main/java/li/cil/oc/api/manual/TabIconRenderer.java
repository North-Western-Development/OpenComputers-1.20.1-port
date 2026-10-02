package li.cil.oc.api.manual;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Allows defining a renderer for a manual tab.
 * <p/>
 * Each renderer instance represents the single graphic it is drawing. To
 * provide different graphics for different tabs you'll need to create
 * multiple tab renderer instances.
 * <p/>
 *
 * @see li.cil.oc.api.prefab.ItemStackTabIconRenderer
 * @see li.cil.oc.api.prefab.TextureTabIconRenderer
 */
public interface TabIconRenderer {
    /**
     * Called when icon of a tab should be rendered.
     * <p/>
     * This should render something in a 16x16 area. The pose stack of the
     * passed {@link GuiGraphics} has been adjusted so that drawing starts at
     * (0,0,0), and should go to (16,16,0).
     *
     * @param graphics the GUI rendering context (use {@link GuiGraphics#pose()}
     *                 for the current transformation).
     * <p/>
     * <em>Client side only.</em>
     */
    void render(GuiGraphics graphics);
}
