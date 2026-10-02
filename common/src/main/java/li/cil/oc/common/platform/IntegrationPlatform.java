package li.cil.oc.common.platform;

import com.mojang.authlib.GameProfile;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Loader-specific hooks used by the built-in integrations (vanilla block drivers,
 * nanomachines). Implemented in {@code li.cil.oc.common.platform.forge.IntegrationPlatformImpl}
 * and {@code li.cil.oc.common.platform.fabric.IntegrationPlatformImpl}.
 */
public final class IntegrationPlatform {
    private IntegrationPlatform() {
    }

    /**
     * A shared fake player for the given level and profile
     * (Forge {@code FakePlayerFactory.get}, Fabric {@code FakePlayer.get}).
     */
    @ExpectPlatform
    public static ServerPlayer getFakePlayer(ServerLevel level, GameProfile profile) {
        throw new AssertionError();
    }

    /**
     * Asks protection hooks whether the player may right-click (use) the block.
     * Forge: posts {@code PlayerInteractEvent.RightClickBlock} and checks it was not canceled / denied.
     * Fabric: invokes {@code UseBlockCallback} and checks it did not return FAIL.
     */
    @ExpectPlatform
    public static boolean canUseBlock(Player player, BlockPos pos, Direction face) {
        throw new AssertionError();
    }

    /**
     * Asks protection hooks whether the player may left-click (start breaking) the block.
     * Forge: posts {@code PlayerInteractEvent.LeftClickBlock} and checks it was not canceled / denied.
     * Fabric: invokes {@code AttackBlockCallback} and checks it did not return FAIL.
     */
    @ExpectPlatform
    public static boolean canAttackBlock(Player player, BlockPos pos, Direction face) {
        throw new AssertionError();
    }
}
