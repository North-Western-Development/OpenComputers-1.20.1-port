package li.cil.oc.client.gui.traits;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.client.KeyBindings;
import li.cil.oc.client.Textures;
import li.cil.oc.integration.util.ItemSearch;
import li.cil.oc.util.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Stateful trait for screens forwarding keyboard / clipboard input to a text buffer.
 * <p>
 * Implementors extend a vanilla {@code Screen}, hold one {@link State} instance
 * (returned by {@link #inputBufferState()}) and wire the Screen callbacks to the
 * {@code handle*} / {@code inputBuffer*} defaults, calling the super implementation
 * when a handler returns false (Scala's {@code super} chain):
 * <pre>
 * public boolean keyPressed(int k, int s, int m) { return handleKeyPressed(k, s, m) || super.keyPressed(k, s, m); }
 * public boolean keyReleased(int k, int s, int m) { return handleKeyReleased(k, s, m) || super.keyReleased(k, s, m); }
 * public boolean charTyped(char c, int m) { return handleCharTyped(c, m) || super.charTyped(c, m); }
 * public boolean mouseClicked(double x, double y, int b) { return handleMouseClicked(x, y, b) || super.mouseClicked(x, y, b); }
 * public void tick() { super.tick(); inputBufferTick(); }
 * public void removed() { super.removed(); inputBufferRemoved(); }
 * public boolean isPauseScreen() { return false; }
 * </pre>
 */
public interface InputBuffer extends DisplayBuffer {
    final class State {
        private final Map<Integer, Character> pressedKeys = new HashMap<>();

        private long showKeyboardMissing = 0L;

        private boolean hasQueuedKey = false;
        private int queuedKey = 0;
        private char queuedChar = '\u0000';
        private char highSurrogate = '\u0000';
    }

    State inputBufferState();

    TextBuffer buffer();

    boolean hasKeyboard();

    /** Screen#shouldCloseOnEsc; separate name because interface methods are not remapped with Minecraft's. */
    boolean inputShouldCloseOnEsc();

    /** Screen#onClose; see {@link #inputShouldCloseOnEsc()}. */
    void inputClose();

    @Override
    default int bufferColumns() {
        return buffer() == null ? 0 : buffer().getViewportWidth();
    }

    @Override
    default int bufferRows() {
        return buffer() == null ? 0 : buffer().getViewportHeight();
    }

    default void pushQueuedKey(int keyCode) {
        final State s = inputBufferState();
        flushQueuedKey();
        s.hasQueuedKey = true;
        s.queuedKey = keyCode;
        s.queuedChar = GLFWTranslator.keyToChar(keyCode);
    }

    default void pushQueuedChar(char character) {
        final State s = inputBufferState();
        if (s.hasQueuedKey) {
            if (!Character.isSurrogate(character)) s.queuedChar = character;
            // Flush either way as the next code point will be unrelated.
            flushQueuedKey();
        }
        // text_input happens independent of key events
        if (Character.isSurrogate(character)) {
            // Convert two successive surrogates back into a code point.
            if (Character.isHighSurrogate(character)) s.highSurrogate = character;
            else buffer().textInput(Character.toCodePoint(s.highSurrogate, character), null);
        } else buffer().textInput(character, null);
    }

    default void flushQueuedKey() {
        final State s = inputBufferState();
        if (s.hasQueuedKey) {
            s.hasQueuedKey = false;
            // Key repeats are sent as key_down again, except for modifier keys.
            if (!s.pressedKeys.containsKey(s.queuedKey) || !ignoreRepeat(s.queuedKey)) {
                final int lwjglCode = GLFWTranslator.glfwToLWJGL(s.queuedKey);
                if (lwjglCode > 0) {
                    s.pressedKeys.put(s.queuedKey, s.queuedChar);
                    if (buffer() != null) buffer().keyDown(s.queuedChar, lwjglCode, null);
                } else if (s.queuedChar > 0) {
                    s.pressedKeys.put(s.queuedKey, s.queuedChar);
                    if (buffer() != null) buffer().keyDown(s.queuedChar, 0, null);
                }
            }
        }
    }

    @Override
    default void drawBufferLayer(GuiGraphics graphics) {
        DisplayBuffer.super.drawBufferLayer(graphics);

        if (System.currentTimeMillis() - inputBufferState().showKeyboardMissing < 1000) {
            final int x = bufferX() + buffer().renderWidth() - 16;
            final int y = bufferY() + buffer().renderHeight() - 16;

            graphics.blit(Textures.GUI.KeyboardMissing, x, y, 0, 0, 16, 16, 16, 16);

            RenderState.checkError(getClass().getName() + ".drawBufferLayer: keyboard icon");
        }
    }

    /**
     * Call after {@code super.tick()}.
     */
    default void inputBufferTick() {
        flushQueuedKey();
    }

    /**
     * Call after {@code super.removed()}.
     */
    default void inputBufferRemoved() {
        // Note: 1.16.5 toggled KeyboardHandler.setSendRepeatsToGui in init/removed; since 1.19
        // key repeats are always delivered to screens.
        final State s = inputBufferState();
        if (buffer() != null) {
            flushQueuedKey();
            for (Map.Entry<Integer, Character> entry : s.pressedKeys.entrySet()) {
                final int lwjglCode = GLFWTranslator.glfwToLWJGL(entry.getKey());
                if (lwjglCode > 0) buffer().keyUp(entry.getValue(), lwjglCode, null);
                else buffer().keyUp(entry.getValue(), 0, null);
            }
        }
    }

    default boolean onInput(InputConstants.Key input) {
        if (KeyBindings.isActiveAndMatches(KeyBindings.clipboardPaste, input)) {
            if (buffer() != null) {
                if (hasKeyboard()) buffer().clipboard(Minecraft.getInstance().keyboardHandler.getClipboard(), null);
                else inputBufferState().showKeyboardMissing = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    private static boolean ignoreRepeat(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL ||
                keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
                keyCode == GLFW.GLFW_KEY_MENU ||
                keyCode == GLFW.GLFW_KEY_LEFT_ALT ||
                keyCode == GLFW.GLFW_KEY_RIGHT_ALT ||
                keyCode == GLFW.GLFW_KEY_LEFT_SHIFT ||
                keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT ||
                keyCode == GLFW.GLFW_KEY_LEFT_SUPER ||
                keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }

    /** Input is not captured while an item list mod's (JEI) search field in a container screen has focus. */
    private boolean isItemSearchFocused() {
        return this instanceof AbstractContainerScreen<?> && ItemSearch.isInputFocused();
    }

    /**
     * @return true if consumed; otherwise call {@code super.charTyped}.
     */
    default boolean handleCharTyped(char codePt, int mods) {
        if (!isItemSearchFocused()) {
            if (buffer() != null) {
                if (hasKeyboard()) pushQueuedChar(codePt);
                else inputBufferState().showKeyboardMissing = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if consumed; otherwise call {@code super.keyPressed}.
     */
    default boolean handleKeyPressed(int keyCode, int scanCode, int mods) {
        if (!isItemSearchFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE && inputShouldCloseOnEsc()) {
                inputClose();
                return true;
            }
            if (onInput(InputConstants.getKey(keyCode, scanCode))) return true;
            if (buffer() != null && keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                if (hasKeyboard()) pushQueuedKey(keyCode);
                else inputBufferState().showKeyboardMissing = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if consumed; otherwise call {@code super.keyReleased}.
     */
    default boolean handleKeyReleased(int keyCode, int scanCode, int mods) {
        if (!isItemSearchFocused()) {
            flushQueuedKey();
            final Character character = inputBufferState().pressedKeys.remove(keyCode);
            if (character != null) {
                final int lwjglCode = GLFWTranslator.glfwToLWJGL(keyCode);
                if (lwjglCode > 0) {
                    buffer().keyUp(character, lwjglCode, null);
                    return true;
                } else if (character > 0) {
                    buffer().keyUp(character, 0, null);
                    return true;
                }
            }
            // Wasn't pressed while viewing the screen.
        }
        return false;
    }

    /**
     * @return true if consumed; otherwise call {@code super.mouseClicked}.
     */
    default boolean handleMouseClicked(double x, double y, int button) {
        if (onInput(InputConstants.Type.MOUSE.getOrCreate(button))) return true;
        if (buffer() != null && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (hasKeyboard()) buffer().clipboard(Minecraft.getInstance().keyboardHandler.getClipboard(), null);
            else inputBufferState().showKeyboardMissing = System.currentTimeMillis();
            return true;
        }
        return false;
    }
}
