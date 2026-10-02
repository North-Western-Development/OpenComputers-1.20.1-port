package li.cil.oc;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.init.Items;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * OpenComputers' creative tab.
 * <p>
 * Items opt into it via {@code new Item.Properties().arch$tab(CreativeTab.TAB)};
 * Architectury then lists them in the tab. Additional pre-configured stacks
 * (configured robots, loot disks, ...) are appended through
 * {@link Items#decorateCreativeTab(NonNullList)}, like {@code fillItemList}
 * did on 1.16.5.
 */
public final class CreativeTab {
    private CreativeTab() {
    }

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(OpenComputers.ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup." + OpenComputers.Name))
            .icon(CreativeTab::icon)
            .displayItems((parameters, output) -> {
                final NonNullList<ItemStack> list = NonNullList.create();
                Items.decorateCreativeTab(list);
                for (ItemStack stack : list) {
                    if (stack != null && !stack.isEmpty()) {
                        output.accept(stack);
                    }
                }
            })));

    private static ItemStack icon() {
        final ItemInfo info = li.cil.oc.api.Items.get(Constants.BlockName.CaseTier1);
        return info != null ? info.createItemStack(1) : ItemStack.EMPTY;
    }

    /**
     * Registers the tab. Called from {@link OpenComputers#init()} before items are registered.
     */
    public static void register() {
        TABS.register();
    }
}
