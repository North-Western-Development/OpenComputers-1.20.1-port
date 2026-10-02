package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.Tier;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabletData extends ItemData {
    public TabletData() {
        super(Constants.ItemName.Tablet);
    }

    public TabletData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public ItemStack[] items = emptyItems();

    public boolean isRunning = false;

    public double energy = 0.0;

    public double maxEnergy = 0.0;

    public int tier = Tier.One;

    public ItemStack container = ItemStack.EMPTY;

    private static ItemStack[] emptyItems() {
        final ItemStack[] result = new ItemStack[32];
        Arrays.fill(result, ItemStack.EMPTY);
        return result;
    }

    private static final String ItemsTag = Settings.namespace + "items";
    private static final String SlotTag = "slot";
    private static final String ItemTag = "item";
    private static final String IsRunningTag = Settings.namespace + "isRunning";
    private static final String EnergyTag = Settings.namespace + "energy";
    private static final String MaxEnergyTag = Settings.namespace + "maxEnergy";
    private static final String TierTag = Settings.namespace + "tier";
    private static final String ContainerTag = Settings.namespace + "container";

    @Override
    public void loadData(CompoundTag nbt) {
        ExtendedNBT.<CompoundTag>foreach(nbt.getList(ItemsTag, Tag.TAG_COMPOUND), slotNbt -> {
            final int slot = slotNbt.getByte(SlotTag);
            if (slot >= 0 && slot < items.length) {
                items[slot] = ItemStack.of(slotNbt.getCompound(ItemTag));
            }
        });
        isRunning = nbt.getBoolean(IsRunningTag);
        energy = nbt.getDouble(EnergyTag);
        maxEnergy = nbt.getDouble(MaxEnergyTag);
        tier = nbt.getInt(TierTag);
        if (nbt.contains(ContainerTag)) {
            container = ItemStack.of(nbt.getCompound(ContainerTag));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        final List<CompoundTag> list = new ArrayList<>();
        for (int slot = 0; slot < items.length; slot++) {
            final ItemStack stack = items[slot];
            if (!stack.isEmpty()) {
                final CompoundTag slotNbt = new CompoundTag();
                slotNbt.putByte(SlotTag, (byte) slot);
                ExtendedNBT.setNewCompoundTag(slotNbt, ItemTag, stack::save);
                list.add(slotNbt);
            }
        }
        ExtendedNBT.setNewTagList(nbt, ItemsTag, list);
        nbt.putBoolean(IsRunningTag, isRunning);
        nbt.putDouble(EnergyTag, energy);
        nbt.putDouble(MaxEnergyTag, maxEnergy);
        nbt.putInt(TierTag, tier);
        if (!container.isEmpty()) ExtendedNBT.setNewCompoundTag(nbt, ContainerTag, container::save);
    }
}
