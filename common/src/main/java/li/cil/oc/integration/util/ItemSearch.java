package li.cil.oc.integration.util;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Hooks for item list mods (JEI and the like); client side only.
 */
public final class ItemSearch {
    private ItemSearch() {
    }

    public static final Set<BooleanSupplier> focusedInput = new LinkedHashSet<>();
    public static final Set<StackFocusing> stackFocusing = new LinkedHashSet<>();

    public static boolean isInputFocused() {
        for (BooleanSupplier f : focusedInput) {
            if (f.getAsBoolean()) return true;
        }
        return false;
    }

    public static ItemStack hoveredStack(AbstractContainerScreen<?> container, int mouseX, int mouseY) {
        for (StackFocusing f : stackFocusing) {
            final ItemStack stack = f.hoveredStack(container, mouseX, mouseY);
            if (stack != null && !stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    @FunctionalInterface
    public interface StackFocusing {
        /** @return the hovered stack, or {@link ItemStack#EMPTY}. */
        ItemStack hoveredStack(AbstractContainerScreen<?> container, int mouseX, int mouseY);
    }
}
