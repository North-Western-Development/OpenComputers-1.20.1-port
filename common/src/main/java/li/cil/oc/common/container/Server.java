package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.InventorySlots;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Server extends Player {
    public final ItemStack stack;
    public final int rackSlot;
    public final int verticalSlots;

    public boolean isRunning = false;
    public boolean isItem = true;

    public Server(MenuType<?> selfType, int id, Inventory playerInventory, ItemStack stack, Container serverInventory, int tier, int rackSlot) {
        super(selfType, id, playerInventory, serverInventory);
        this.stack = stack;
        this.rackSlot = rackSlot;

        for (int i = 0; i <= 1; i++) {
            InventorySlots.InventorySlot slot = InventorySlots.server[tier][slots.size()];
            addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier);
        }

        verticalSlots = Math.min(3, 1 + tier);
        for (int i = 0; i <= verticalSlots; i++) {
            InventorySlots.InventorySlot slot = InventorySlots.server[tier][slots.size()];
            addSlotToContainer(100, 7 + i * slotSize, slot.slot, slot.tier);
        }

        for (int i = 0; i <= verticalSlots; i++) {
            InventorySlots.InventorySlot slot = InventorySlots.server[tier][slots.size()];
            addSlotToContainer(124, 7 + i * slotSize, slot.slot, slot.tier);
        }

        for (int i = 0; i <= verticalSlots; i++) {
            InventorySlots.InventorySlot slot = InventorySlots.server[tier][slots.size()];
            addSlotToContainer(148, 7 + i * slotSize, slot.slot, slot.tier);
        }

        for (int i = 2; i <= verticalSlots; i++) {
            InventorySlots.InventorySlot slot = InventorySlots.server[tier][slots.size()];
            addSlotToContainer(76, 7 + i * slotSize, slot.slot, slot.tier);
        }

        {
            InventorySlots.InventorySlot slot = InventorySlots.server[tier][slots.size()];
            addSlotToContainer(26, 34, slot.slot, slot.tier);
        }

        // Show the player's inventory.
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.server.component.Server.class;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        if (otherInventory instanceof li.cil.oc.server.component.Server) return super.stillValid(player);
        return player == playerInventory.player;
    }

    @Override
    public void updateCustomData(CompoundTag nbt) {
        super.updateCustomData(nbt);
        isRunning = nbt.getBoolean("isRunning");
        isItem = nbt.getBoolean("isItem");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        super.detectCustomDataChanges(nbt);
        if (otherInventory instanceof li.cil.oc.server.component.Server s) nbt.putBoolean("isRunning", s.machine().isRunning());
        else nbt.putBoolean("isItem", true);
    }
}
