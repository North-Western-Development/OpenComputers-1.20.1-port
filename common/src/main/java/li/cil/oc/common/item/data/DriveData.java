package li.cil.oc.common.item.data;

import li.cil.oc.Settings;
import li.cil.oc.server.fs.FileSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DriveData extends ItemData {
    public DriveData() {
        super(null);
    }

    public DriveData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public boolean isUnmanaged = false;

    public String lockInfo = "";

    public boolean isLocked() {
        return lockInfo != null && !lockInfo.isEmpty();
    }

    private static final String UnmanagedTag = Settings.namespace + "unmanaged";
    private static final String LockTag = Settings.namespace + "lock";

    @Override
    public void loadData(CompoundTag nbt) {
        isUnmanaged = nbt.getBoolean(UnmanagedTag);
        lockInfo = nbt.contains(LockTag) ? nbt.getString(LockTag) : "";
    }

    @Override
    public void saveData(CompoundTag nbt) {
        nbt.putBoolean(UnmanagedTag, isUnmanaged);
        nbt.putString(LockTag, lockInfo);
    }

    // ----------------------------------------------------------------------- //

    public static void lock(ItemStack stack, Player player) {
        final String key = player.getName().getString();
        final DriveData data = new DriveData(stack);
        if (!data.isLocked()) {
            data.lockInfo = key != null && !key.isEmpty() ? key : "notch"; // meaning: "unknown"
            data.saveData(stack);
        }
    }

    public static void setUnmanaged(ItemStack stack, boolean unmanaged) {
        final DriveData data = new DriveData(stack);
        if (data.isUnmanaged != unmanaged) {
            FileSystem.INSTANCE.removeAddress(stack);
            data.lockInfo = "";
        }
        data.isUnmanaged = unmanaged;
        data.saveData(stack);
    }
}
