package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class Rack extends RedstoneAware implements PowerAcceptor, StateAware, GUI {
    public Rack(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double energyThroughput() {
        return Settings.get().serverRackRate;
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Rack te) {
            ContainerTypes.openRackGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.RACK.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Rack rack) {
            Optional<Integer> slotOpt = rack.slotAt(side, hitX, hitY, hitZ);
            if (slotOpt.isPresent()) {
                int slot = slotOpt.get();
                // Snap to grid to get same behavior on client and server...
                Vec3 hitVec = new Vec3((int) (hitX * 16f) / 16f, (int) (hitY * 16f) / 16f, (int) (hitZ * 16f) / 16f);
                float rotation;
                switch (side) {
                    case WEST:
                        rotation = (float) Math.toRadians(90);
                        break;
                    case NORTH:
                        rotation = (float) Math.toRadians(180);
                        break;
                    case EAST:
                        rotation = (float) Math.toRadians(270);
                        break;
                    default:
                        rotation = 0;
                }
                // Rotate *centers* of pixels to keep association when reversing axis.
                Vec3 localHitVec = rotate(hitVec.add(-0.5 + 1 / 32f, -0.5 + 1 / 32f, -0.5 + 1 / 32f), rotation).add(0.5 - 1 / 32f, 0.5 - 1 / 32f, 0.5 - 1 / 32f);
                int globalX = (int) (localHitVec.x * 16.05f); // [0, 15], work around floating point inaccuracies
                int globalY = (int) (localHitVec.y * 16.05f); // [0, 15], work around floating point inaccuracies
                int localX = (side.getAxis() != Direction.Axis.Z ? 15 - globalX : globalX) - 1;
                int localY = (15 - globalY) - 2 - 3 * slot;
                if (localX >= 0 && localX < 14 && localY >= 0 && localY < 3) {
                    RackMountable mountable = rack.getMountable(slot);
                    if (mountable != null && mountable.onActivate(player, hand, heldItem, localX / 14f, localY / 3f)) {
                        return true; // Activation handled by mountable.
                    }
                }
            }
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    public Vec3 rotate(Vec3 v, float t) {
        double cos = Math.cos(t);
        double sin = Math.sin(t);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }
}
