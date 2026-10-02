package li.cil.oc.common.container;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.client.Textures;
import li.cil.oc.common.Tier;
import li.cil.oc.integration.opencomputers.DriverKeyboard;
import li.cil.oc.integration.opencomputers.DriverScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class Robot extends Player {
    public final RobotInfo info;

    private static final int withScreenHeight = 256;
    private static final int noScreenHeight = 108;
    public final int deltaY;

    // This factor is used to make the energy values transferable using
    // MCs 'progress bar' stuff, even though those internally send the
    // values as shorts over the net (for whatever reason).
    private static final int factor = 100;

    private final DataSlot globalBufferData;
    private final DataSlot globalBufferSizeData;
    private final DataSlot runningData;
    private final DataSlot selectedSlotData;

    public Robot(MenuType<?> selfType, int id, Inventory playerInventory, Container robot, RobotInfo info) {
        super(selfType, id, playerInventory, robot);
        this.info = info;
        this.deltaY = info.screenBuffer.isPresent() ? 0 : withScreenHeight - noScreenHeight;

        addSlotToContainer(170 + 0 * slotSize, 232 - deltaY, li.cil.oc.common.Slot.Tool);
        addSpecialSlot(170 + 1 * slotSize, 232 - deltaY, info.slot1, info.tier1);
        addSpecialSlot(170 + 2 * slotSize, 232 - deltaY, info.slot2, info.tier2);
        addSpecialSlot(170 + 3 * slotSize, 232 - deltaY, info.slot3, info.tier3);

        generateSlotsFor(0);

        addPlayerInventorySlots(6, 174 - deltaY);

        if (robot instanceof li.cil.oc.common.tileentity.Robot te) {
            globalBufferData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (int) te.globalBuffer / factor;
                }

                @Override
                public void set(int value) {
                    te.globalBuffer = value * factor;
                }
            });
            globalBufferSizeData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (int) te.globalBufferSize / factor;
                }

                @Override
                public void set(int value) {
                    te.globalBufferSize = value * factor;
                }
            });
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
            selectedSlotData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return te.selectedSlot();
                }

                @Override
                public void set(int value) {
                    te.setSelectedSlot(value);
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
        return li.cil.oc.common.tileentity.Robot.class;
    }

    // Like addSlotToContainer, but handles the very special, much edge case with screen & keyboard.
    public void addSpecialSlot(int x, int y, String slot, int tier) {
        int index = slots.size();
        addSlot(new StaticComponentSlot(this, otherInventory, index, x, y, getHostClass(), slot, tier) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (DriverScreen.INSTANCE.worksWith(stack, getHostClass())) return false;
                if (DriverKeyboard.INSTANCE.worksWith(stack, getHostClass())) return false;
                return super.mayPlace(stack);
            }
        });
    }

    // Slot.x and Slot.y are final, so have to rebuild when scrolling
    public void generateSlotsFor(int scroll) {
        int maxRows = Math.max(info.mainInvSize / 4, 4);
        for (int i = 0; i < maxRows; i++) {
            int y = 156 + (i - scroll) * slotSize - deltaY;
            for (int j = 0; j <= 3; j++) {
                int x = 170 + j * slotSize;
                int idx = 4 + j + 4 * i;
                InventorySlot slot = new InventorySlot(this, otherInventory, idx, x, y, i >= scroll && i < scroll + 4);
                slot.index = idx;
                if (slots.size() <= idx) addSlot(slot);
                else slots.set(idx, slot);
            }
        }
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

    public class InventorySlot extends StaticComponentSlot {
        private final boolean enabled;

        public InventorySlot(Player container, Container inventory, int index, int x, int y, boolean enabled) {
            super(container, inventory, index, x, y, Robot.this.getHostClass(), li.cil.oc.common.Slot.Any, Tier.Any);
            this.enabled = enabled;
        }

        public boolean isValid() {
            return getContainerSlot() >= 4 && getContainerSlot() < 4 + info.mainInvSize;
        }

        @Override
        public boolean isActive() {
            return enabled && isValid() && super.isActive();
        }

        @Override
        public ResourceLocation getBackgroundLocation() {
            if (isValid()) return super.getBackgroundLocation();
            return Textures.Icons.get(Tier.None);
        }

        @Override
        public ItemStack getItem() {
            if (isValid()) return super.getItem();
            return ItemStack.EMPTY;
        }
    }
}
