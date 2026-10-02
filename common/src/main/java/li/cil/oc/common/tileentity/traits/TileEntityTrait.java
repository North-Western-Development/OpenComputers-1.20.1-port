package li.cil.oc.common.tileentity.traits;

import li.cil.oc.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Root of all tile entity trait interfaces. It re-declares the members of
 * {@link TileEntity} (and {@link net.minecraft.world.level.block.entity.BlockEntity})
 * that the traits' default methods need, so they can be called on {@code this}
 * from inside an interface. Every implementation is a {@link TileEntity}, which
 * provides all of these. See {@link TileEntity} for the overall design.
 */
public interface TileEntityTrait {
    @Nullable
    Level ocLevel();

    BlockPos ocBlockPos();

    BlockState ocBlockState();

    void ocSetChanged();

    boolean ocIsRemoved();

    int x();

    int y();

    int z();

    BlockPosition position();

    boolean isClient();

    boolean isServer();

    void updateEntity();

    void initialize();

    void dispose();
}
