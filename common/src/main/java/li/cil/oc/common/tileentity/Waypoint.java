package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.server.network.Waypoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Waypoint extends TileEntity implements Environment, Rotatable, RedstoneAware, Tickable {
    public final Component node = li.cil.oc.api.Network.newNode(this, Visibility.Network).
        withComponent("waypoint").
        create();

    public String label = "";

    private static final String LabelTag = Settings.namespace + "label";

    public Waypoint(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Component node() {
        return node;
    }

    @Override
    public Direction[] validFacings() {
        return Direction.values();
    }

    // ----------------------------------------------------------------------- //

    @Callback(doc = "function(): string -- Get the current label of this waypoint.")
    public Object[] getLabel(Context context, Arguments args) {
        return result(label);
    }

    @Callback(doc = "function(value:string) -- Set the label for this waypoint.")
    public Object[] setLabel(Context context, Arguments args) {
        final String value = args.checkString(0);
        label = value.length() > 32 ? value.substring(0, 32) : value;
        context.pause(0.5);
        return null;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (isClient()) {
            final Level level = getLevel();
            final Direction facing = facing();
            final Vec3 origin = position().toVec3().add(facing.getStepX() * 0.5, facing.getStepY() * 0.5, facing.getStepZ() * 0.5);
            final float dx = (level.random.nextFloat() - 0.5f) * 0.8f;
            final float dy = (level.random.nextFloat() - 0.5f) * 0.8f;
            final float dz = (level.random.nextFloat() - 0.5f) * 0.8f;
            final float vx = (level.random.nextFloat() - 0.5f) * 0.2f + facing.getStepX() * 0.3f;
            final float vy = (level.random.nextFloat() - 0.5f) * 0.2f + facing.getStepY() * 0.3f - 0.5f;
            final float vz = (level.random.nextFloat() - 0.5f) * 0.2f + facing.getStepZ() * 0.3f;
            level.addParticle(ParticleTypes.PORTAL, origin.x + dx, origin.y + dy, origin.z + dz, vx, vy, vz);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        EventHandler.scheduleServer(() -> Waypoints.add(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        Waypoints.remove(this);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        label = nbt.getString(LabelTag);
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        nbt.putString(LabelTag, label);
    }

    @Override
    public void loadForClient(CompoundTag nbt) {
        super.loadForClient(nbt);
        label = nbt.getString(LabelTag);
    }

    @Override
    public void saveForClient(CompoundTag nbt) {
        super.saveForClient(nbt);
        nbt.putString(LabelTag, label);
    }
}
