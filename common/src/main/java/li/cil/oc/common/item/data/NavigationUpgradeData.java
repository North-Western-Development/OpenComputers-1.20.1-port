package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class NavigationUpgradeData extends ItemData {
    public NavigationUpgradeData() {
        super(Constants.ItemName.NavigationUpgrade);
    }

    public NavigationUpgradeData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public ItemStack map = new ItemStack(Items.FILLED_MAP);

    public MapItemSavedData mapData(Level world) throws Exception {
        final MapItemSavedData data;
        try {
            data = MapItem.getSavedData(map, world);
        } catch (Throwable t) {
            throw new Exception("invalid map");
        }
        // Scala dereferenced the result immediately (NPE -> "invalid map").
        if (data == null) throw new Exception("invalid map");
        return data;
    }

    public int getSize(Level world) throws Exception {
        final MapItemSavedData info = mapData(world);
        return 128 * (1 << info.scale);
    }

    private static final String DataTag = Settings.namespace + "data";
    private static final String MapTag = Settings.namespace + "map";

    @Override
    public void loadData(ItemStack stack) {
        if (stack.hasTag()) {
            loadData(stack.getTag().getCompound(DataTag));
        }
    }

    @Override
    public void saveData(ItemStack stack) {
        saveData(stack.getOrCreateTagElement(DataTag));
    }

    @Override
    public void loadData(CompoundTag nbt) {
        if (nbt.contains(MapTag)) {
            map = ItemStack.of(nbt.getCompound(MapTag));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        if (map != null) {
            ExtendedNBT.setNewCompoundTag(nbt, MapTag, map::save);
        }
    }
}
