package li.cil.oc.api;

import net.minecraft.nbt.CompoundTag;

/**
 * An object that can be persisted to an NBT tag and restored back from it.
 */
public interface Persistable {
    /**
     * Restores a previous state of the object from the specified NBT tag.
     *
     * @param nbt the tag to read the state from.
     */
    void loadData(CompoundTag nbt);

    /**
     * Saves the current state of the object into the specified NBT tag.
     * <br>
     * This should write the state in such a way that it can be restored when
     * {@link #loadData} is called with that tag.
     *
     * @param nbt the tag to save the state to.
     */
    void saveData(CompoundTag nbt);
}