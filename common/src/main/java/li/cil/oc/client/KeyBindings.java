package li.cil.oc.client;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.OpenComputers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private KeyBindings() {
    }

    private static boolean isActive(InputConstants.Key input) {
        final long window = Minecraft.getInstance().getWindow().getWindow();
        switch (input.getType()) {
            case MOUSE:
                return GLFW.glfwGetMouseButton(window, input.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE:
                return false; // GLFW doesn't have a glfwGetScancode method to test these.
            case KEYSYM:
                return GLFW.glfwGetKey(window, input.getValue()) == GLFW.GLFW_PRESS;
            default:
                return false;
        }
    }

    /**
     * The key currently bound to the given mapping (vanilla has no public
     * getter for it, so we go through its persisted name).
     */
    public static InputConstants.Key getBoundKey(KeyMapping keyBinding) {
        return InputConstants.getKey(keyBinding.saveString());
    }

    /**
     * Replacement for Forge's {@code KeyMapping.isActiveAndMatches}.
     */
    // TODO(port): Forge key conflict contexts / modifiers are not available; only the bound key is compared.
    public static boolean isActiveAndMatches(KeyMapping keyBinding, InputConstants.Key input) {
        return input != null && !keyBinding.isUnbound() && getBoundKey(keyBinding).equals(input);
    }

    public static boolean showExtendedTooltips() {
        if (extendedTooltip.isDown()) return true;
        // We have to know if the keybind is pressed even if the active screen doesn't pass events.
        // TODO(port): Forge's KeyConflictContext.GUI check and key modifiers are gone.
        if (extendedTooltip.isUnbound()) return false;
        final InputConstants.Key key = getBoundKey(extendedTooltip);
        if (key == InputConstants.UNKNOWN) return false;
        return isActive(key);
    }

    public static boolean isAnalyzeCopyingAddress() {
        if (analyzeCopyAddr.isDown()) return true;
        // Poll as well, see note on the mappings below.
        return !analyzeCopyAddr.isUnbound() && isActive(getBoundKey(analyzeCopyAddr));
    }

    public static String getKeyBindingName(KeyMapping keyBinding) {
        return keyBinding.getTranslatedKeyMessage().getString();
    }

    // TODO(port): the conflict contexts (GUI for extendedTooltip, IN_GAME for analyzeCopyAddr and
    //  "screen is an InputBuffer" for clipboardPaste) are Forge-only; vanilla mappings have none.
    //  On Fabric, vanilla's KeyMapping.MAP holds a single mapping per key, so the defaults
    //  (left shift / left control) may shadow sneak / sprint. The state of extendedTooltip and
    //  analyzeCopyAddr is therefore also polled directly from GLFW above.
    public static final KeyMapping extendedTooltip = new KeyMapping("key.opencomputers.extendedTooltip",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, OpenComputers.Name);

    public static final KeyMapping analyzeCopyAddr = new KeyMapping("key.opencomputers.analyzeCopyAddress",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, OpenComputers.Name);

    public static final KeyMapping clipboardPaste = new KeyMapping("key.opencomputers.clipboardPaste",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, OpenComputers.Name);
}
