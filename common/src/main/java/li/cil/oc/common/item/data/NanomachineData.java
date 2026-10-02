package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class NanomachineData extends ItemData {
    public NanomachineData() {
        super(Constants.ItemName.Nanomachines);
    }

    public NanomachineData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public NanomachineData(ControllerImpl controller) {
        this();
        uuid = controller.uuid;
        final CompoundTag nbt = new CompoundTag();
        controller.configuration.saveData(nbt, true);
        configuration = Optional.of(nbt);
    }

    public String uuid = "";

    public Optional<CompoundTag> configuration = Optional.empty();

    private static final String UUIDTag = Settings.namespace + "uuid";
    private static final String ConfigurationTag = Settings.namespace + "configuration";

    @Override
    public void loadData(CompoundTag nbt) {
        uuid = nbt.getString(UUIDTag);
        if (nbt.contains(ConfigurationTag)) {
            configuration = Optional.of(nbt.getCompound(ConfigurationTag));
        } else {
            configuration = Optional.empty();
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        nbt.putString(UUIDTag, uuid);
        configuration.ifPresent(c -> nbt.put(ConfigurationTag, c));
    }
}
