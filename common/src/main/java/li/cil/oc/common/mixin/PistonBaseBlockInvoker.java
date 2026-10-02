package li.cil.oc.common.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code PistonBaseBlock.moveBlocks} (private in 1.20.1) for the piston upgrade.
 */
@Mixin(PistonBaseBlock.class)
public interface PistonBaseBlockInvoker {
    @Invoker("moveBlocks")
    boolean oc$moveBlocks(Level level, BlockPos pos, Direction direction, boolean extending);
}
