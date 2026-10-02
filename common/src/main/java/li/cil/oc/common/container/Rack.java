package li.cil.oc.common.container;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Rack extends Player {
    public static final int MaxConnections = 4;

    public final Container rack;
    public final boolean[][] nodePresence = new boolean[4][4];
    public final Optional<Direction>[][] nodeMapping;
    public boolean isRelayEnabled = false;

    @SuppressWarnings("unchecked")
    public Rack(MenuType<?> selfType, int id, Inventory playerInventory, Container rack) {
        super(selfType, id, playerInventory, rack);
        this.rack = rack;
        nodeMapping = new Optional[rack.getContainerSize()][];
        for (int i = 0; i < nodeMapping.length; i++) {
            nodeMapping[i] = new Optional[4];
            Arrays.fill(nodeMapping[i], Optional.empty());
        }

        addSlotToContainer(20, 23, Slot.RackMountable);
        addSlotToContainer(20, 43, Slot.RackMountable);
        addSlotToContainer(20, 63, Slot.RackMountable);
        addSlotToContainer(20, 83, Slot.RackMountable);
        addPlayerInventorySlots(8, 128);
    }

    @Override
    protected Class<? extends EnvironmentHost> getHostClass() {
        return li.cil.oc.common.tileentity.Rack.class;
    }

    @Override
    public void updateCustomData(CompoundTag nbt) {
        super.updateCustomData(nbt);
        ListTag mapping = nbt.getList("nodeMapping", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < Math.min(mapping.size(), nodeMapping.length); i++) {
            int[] sides = mapping.getIntArray(i);
            Optional<Direction>[] target = nodeMapping[i];
            for (int j = 0; j < Math.min(sides.length, target.length); j++) {
                target[j] = sides[j] >= 0 ? Optional.of(Direction.from3DDataValue(sides[j])) : Optional.empty();
            }
        }
        boolean[] presence = ExtendedNBT.getBooleanArray(nbt, "nodePresence");
        for (int i = 0; i < nodePresence.length; i++) {
            for (int j = 0; j < MaxConnections; j++) {
                int index = i * MaxConnections + j;
                if (index < presence.length) nodePresence[i][j] = presence[index];
            }
        }
        isRelayEnabled = nbt.getBoolean("isRelayEnabled");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        super.detectCustomDataChanges(nbt);
        if (rack instanceof li.cil.oc.common.tileentity.Rack te) {
            List<IntArrayTag> mapping = new ArrayList<>();
            for (Optional<Direction>[] sides : te.nodeMapping) {
                int[] values = new int[sides.length];
                for (int i = 0; i < sides.length; i++) {
                    values[i] = sides[i].map(Direction::ordinal).orElse(-1);
                }
                mapping.add(ExtendedNBT.toNbt(values));
            }
            ExtendedNBT.setNewTagList(nbt, "nodeMapping", mapping);
            boolean[] presence = new boolean[te.getContainerSize() * MaxConnections];
            for (int slot = 0; slot < te.getContainerSize(); slot++) {
                RackMountable mountable = te.getMountable(slot);
                if (mountable != null) {
                    presence[slot * MaxConnections] = true;
                    for (int index = 0; index < Math.min(MaxConnections - 1, mountable.getConnectableCount()); index++) {
                        presence[slot * MaxConnections + 1 + index] = mountable.getConnectableAt(index) != null;
                    }
                }
            }
            ExtendedNBT.setBooleanArray(nbt, "nodePresence", presence);
            nbt.putBoolean("isRelayEnabled", te.isRelayEnabled);
        }
    }
}
