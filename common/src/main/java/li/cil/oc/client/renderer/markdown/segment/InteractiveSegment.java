package li.cil.oc.client.renderer.markdown.segment;

import java.util.Optional;

/**
 * Segments that can react to mouse presence and input.
 * <p>
 * The currently hovered interactive segment is picked in the render process
 * and returned there. Calling code can then decide whether to render the
 * segment's tooltip, for example. It should also notice the currently hovered
 * segment when a left-click occurs.
 */
public interface InteractiveSegment {
    /**
     * The tooltip that should be displayed when this segment is being hovered.
     */
    default Optional<String> tooltip() {
        return Optional.empty();
    }

    /**
     * Should be called by whatever is rendering the document when a left mouse
     * click occurs.
     * <p>
     * The mouse coordinates are expected to be in the same frame of reference as
     * the document.
     *
     * @param mouseX the X coordinate of the mouse cursor.
     * @param mouseY the Y coordinate of the mouse cursor.
     * @return whether the click was processed (true) or ignored (false).
     */
    default boolean onMouseClick(int mouseX, int mouseY) {
        return false;
    }

    // Called during the render call on the currently hovered interactive segment.
    // Useful to track hover state, e.g. for link highlighting.
    default void notifyHover() {
    }

    // Collision check, test if coordinate is inside this interactive segment.
    default Optional<InteractiveSegment> checkHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        if (mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h) return Optional.of(this);
        else return Optional.empty();
    }
}
