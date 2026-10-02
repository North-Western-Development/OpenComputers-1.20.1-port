package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.world.entity.player.Player;

/**
 * Base class of OC's built-in behavior providers (kept for its name; in Scala it
 * adapted Scala collections to Java ones).
 */
public abstract class ScalaProvider extends AbstractProvider {
    protected ScalaProvider(String id) {
        super(id);
    }

    public abstract Iterable<Behavior> createScalaBehaviors(Player player);

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        return createScalaBehaviors(player);
    }
}
