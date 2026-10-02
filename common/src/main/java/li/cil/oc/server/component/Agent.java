package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.event.EventBus;
import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.server.agent.ActivationType;
import li.cil.oc.server.agent.Player;
import li.cil.oc.server.component.traits.InventoryControl;
import li.cil.oc.server.component.traits.InventoryWorldControl;
import li.cil.oc.server.component.traits.TankAware;
import li.cil.oc.server.component.traits.TankControl;
import li.cil.oc.server.component.traits.TankWorldControl;
import li.cil.oc.server.component.traits.WorldControl;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedArguments;
import li.cil.oc.util.ExtendedWorld;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static li.cil.oc.util.ResultWrapper.result;

/**
 * Shared robot/drone component logic (formerly a Scala trait). Implementers
 * provide {@link #agent()} and {@link #checkSideForAction(Arguments, int)}.
 * {@link #fakePlayer()} is resolved here (it is declared by both InventoryAware
 * and WorldAware).
 */
public interface Agent extends WorldControl, InventoryControl, InventoryWorldControl, TankAware, TankControl, TankWorldControl {
    li.cil.oc.api.internal.Agent agent();

    @Override
    default BlockPosition position() {
        return BlockPosition.apply(agent());
    }

    @Override
    default net.minecraft.world.entity.player.Player fakePlayer() {
        return agent().player();
    }

    default Player rotatedPlayer(Direction facing, Direction side) {
        final Player player = (Player) agent().player();
        Player.updatePositionAndRotation(player, facing, side);
        // no need to set inventory, calling agent.Player already did that
        //Player.setPlayerInventoryItems(player)
        return player;
    }

    default Player rotatedPlayer(Direction facing) {
        return rotatedPlayer(facing, agent().facing());
    }

    default Player rotatedPlayer() {
        return rotatedPlayer(agent().facing(), agent().facing());
    }

    // ----------------------------------------------------------------------- //

    @Override
    default Container inventory() {
        return agent().mainInventory();
    }

    @Override
    default int selectedSlot() {
        return agent().selectedSlot();
    }

    @Override
    default void setSelectedSlot(int value) {
        agent().setSelectedSlot(value);
    }

    // ----------------------------------------------------------------------- //

    @Override
    default MultiTank tank() {
        return agent().tank();
    }

    @Override
    default int selectedTank() {
        return agent().selectedTank();
    }

    @Override
    default void setSelectedTank(int value) {
        agent().setSelectedTank(value);
    }

    // ----------------------------------------------------------------------- //

    default boolean canPlaceInAir() {
        final RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(agent());
        EventBus.INSTANCE.post(event);
        return event.isAllowed();
    }

