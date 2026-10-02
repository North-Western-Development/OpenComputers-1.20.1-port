package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class HoverBootsData extends ItemData {
    public HoverBootsData() {
        super(Constants.ItemName.HoverBoots);
    }

    public HoverBootsData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public double charge = 0.0;

    private static final String ChargeTag = Settings.namespace + "charge";

    @Override
    public void loadData(CompoundTag nbt) {
        charge = nbt.getDouble(ChargeTag);
    }

    @Override
    public void saveData(CompoundTag nbt) {
        nbt.putDouble(ChargeTag, charge);
    }
}
