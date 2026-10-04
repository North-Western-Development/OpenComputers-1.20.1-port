package li.cil.oc.server.component.traits;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.platform.ComponentPlatform;
import li.cil.oc.common.platform.PlatformHooks;
import li.cil.oc.common.transfer.ContainerItemHandler;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.InventorySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface WorldAware {
    BlockPosition position();

    default Level world() {
        return position().world.get();
    }

    default Player fakePlayer() {
        final ServerPlayer player = ComponentPlatform.fakePlayer((ServerLevel) world(), Settings.get().fakePlayerProfile);
        final BlockPosition position = position();
        player.setPos(position.x + 0.5, position.y + 0.5, position.z + 0.5);
        return player;
    }

    default boolean mayInteract(BlockPosition blockPos, Direction face) {
        try {
            return ComponentPlatform.mayInteract(fakePlayer(), blockPos.toBlockPos(), face);
        } catch (Throwable t) {
            OpenComputers.log.warn("Some event handler threw up while checking for permission to access a block.", t);
            return true;
        }
    }

    default boolean mayInteract(Entity entity) {
        try {
            return ComponentPlatform.mayInteractWithEntity(fakePlayer(), entity);
        } catch (Throwable t) {
            OpenComputers.log.warn("Some event handler threw up while checking for permission to access an entity.", t);
            return true;
        }
    }

    /**
     * Whether the device may access the inventory: the container must still be usable
     * by the fake player, and protection hooks must allow interacting with the block or
     * entity (e.g. a chest minecart) that provides it.
     */
    default boolean mayInteract(InventorySource source) {
        if (source.inventory() instanceof ContainerItemHandler wrapper && wrapper.getContainer() != null
                && !wrapper.getContainer().stillValid(fakePlayer())) {
            return false;
        }
        if (source instanceof InventorySource.Block block) {
            return mayInteract(block.position(), block.side() != null ? block.side() : Direction.UP);
        } else if (source instanceof InventorySource.Entity entity) {
            return mayInteract(entity.entity());
        }
        return true;
    }

    default <T extends Entity> List<T> entitiesInBounds(Class<T> clazz, AABB bounds) {
        return world().getEntitiesOfClass(clazz, bounds);
    }

    default <T extends Entity> List<T> entitiesInBlock(Class<T> clazz, BlockPosition blockPos) {
        return entitiesInBounds(clazz, blockPos.bounds());
    }

    default <T extends Entity> List<T> entitiesOnSide(Class<T> clazz, Direction side) {
        return entitiesInBlock(clazz, position().offset(side));
    }

    default <T extends Entity> Optional<T> closestEntity(Class<T> clazz, Direction side) {
        final BlockPosition blockPos = position().offset(side);
        final List<T> candidates = world().getEntitiesOfClass(clazz, blockPos.bounds(), e -> true);
        if (candidates.isEmpty()) return Optional.empty();
        final Player player = fakePlayer();
        return candidates.stream().min(Comparator.comparingDouble(player::distanceToSqr));
    }

    /**
     * @return (something, what), e.g. (true, "solid").
     */
    default Pair<Boolean, String> blockContent(Direction side) {
        final Optional<Entity> closest = closestEntity(Entity.class, side);
        if (closest.isPresent() && (closest.get() instanceof LivingEntity || closest.get() instanceof Minecart)) {
            return Pair.of(true, "entity");
        }
        final BlockPosition blockPos = position().offset(side);
        final Level world = world();
        final BlockPos pos = blockPos.toBlockPos();
        final BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return Pair.of(false, "air");
        }
        // Note: the 1.16 code had an inverted check here (`!isInstanceOf[IFluidBlock]`) which
        // reported every non-air block as liquid; restored the 1.12 behaviour (fluid blocks only).
        else if (state.getBlock() instanceof LiquidBlock) {
            return Pair.of(!canBreak(world, pos), "liquid");
        } else if (state.canBeReplaced()) {
            return Pair.of(!canBreak(world, pos), "replaceable");
        } else if (state.getCollisionShape(world, pos, CollisionContext.empty()).isEmpty()) {
            return Pair.of(true, "passable");
        } else {
            return Pair.of(true, "solid");
        }
    }

    private boolean canBreak(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel && fakePlayer() instanceof ServerPlayer player) {
            return PlatformHooks.canBreakBlock(serverLevel, pos, player);
        }
        return true;
    }
}
