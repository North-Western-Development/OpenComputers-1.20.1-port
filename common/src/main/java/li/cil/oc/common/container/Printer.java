package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.PrintData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Printer extends Player {
    public final Container printer;

    public Printer(MenuType<?> selfType, int id, Inventory playerInventory, Container printer) {
        super(selfType, id, playerInventory, printer);
        this.printer = printer;
        addSlot(new StaticComponentSlot(this, otherInventory, slots.size(), 18, 19, getHostClass(), Slot.Filtered, Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!container.canPlaceItem(getContainerSlot(), stack)) return false;
                return PrintData.materialValue(stack) > 0;
            }
        });
        addSlot(new StaticComponentSlot(this, otherInventory, slots.size(), 18, 51, getHostClass(), Slot.Filtered, Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!container.canPlaceItem(getContainerSlot(), stack)) return false;
                return PrintData.inkValue(stack) > 0;
            }
        });
        addSlotToContainer(152, 35);

        // Show the player's inventory.
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Printer.class;
    }

    public double progress() {
        return synchronizedData.getDouble("progress");
    }

    public int maxAmountMaterial() {
        return synchronizedData.getInt("maxAmountMaterial");
    }

    public int amountMaterial() {
        return synchronizedData.getInt("amountMaterial");
    }

    public int maxAmountInk() {
        return synchronizedData.getInt("maxAmountInk");
    }

    public int amountInk() {
        return synchronizedData.getInt("amountInk");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        if (printer instanceof li.cil.oc.common.tileentity.Printer te) {
            synchronizedData.putDouble("progress", te.isPrinting() ? te.progress() / 100.0 : 0);
            synchronizedData.putInt("maxAmountMaterial", te.maxAmountMaterial);
            synchronizedData.putInt("amountMaterial", te.amountMaterial);
            synchronizedData.putInt("maxAmountInk", te.maxAmountInk);
            synchronizedData.putInt("amountInk", te.amountInk);
        }
        super.detectCustomDataChanges(nbt);
    }
}
