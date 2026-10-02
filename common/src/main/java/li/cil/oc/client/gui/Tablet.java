package li.cil.oc.client.gui;

import li.cil.oc.client.gui.traits.LockedHotbar;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class Tablet extends DynamicGuiContainer<li.cil.oc.common.container.Tablet> implements LockedHotbar {
    public Tablet(li.cil.oc.common.container.Tablet state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
    }

    @Override
    public ItemStack lockedStack() {
        return inventoryContainer.stack;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (isSlotClickAllowed(slot)) {
            super.slotClicked(slot, slotId, mouseButton, clickType);
        }
    }
}
