package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.InteractiveImageRenderer;

/**
 * Shows the "missing" image with a warning tooltip (formerly anonymous
 * {@code TextureImageRenderer with InteractiveImageRenderer} instances).
 */
public class MissingImageRenderer extends TextureImageRenderer implements InteractiveImageRenderer {
    private final String tooltip;

    public MissingImageRenderer(String tooltip) {
        super(TextureImageProvider.ManualMissingItem);
        this.tooltip = tooltip;
    }

    @Override
    public String getTooltip(String tooltip) {
        return this.tooltip;
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY) {
        return false;
    }
}
