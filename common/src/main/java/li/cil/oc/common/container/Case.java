package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;

public class Case extends Player {
    private final Container computer;
    private final DataSlot runningData;

    public Case(MenuType<?> selfType, int id, Inventory playerInventory, Container computer, int tier) {
        super(selfType, id, playerInventory, computer);
        this.computer = computer;

        for (int i = 0; i <= (tier >= Tier.Three ? 2 : 1); i++) {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(98, 16 + i * slotSize, slot.slot, slot.tier);
        }

        for (int i = 0; i <= (tier == Tier.One ? 0 : 1); i++) {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(120, 16 + (i + 1) * slotSize, slot.slot, slot.tier);
        }

        for (int i = 0; i <= (tier == Tier.One ? 0 : 1); i++) {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(142, 16 + i * slotSize, slot.slot, slot.tier);
        }

        if (tier >= Tier.Three) {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(142, 16 + 2 * slotSize, slot.slot, slot.tier);
        }

        {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(120, 16, slot.slot, slot.tier);
        }

        if (tier == Tier.One) {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(120, 16 + 2 * slotSize, slot.slot, slot.tier);
        }

        {
            InventorySlots.InventorySlot slot = InventorySlots.computer[tier][getItems().size()];
            addSlotToContainer(48, 34, slot.slot, slot.tier);
        }

        // Show the player's inventory.
        addPlayerInventorySlots(8, 84);

        if (computer instanceof li.cil.oc.common.tileentity.Case te) {
            runningData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return te.isRunning() ? 1 : 0;
                }

                @Override
                public void set(int value) {
                    te.setRunning(value != 0);
                }
            });
        }
        else runningData = addDataSlot(DataSlot.standalone());
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Case.class;
    }

    public boolean isRunning() {
        return runningData.get() != 0;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return super.stillValid(player) && (!(computer instanceof li.cil.oc.common.tileentity.Case te) || te.canInteract(player.getName().getString()));
    }
}
