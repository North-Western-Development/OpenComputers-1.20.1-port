package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.common.inventory.ServerInventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ServerTemplate {
    private ServerTemplate() {
    }

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier1) ||
            li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier2) ||
            li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier3);
    }

    public static Object[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        final ServerInventory info = new ServerInventory() {
            private final ItemsHolder itemsHolder = new ItemsHolder();

            @Override
            public ItemStack[] items() {
                return itemsHolder.get(this);
            }

            @Override
            public ItemStack container() {
                return stack;
            }

            @Override
            public int rackSlot() {
                return -1;
            }
        };
        final List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < info.getContainerSize(); slot++) {
            final ItemStack item = info.getItem(slot);
            if (item != null) drops.add(item);
        }
        return new Object[]{ingredients, drops.toArray(new ItemStack[0])};
    }

    public static void register() {
        // Disassembler
        li.cil.oc.api.IMC.registerDisassemblerTemplate("Server",
            "li.cil.oc.common.template.ServerTemplate.selectDisassembler",
            "li.cil.oc.common.template.ServerTemplate.disassemble");
    }
}