    default void onWorldInteraction(Context context, double duration) {
        context.pause(duration);
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function():string -- Get the name of the agent.")
    default Object[] name(Context context, Arguments args) {
        return result(agent().name());
    }

    private List<Direction> sidesFor(Arguments args, Direction facing) {
        final List<Direction> sides = new ArrayList<>();
        if (args.isInteger(1)) {
            sides.add(checkSideForFace(args, 1, facing));
        } else {
            // Always try the direction we're looking first.
            sides.add(facing);
            for (Direction side : Direction.values()) {
                if (side != facing && side != facing.getOpposite()) sides.add(side);
            }
        }
        return sides;
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean, string -- Perform a 'left click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
    default Object[] swing(Context context, Arguments args) {
        // Swing the equipped tool (left click).
        final Direction facing = checkSideForAction(args, 0);
        final List<Direction> sides = sidesFor(args, facing);
        final boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);

        String reason = null;
        for (Direction side : sides) {
            final Player player = rotatedPlayer(facing, side);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);

            final Pair<Boolean, String> outcome;
            final HitResult hit = pick(player, Settings.get().swingRange);
            final HitResult.Type type = hit != null ? hit.getType() : HitResult.Type.MISS;
            if (type == HitResult.Type.ENTITY) {
                outcome = swingAttack(context, player, ((EntityHitResult) hit).getEntity());
            } else if (type == HitResult.Type.BLOCK) {
                final BlockHitResult blockHit = (BlockHitResult) hit;
                outcome = swingClick(context, player, blockHit.getBlockPos(), blockHit.getDirection());
            } else {
                // Retry with full block bounds, disregarding swing range.
                final Optional<Entity> entity = player.closestEntity(LivingEntity.class);
                if (entity.isPresent()) {
                    outcome = swingAttack(context, player, entity.get());
                } else if (ExtendedWorld.extinguishFire(world(), player, position(), facing)) {
                    onWorldInteraction(context, Settings.get().swingDelay);
                    outcome = Pair.of(true, "fire");
                } else outcome = Pair.of(false, "air");
            }

            player.setPose(Pose.STANDING);
            if (outcome.getLeft()) {
                return result(true, outcome.getRight());
            }
            if (reason == null) reason = outcome.getRight();
        }

        // all side attempts failed - but there could be a partial block that is hard to "see"
        final boolean hasBlock = blockContent(facing).getLeft();
        if (hasBlock) {
            final BlockPosition blockPos = position().offset(facing);
            final Player player = rotatedPlayer(facing, facing);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);
            final Pair<Boolean, String> outcome = swingClick(context, player, blockPos.toBlockPos(), facing);
            player.setPose(Pose.STANDING);
            return result(outcome.getLeft(), outcome.getRight());
        }

