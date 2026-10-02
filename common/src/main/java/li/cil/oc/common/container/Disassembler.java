package li.cil.oc.common.container;

import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.util.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Disassembler extends Player {
    public final Container disassembler;

    public Disassembler(MenuType<?> selfType, int id, Inventory playerInventory, Container disassembler) {
        super(selfType, id, playerInventory, disassembler);
        this.disassembler = disassembler;
        addSlot(new StaticComponentSlot(this, otherInventory, slots.size(), 80, 35, getHostClass(), "ocitem", Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!container.canPlaceItem(getContainerSlot(), stack)) return false;
                return allowDisassembling(stack) &&
                    (((Settings.get().disassembleAllTheThings || Items.get(stack) != null) &&
                        ItemUtils.getIngredients(playerInventory.player.level().getRecipeManager(), playerInventory.player.level().registryAccess(), stack).length > 0) ||
                        DisassemblerTemplates.select(stack).isPresent());
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    private static boolean allowDisassembling(ItemStack stack) {
        return !stack.isEmpty() && (!stack.hasTag() || !stack.getTag().getBoolean(Settings.namespace + "undisassemblable"));
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Disassembler.class;
    }

    public double disassemblyProgress() {
        return synchronizedData.getDouble("disassemblyProgress");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        if (disassembler instanceof li.cil.oc.common.tileentity.Disassembler te) {
            synchronizedData.putDouble("disassemblyProgress", te.progress());
        }
        super.detectCustomDataChanges(nbt);
    }
}
