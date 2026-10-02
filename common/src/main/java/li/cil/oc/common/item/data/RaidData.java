package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public class RaidData extends ItemData {
    public RaidData() {
        super(Constants.BlockName.Raid);
    }

    public RaidData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public ItemStack[] disks = new ItemStack[0];

    public CompoundTag filesystem = new CompoundTag();

    public Optional<String> label = Optional.empty();

    private static final String DisksTag = Settings.namespace + "disks";
    private static final String FileSystemTag = Settings.namespace + "filesystem";
    private static final String LabelTag = Settings.namespace + "label";

    @Override
    public void loadData(CompoundTag nbt) {
        disks = Arrays.stream(ExtendedNBT.toTagArray(nbt.getList(DisksTag, Tag.TAG_COMPOUND), CompoundTag.class))
            .map(ItemStack::of).toArray(ItemStack[]::new);
        filesystem = nbt.getCompound(FileSystemTag);
        if (nbt.contains(LabelTag)) {
            label = Optional.of(nbt.getString(LabelTag));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        ExtendedNBT.setNewTagList(nbt, DisksTag, ExtendedNBT.itemStackIterableToNbt(Arrays.asList(disks)));
        nbt.put(FileSystemTag, filesystem);
        label.ifPresent(l -> nbt.putString(LabelTag, l));
    }
}
