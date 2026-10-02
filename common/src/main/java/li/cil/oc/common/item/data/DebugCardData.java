package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.server.component.DebugCard.AccessContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class DebugCardData extends ItemData {
    public DebugCardData() {
        super(Constants.ItemName.DebugCard);
    }

    public DebugCardData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public Optional<AccessContext> access = Optional.empty();

    private static final String DataTag = Settings.namespace + "data";

    @Override
    public void loadData(CompoundTag nbt) {
        access = AccessContext.loadData(dataTag(nbt));
    }

    @Override
    public void saveData(CompoundTag nbt) {
        final CompoundTag tag = dataTag(nbt);
        AccessContext.remove(tag);
        access.ifPresent(a -> a.saveData(tag));
    }

    private CompoundTag dataTag(CompoundTag nbt) {
        if (!nbt.contains(DataTag)) {
            nbt.put(DataTag, new CompoundTag());
        }
        return nbt.getCompound(DataTag);
    }
}
