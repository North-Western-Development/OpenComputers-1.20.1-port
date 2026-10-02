package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.common.item.data.NavigationUpgradeData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class NavigationUpgradeTemplate {
    private NavigationUpgradeTemplate() {
    }

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.NavigationUpgrade);
    }

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        final NavigationUpgradeData info = new NavigationUpgradeData(stack);
        final ItemStack[] result = new ItemStack[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            final ItemStack part = ingredients[i];
            result[i] = part.getItem() == Items.FILLED_MAP ? info.map : part;
        }
        return result;
    }

    public static void register() {
        // Disassembler
        li.cil.oc.api.IMC.registerDisassemblerTemplate(
            "Navigation Upgrade",
            "li.cil.oc.common.template.NavigationUpgradeTemplate.selectDisassembler",
            "li.cil.oc.common.template.NavigationUpgradeTemplate.disassemble");
    }
}
