package li.cil.oc.common.container;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Relay extends Player {
    private final Container relay;

    public Relay(MenuType<?> selfType, int id, Inventory playerInventory, Container relay) {
        super(selfType, id, playerInventory, relay);
        this.relay = relay;
        addSlotToContainer(151, 15, Slot.CPU);
        addSlotToContainer(151, 34, Slot.Memory);
        addSlotToContainer(151, 53, Slot.HDD);
        addSlot(new StaticComponentSlot(this, otherInventory, slots.size(), 178, 15, getHostClass(), Slot.Card, Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                ItemInfo info = Items.get(stack);
                if (info == null || (info != Items.get(Constants.ItemName.WirelessNetworkCardTier1) && info != Items.get(Constants.ItemName.WirelessNetworkCardTier2) &&
                    info != Items.get(Constants.ItemName.LinkedCard))) return false;
                return super.mayPlace(stack);
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Relay.class;
    }

    public int relayDelay() {
        return synchronizedData.getInt("relayDelay");
    }

    public int relayAmount() {
        return synchronizedData.getInt("relayAmount");
    }

    public int maxQueueSize() {
        return synchronizedData.getInt("maxQueueSize");
    }

    public int packetsPerCycleAvg() {
        return synchronizedData.getInt("packetsPerCycleAvg");
    }

    public int queueSize() {
        return synchronizedData.getInt("queueSize");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        if (relay instanceof li.cil.oc.common.tileentity.Relay te) {
            synchronizedData.putInt("relayDelay", te.relayDelay());
            synchronizedData.putInt("relayAmount", te.relayAmount());
            synchronizedData.putInt("maxQueueSize", te.maxQueueSize());
            synchronizedData.putInt("packetsPerCycleAvg", (int) te.packetsPerCycleAvg().apply());
            synchronizedData.putInt("queueSize", te.queue().size());
        }
        super.detectCustomDataChanges(nbt);
    }
}
