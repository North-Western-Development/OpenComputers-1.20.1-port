package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class Database extends Player {
    public final ItemStack container;
    public final int tier;
    public final int rows;
    public final int offset;
    private final Container databaseInventory;

    public Database(MenuType<?> selfType, int id, Inventory playerInventory, ItemStack container, Container databaseInventory, int tier) {
        super(selfType, id, playerInventory, databaseInventory);
        this.container = container;
        this.tier = tier;
        this.databaseInventory = databaseInventory;
        this.rows = (int) Math.ceil(Math.sqrt(databaseInventory.getContainerSize()));
        this.offset = 8 + new int[]{3, 2, 0}[tier] * slotSize;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < rows; col++) {
                addSlotToContainer(offset + col * slotSize, offset + row * slotSize);
            }
        }

        // Show the player's inventory.
        addPlayerInventorySlots(8, 174);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return null;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return player == playerInventory.player;
    }

    @Override
    public void clicked(int slot, int dragType, ClickType clickType, net.minecraft.world.entity.player.Player player) {
        if (slot >= databaseInventory.getContainerSize() || slot < 0) {
            // if the slot interaction is with the user inventory use
            // default behavior
            super.clicked(slot, dragType, clickType, player);
            return;
        }
        // remove the ghost item
        Slot ghostSlot = this.slots.get(slot);
        if (ghostSlot != null) {
            ItemStack hand = getCarried();
            ItemStack itemToAdd = ItemStack.EMPTY;
            // if the player is holding an item, place a copy
            if (!hand.isEmpty()) {
                itemToAdd = hand.copy();
            }
            ghostSlot.set(itemToAdd);
        }
    }

    @Override
    protected void tryTransferStackInSlot(Slot from, boolean intoPlayerInventory) {
        if (intoPlayerInventory) {
            from.setChanged();
            return;
        }

        ItemStack fromStack = from.getItem().copy();
        if (fromStack.isEmpty()) {
            return;
        }

        fromStack.setCount(1);

        for (int i = 0; i < slots.size(); i++) {
            Slot intoSlot = slots.get(i);
            if (intoSlot.container != from.container) {
                if (!intoSlot.hasItem() && intoSlot.mayPlace(fromStack)) {
                    if (intoSlot.getMaxStackSize() > 0) {
                        intoSlot.set(fromStack);
                        return;
                    }
                }
            }
        }
    }
}