        return result(false, reason);
    }

    private Pair<Boolean, String> swingAttack(Context context, Player player, Entity entity) {
        final CapturedDrops drops = beginConsumeDrops(entity);
        player.attack(entity);
        // Mine carts have to be hit quickly in succession to break, so we click
        // until it breaks. But avoid an infinite loop... you never know.
        if (entity instanceof Minecart) {
            for (int i = 0; i < 10 && entity.isAlive(); i++) {
                player.attack(entity);
            }
        }
        endConsumeDrops(player, entity, drops);
        onWorldInteraction(context, Settings.get().swingDelay);
        return Pair.of(true, "entity");
    }

    private Pair<Boolean, String> swingClick(Context context, Player player, BlockPos pos, Direction side) {
        final double breakTime = player.clickBlock(pos, side);
        final boolean broke = breakTime > 0;
        if (broke) {
            onWorldInteraction(context, breakTime);
        }
        return Pair.of(broke, "block");
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false[, duration:number=0]]]):boolean, string -- Perform a 'right click' towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
    default Object[] use(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final List<Direction> sides = sidesFor(args, facing);
        final boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);
        final double duration = args.isDouble(3) ? args.checkDouble(3) : 0.0;

        for (Direction side : sides) {
            final Player player = rotatedPlayer(facing, side);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);

            final Pair<Boolean, String> outcome;
            final HitResult hit = pick(player, Settings.get().useAndPlaceRange);
            if (hit != null && hit.getType() == HitResult.Type.ENTITY && useInteract(player, ((EntityHitResult) hit).getEntity()).consumesAction()) {
                onWorldInteraction(context, Settings.get().useDelay);
                outcome = Pair.of(true, "item_interacted");
            } else if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                final BlockHitResult blockHit = (BlockHitResult) hit;
                final ClickParams params = clickParamsFromHit(blockHit);
                outcome = activationResult(context, player.activateBlockOrUseItem(params.pos(), blockHit.getDirection(), params.hitX(), params.hitY(), params.hitZ(), duration));
            } else {
                ActivationType activationType = ActivationType.None;
                if (canPlaceInAir()) {
                    final ClickParams place = clickParamsForPlace(facing);
                    if (player.placeBlock(0, place.pos(), facing, place.hitX(), place.hitY(), place.hitZ()))
                        activationType = ActivationType.ItemPlaced;
                    else {
                        final ClickParams use = clickParamsForItemUse(facing, side);
                        activationType = player.activateBlockOrUseItem(use.pos(), side.getOpposite(), use.hitX(), use.hitY(), use.hitZ(), duration);
                    }
                }
                if (activationType == ActivationType.None) {
                    if (player.useEquippedItem(duration)) {
                        onWorldInteraction(context, Settings.get().useDelay);
                        outcome = Pair.of(true, "item_used");
                    } else outcome = Pair.of(false, "air");
                } else outcome = activationResult(context, activationType);
            }

            player.setPose(Pose.STANDING);
            if (outcome.getLeft()) {
                return result(true, outcome.getRight());
            }
        }

        return result(false);
    }

    private Pair<Boolean, String> activationResult(Context context, ActivationType activationType) {
        switch (activationType) {
            case BlockActivated:
                onWorldInteraction(context, Settings.get().useDelay);
                return Pair.of(true, "block_activated");
            case ItemPlaced:
                onWorldInteraction(context, Settings.get().useDelay);
                return Pair.of(true, "item_placed");
            case ItemUsed:
                onWorldInteraction(context, Settings.get().useDelay);
                return Pair.of(true, "item_used");
            default:
                return Pair.of(false, "");
        }
    }

    private InteractionResult useInteract(Player player, Entity entity) {
        final CapturedDrops drops = beginConsumeDrops(entity);
        final InteractionResult result = player.interactOn(entity, InteractionHand.MAIN_HAND);
        endConsumeDrops(player, entity, drops);
        return result;
    }

    @Callback(doc = "function(side:number[, face:number=side[, sneaky:boolean=false]]):boolean -- Place a block towards the specified side. The `face' allows a more precise click calibration, and is relative to the targeted blockspace.")
    default Object[] place(Context context, Arguments args) {
        final Direction facing = checkSideForAction(args, 0);
        final List<Direction> sides = sidesFor(args, facing);
        final boolean sneaky = args.isBoolean(2) && args.checkBoolean(2);
        if (agent().mainInventory().getItem(agent().selectedSlot()).isEmpty()) {
            return result(null, "nothing selected");
        }

        for (Direction side : sides) {
            final Player player = rotatedPlayer(facing, side);
            player.setPose(sneaky ? Pose.CROUCHING : Pose.STANDING);
            final HitResult hit = pick(player, Settings.get().useAndPlaceRange);
            final boolean success;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                final BlockHitResult blockHit = (BlockHitResult) hit;
                final ClickParams params = clickParamsFromHit(blockHit);
                success = player.placeBlock(agent().selectedSlot(), params.pos(), blockHit.getDirection(), params.hitX(), params.hitY(), params.hitZ());
            } else if ((hit == null || hit.getType() == HitResult.Type.MISS) && canPlaceInAir() && player.closestEntity(Entity.class).isEmpty()) {
                // Note: the 1.16 code matched `None` here, which never happens (a miss is a BlockHitResult
                // of type MISS), making angel placement unreachable; also accept MISS like 1.12 did.
                final ClickParams params = clickParamsForPlace(facing);
                // blockPos here is the position of the agent
                // When a robot uses angel placement, the BlockItem code offsets the pos to Direction
                // but for a drone, the block at its position is air, which is replaceable, and thus
                // BlockItem does not offset the position. We can do it here to correct that, and the code
                // here is still correct for the robot's use case
                final BlockPos adjustedPos = params.pos().relative(facing);
                // adjustedPos is the position we want to place the block
                // but onItemUse will try to adjust the placement if the target position is not replaceable
                // we don't want that
                final BlockState state = world().getBlockState(adjustedPos);
                if (state.canBeReplaced()) {
                    success = player.placeBlock(agent().selectedSlot(), adjustedPos, facing, params.hitX(), params.hitY(), params.hitZ());
                } else {
                    success = false;
                }
            } else success = false;
            player.setPose(Pose.STANDING);
            if (success) {
                onWorldInteraction(context, Settings.get().placeDelay);
                return result(true);
            }
        }

        return result(false);
    }

    // ----------------------------------------------------------------------- //

    /**
     * Snapshot taken before an entity interaction; replaces Forge's
     * {@code Entity.captureDrops}, which has no vanilla/Fabric equivalent.
     */
    record CapturedDrops(AABB bounds, List<ItemEntity> before) {
    }

    default CapturedDrops beginConsumeDrops(Entity entity) {
        // TODO(port): Forge's captureDrops kept drops from spawning at all; now we look for item
        //  entities that appeared around the entity during the interaction and pick them up.
        final AABB bounds = entity.getBoundingBox().inflate(1.0);
        return new CapturedDrops(bounds, entity.level().getEntitiesOfClass(ItemEntity.class, bounds));
    }

    default void endConsumeDrops(Player player, Entity entity, CapturedDrops drops) {
        // this inventory size check is a HACK to preserve old behavior that a agent can suck items out
        // of the capturedDrops. Ideally, we'd only pick up items off the ground. We could clear the
        // capturedDrops when Player.attack() is called
        // But this felt slightly less hacky, slightly
        if (player.getInventory().getContainerSize() > 0) {
            for (ItemEntity drop : entity.level().getEntitiesOfClass(ItemEntity.class, drops.bounds())) {
                if (drop.isAlive() && !drops.before().contains(drop)) {
                    final net.minecraft.world.item.ItemStack stack = drop.getItem();
                    InventoryUtils.addToPlayerInventory(stack, player, false);
                    if (stack.isEmpty()) drop.discard();
                    else drop.setItem(stack);
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    default Direction checkSideForFace(Arguments args, int n, Direction facing) {
        return agent().toGlobal(ExtendedArguments.checkSideForFace(args, n, agent().toLocal(facing)));
    }

    default HitResult pick(Player player, double range) {
        final Vec3 origin = new Vec3(
                player.getX() + player.facing.getStepX() * 0.5,
                player.getY() + player.facing.getStepY() * 0.5,
                player.getZ() + player.facing.getStepZ() * 0.5);
        final Vec3 blockCenter = origin.add(
                player.facing.getStepX() * 0.51,
                player.facing.getStepY() * 0.51,
                player.facing.getStepZ() * 0.51);
        final Vec3 target = blockCenter.add(
                player.side.getStepX() * range,
                player.side.getStepY() * range,
                player.side.getStepZ() * range);
        final HitResult hit = world().clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        final Optional<Entity> closest = player.closestEntity(Entity.class);
        if (closest.isPresent()) {
            final Entity entity = closest.get();
            if ((entity instanceof LivingEntity || entity instanceof Minecart || entity instanceof li.cil.oc.common.entity.Drone) &&
                    (hit.getType() == HitResult.Type.MISS || player.distanceToSqr(hit.getLocation()) > player.distanceToSqr(entity))) {
                return new EntityHitResult(entity);
            }
        }
        return hit;
    }

    /** Formerly a (BlockPos, Float, Float, Float) tuple. */
    record ClickParams(BlockPos pos, float hitX, float hitY, float hitZ) {
    }

    default ClickParams clickParamsFromHit(BlockHitResult hit) {
        return new ClickParams(hit.getBlockPos(),
                (float) (hit.getLocation().x - hit.getBlockPos().getX()),
                (float) (hit.getLocation().y - hit.getBlockPos().getY()),
                (float) (hit.getLocation().z - hit.getBlockPos().getZ()));
    }

    default ClickParams clickParamsForItemUse(Direction facing, Direction side) {
        final BlockPosition blockPos = position().offset(facing).offset(side);
        return new ClickParams(blockPos.toBlockPos(),
                0.5f - side.getStepX() * 0.5f,
                0.5f - side.getStepY() * 0.5f,
                0.5f - side.getStepZ() * 0.5f);
    }

    default ClickParams clickParamsForPlace(Direction facing) {
        return new ClickParams(position().toBlockPos(),
                0.5f + facing.getStepX() * 0.5f,
                0.5f + facing.getStepY() * 0.5f,
                0.5f + facing.getStepZ() * 0.5f);
    }
}
