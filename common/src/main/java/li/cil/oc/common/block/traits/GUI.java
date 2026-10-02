package li.cil.oc.common.block.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Marker for blocks that open a GUI (menu) when activated by a non-sneaking player.
 * <p>
 * Only valid on subclasses of {@link li.cil.oc.common.block.SimpleBlock}; the behaviour
 * (activation opens the GUI, no vanilla menu provider) is implemented in SimpleBlock via
 * an {@code instanceof GUI} check, since Java interfaces cannot override class methods.
 */
public interface GUI {
    void openGui(ServerPlayer player, Level world, BlockPos pos);
}
