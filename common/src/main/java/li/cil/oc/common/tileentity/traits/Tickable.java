package li.cil.oc.common.tileentity.traits;

/**
 * Marker for tile entities that need {@link TileEntity#updateEntity()} to be called every tick
 * (on both sides). The blocks' {@code getTicker} checks for this interface.
 */
public interface Tickable extends TileEntityTrait {
}
