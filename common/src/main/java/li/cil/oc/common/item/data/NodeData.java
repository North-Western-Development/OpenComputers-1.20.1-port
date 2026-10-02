package li.cil.oc.common.item.data;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Visibility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

// Generic one for items that are used as components; gets the items node info.
public class NodeData extends ItemData {
    public static final String NodeTag = "node";
    public static final String AddressTag = "address";
    public static final String BufferTag = "buffer";
    public static final String VisibilityTag = "visibility";

    public NodeData() {
        super(null);
    }

    public NodeData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public Optional<String> address = Optional.empty();
    public Optional<Double> buffer = Optional.empty();
    public Optional<Visibility> visibility = Optional.empty();

    private static final String DataTag = Settings.namespace + "data";

    @Override
    public void loadData(CompoundTag nbt) {
        final CompoundTag nodeNbt = nbt.getCompound(DataTag).getCompound(NodeTag);
        if (nodeNbt.contains(AddressTag)) {
            address = Optional.of(nodeNbt.getString(AddressTag));
        }
        if (nodeNbt.contains(BufferTag)) {
            buffer = Optional.of(nodeNbt.getDouble(BufferTag));
        }
        if (nodeNbt.contains(VisibilityTag)) {
            visibility = Optional.of(Visibility.values()[nodeNbt.getInt(VisibilityTag)]);
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        if (!nbt.contains(DataTag)) {
            nbt.put(DataTag, new CompoundTag());
        }
        final CompoundTag dataNbt = nbt.getCompound(DataTag);
        if (!dataNbt.contains(NodeTag)) {
            dataNbt.put(NodeTag, new CompoundTag());
        }
        final CompoundTag nodeNbt = dataNbt.getCompound(NodeTag);
        address.ifPresent(v -> nodeNbt.putString(AddressTag, v));
        buffer.ifPresent(v -> nodeNbt.putDouble(BufferTag, v));
        visibility.ifPresent(v -> nodeNbt.putInt(VisibilityTag, v.ordinal()));
    }
}
