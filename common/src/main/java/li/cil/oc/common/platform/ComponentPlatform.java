package li.cil.oc.common.platform;

import com.mojang.authlib.GameProfile;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Loader specific hooks used by the server side components
 * ({@code li.cil.oc.server.component.*}). Implemented in
 * {@code li.cil.oc.common.platform.forge.ComponentPlatformImpl} and
 * {@code li.cil.oc.common.platform.fabric.ComponentPlatformImpl}.
 */
public final class ComponentPlatform {
    private ComponentPlatform() {
    }

    /**
     * Shared fake player for the given level and profile (Forge
     * {@code FakePlayerFactory.get}, Fabric {@code FakePlayer.get}).
     * Callers set the position themselves.
     */
    @ExpectPlatform
    public static ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        throw new AssertionError();
    }

    /**
     * Asks protection hooks whether the player may "right click" (interact
     * with) the block at the specified position (Forge
     * {@code PlayerInteractEvent.RightClickBlock}, Fabric {@code UseBlockCallback}).
     */
    @ExpectPlatform
    public static boolean mayInteract(Player player, BlockPos pos, Direction face) {
        throw new AssertionError();
    }

    /**
     * Asks protection hooks whether the player may interact with (right click)
     * the entity, e.g. to access its inventory (Forge
     * {@code PlayerInteractEvent.EntityInteract}, Fabric {@code UseEntityCallback}).
     */
    @ExpectPlatform
    public static boolean mayInteractWithEntity(Player player, Entity entity) {
        throw new AssertionError();
    }

    /**
     * Whether the item entity about to be dropped ("tossed") by the player
     * may be spawned (Forge {@code ItemTossEvent}; always true on Fabric).
     */
    @ExpectPlatform
    public static boolean canTossItem(ItemEntity item, Player player) {
        throw new AssertionError();
    }
}
