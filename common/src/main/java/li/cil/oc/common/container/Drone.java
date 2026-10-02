package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.common.entity.DroneInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Drone extends Player {
    public final int mainInvSize;
    public final int deltaY = 0;

    // This factor is used to make the energy values transferable using
    // MCs 'progress bar' stuff, even though those internally send the
    // values as shorts over the net (for whatever reason).
    private static final int factor = 100;

    private final Container droneInv;
    private final DataSlot globalBufferData;
    private final DataSlot globalBufferSizeData;
    private final DataSlot runningData;
    private final DataSlot selectedSlotData;

    public Drone(MenuType<?> selfType, int id, Inventory playerInventory, Container droneInv, int mainInvSize) {
        super(selfType, id, playerInventory, droneInv);
        this.droneInv = droneInv;
        this.mainInvSize = mainInvSize;

        for (int i = 0; i <= 1; i++) {
            int y = 8 + i * slotSize - deltaY;
            for (int j = 0; j <= 3; j++) {
                int x = 98 + j * slotSize;
                addSlot(new InventorySlot(this, otherInventory, slots.size(), x, y));
            }
        }

        addPlayerInventorySlots(8, 66);

        if (droneInv instanceof DroneInventory inv) {
            li.cil.oc.common.entity.Drone drone = inv.drone;
            globalBufferData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return drone.globalBuffer() / factor;
                }

                @Override
                public void set(int value) {
                    drone.setGlobalBuffer(value * factor);
                }
            });
            globalBufferSizeData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return drone.globalBufferSize() / factor;
                }

                @Override
                public void set(int value) {
                    drone.setGlobalBufferSize(value * factor);
                }
            });
            runningData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return drone.isRunning() ? 1 : 0;
                }

                @Override
                public void set(int value) {
                    drone.setRunning(value != 0);
                }
            });
            selectedSlotData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return drone.selectedSlot();
                }

                @Override
                public void set(int value) {
                    drone.setSelectedSlot(value);
                }
            });
        }
        else {
            globalBufferData = addDataSlot(DataSlot.standalone());
            globalBufferSizeData = addDataSlot(DataSlot.standalone());
            runningData = addDataSlot(DataSlot.standalone());
            selectedSlotData = addDataSlot(DataSlot.standalone());
        }
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.entity.Drone.class;
    }

    public int globalBuffer() {
        return globalBufferData.get() * factor;
    }

    public int globalBufferSize() {
        return globalBufferSizeData.get() * factor;
    }

    public boolean isRunning() {
        return runningData.get() != 0;
    }

    public int selectedSlot() {
        return selectedSlotData.get();
    }

    public String statusText() {
        return synchronizedData.getString("statusText");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        if (droneInv instanceof DroneInventory inv) {
            synchronizedData.putString("statusText", inv.drone.statusText());
        }
        super.detectCustomDataChanges(nbt);
    }

    public class InventorySlot extends StaticComponentSlot {
        public InventorySlot(Player container, Container inventory, int index, int x, int y) {
            super(container, inventory, index, x, y, Drone.this.getHostClass(), li.cil.oc.common.Slot.Any, Tier.Any);
        }

        public boolean isValid() {
            return getContainerSlot() >= 0 && getContainerSlot() < mainInvSize;
        }

        @Override
        public boolean isActive() {
            return isValid() && super.isActive();
        }

        @Override
        public ResourceLocation getBackgroundLocation() {
            if (isValid()) return super.getBackgroundLocation();
            return SlotIcons.get(Tier.None);
        }

        @Override
        public ItemStack getItem() {
            if (isValid()) return super.getItem();
            return ItemStack.EMPTY;
        }
    }
}
