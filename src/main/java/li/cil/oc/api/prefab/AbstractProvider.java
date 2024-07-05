package li.cil.oc.api.prefab;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

/**
 * Example base implementation of nanomachine behavior provider.
 * <br>
 * This class takes care of handling the unique identifier used to tell
 * if a behavior is its own when loading from NBT.
 */
public abstract class AbstractProvider implements BehaviorProvider {
    /**
     * Unique identifier used to tell if a behavior is ours when asked to load it.
     */
    protected final String id;

    /**
     * For the ID passed in here, it is suggested to use a one-time generated
     * UUID that is hard-coded into your provider implementation. Take care to
     * use a different one for each provider you create!
     *
     * @param id the unique identifier for this provider.
     */
    protected AbstractProvider(String id) {
        if (id == null) throw new NullPointerException("id must not be null");
        this.id = id;
    }

    /**
     * Called when saving a behavior created using this behavior to NBT.
     * <br>
     * The ID will already have been written, don't overwrite it. Store
     * any additional data you need to restore the behavior here, if any.
     *
     * @param behavior the behavior to persist.
     * @param nbt      the NBT tag to persist it to.
     */
    protected void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
    }

    /**
     * Called when loading a behavior from NBT.
     * <br>
     * Use the data written in {@link #writeBehaviorToNBT} to restore the behavior
     * to its previous state, then return it.
     *
     * @param player the player to restore the behavior for.
     * @param nbt    the NBT tag to load restore the behavior from.
     * @return the restored behavior.
     */
    protected abstract Behavior readBehaviorFromNBT(Player player, CompoundTag nbt);

    // ----------------------------------------------------------------------- //

    @Override
    public CompoundTag save(Behavior behavior) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("provider", id);
        writeBehaviorToNBT(behavior, nbt);
        return nbt;
    }

    @Override
    public Behavior load(Player player, CompoundTag nbt) {
        if (id.equals(nbt.getString("provider"))) {
            return readBehaviorFromNBT(player, nbt);
        } else {
            return null;
        }
    }
}
