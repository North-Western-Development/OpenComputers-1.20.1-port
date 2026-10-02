package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.Tier;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MicrocontrollerData extends ItemData {
    public MicrocontrollerData(String itemName) {
        super(itemName);
    }

    public MicrocontrollerData() {
        this(Constants.BlockName.Microcontroller);
    }

    public MicrocontrollerData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public int tier = Tier.One;

    public ItemStack[] components = new ItemStack[]{ItemStack.EMPTY};

    public int storedEnergy = 0;

    private static final String TierTag = Settings.namespace + "tier";
    private static final String ComponentsTag = Settings.namespace + "components";
    private static final String StoredEnergyTag = Settings.namespace + "storedEnergy";

    @Override
    public void loadData(CompoundTag nbt) {
        tier = nbt.getByte(TierTag);
        final List<ItemStack> loaded = new ArrayList<>();
        for (CompoundTag tag : ExtendedNBT.toTagArray(nbt.getList(ComponentsTag, Tag.TAG_COMPOUND), CompoundTag.class)) {
            final ItemStack stack = ItemStack.of(tag);
            if (!stack.isEmpty()) loaded.add(stack);
        }
        storedEnergy = nbt.getInt(StoredEnergyTag);

        // Reserve slot for EEPROM if necessary, avoids having to resize the
        // components array in the MCU tile entity, which isn't possible currently.
        final ItemInfo eeprom = li.cil.oc.api.Items.get(Constants.ItemName.EEPROM);
        boolean hasEEPROM = false;
        for (ItemStack stack : loaded) {
            if (li.cil.oc.api.Items.get(stack) == eeprom) {
                hasEEPROM = true;
                break;
            }
        }
        if (!hasEEPROM) {
            loaded.add(ItemStack.EMPTY);
        }
        components = loaded.toArray(new ItemStack[0]);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        nbt.putByte(TierTag, (byte) tier);
        ExtendedNBT.setNewTagList(nbt, ComponentsTag, ExtendedNBT.itemStackIterableToNbt(
            Arrays.stream(components).filter(s -> !s.isEmpty()).toList()));
        nbt.putInt(StoredEnergyTag, storedEnergy);
    }

    public ItemStack copyItemStack() {
        final ItemStack stack = createItemStack();
        final MicrocontrollerData newInfo = new MicrocontrollerData(stack);
        newInfo.saveData(stack);
        return stack;
    }
}
