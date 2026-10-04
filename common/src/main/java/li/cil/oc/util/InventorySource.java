package li.cil.oc.util;

import li.cil.oc.common.transfer.ItemHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * An inventory found in the world together with what provides it: a block
 * (block entity) or an entity (e.g. a chest minecart). Permission checks need
 * to know which one it is (block interaction vs. entity interaction).
 */
public sealed interface InventorySource permits InventorySource.Block, InventorySource.Entity {
    @Nullable
    Direction side();

    ItemHandler inventory();

    record Block(BlockPosition position, @Nullable Direction side, ItemHandler inventory) implements InventorySource {
    }

    record Entity(net.minecraft.world.entity.Entity entity, @Nullable Direction side, ItemHandler inventory) implements InventorySource {
    }
}
