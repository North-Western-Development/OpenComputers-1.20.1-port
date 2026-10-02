package li.cil.oc.common.item.data;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.util.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class DroneData extends MicrocontrollerData {
    public DroneData() {
        super(Constants.ItemName.Drone);
    }

    public DroneData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public String name = "";

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        name = ItemUtils.getDisplayName(nbt).orElse("");
        if (Strings.isNullOrEmpty(name)) {
            name = RobotData.randomName();
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        if (!Strings.isNullOrEmpty(name)) {
            ItemUtils.setDisplayName(nbt, name);
        }
    }
}
