package li.cil.oc.integration.jei;

import li.cil.oc.common.EventHandler;
import li.cil.oc.integration.Mod;
import li.cil.oc.integration.ModProxy;
import li.cil.oc.integration.Mods;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI integration proxy. The actual integration is {@link ModPluginOpenComputers}, which JEI
 * discovers itself (Forge {@code @JeiPlugin}, Fabric {@code "jei_mod_plugin"} entrypoint) and only
 * loads on the client. This class holds the runtime state shared with OC's client code; it must not
 * reference client-only Minecraft classes (it is loaded on servers that have JEI installed).
 */
public final class ModJEI implements ModProxy {
    public static final ModJEI INSTANCE = new ModJEI();

    private ModJEI() {
    }

    @Override
    public Mod getMod() {
        return Mods.JustEnoughItems;
    }

    // ----------------------------------------------------------------------- //

    public static volatile Optional<IJeiRuntime> runtime = Optional.empty();

    public static volatile Optional<IIngredientManager> ingredientRegistry = Optional.empty();

    private static final List<ItemStack> disksForRuntime = new ArrayList<>();

    private static boolean scheduled = false;

    /**
     * Adds a loot disk the server told us about to JEI's item list (client side only; called by the
     * client packet handler when JEI is present).
     */
    public static void addDiskAtRuntime(ItemStack stack) {
        final Optional<IIngredientManager> registry = ingredientRegistry;
        if (registry.isEmpty()) return;
        if (registry.get().getAllIngredients(VanillaTypes.ITEM_STACK).stream().anyMatch(s -> ItemStack.matches(s, stack))) {
            return;
        }
        synchronized (disksForRuntime) {
            disksForRuntime.add(stack.copy());
            if (!scheduled) {
                EventHandler.scheduleClient(() -> {
                    final List<ItemStack> disks;
                    synchronized (disksForRuntime) {
                        disks = new ArrayList<>(disksForRuntime);
                        disksForRuntime.clear();
                        scheduled = false;
                    }
                    ingredientRegistry.ifPresent(r -> r.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, disks));
                });
                scheduled = true;
            }
        }
    }
}
