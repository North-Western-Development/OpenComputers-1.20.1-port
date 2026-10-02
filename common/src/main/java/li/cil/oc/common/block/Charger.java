package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

public class Charger extends RedstoneAware implements PowerAcceptor, StateAware, GUI {
    public Charger(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double energyThroughput() {
        return Settings.get().chargerRate;
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Charger te) {
            ContainerTypes.openChargerGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.CHARGER.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        return true;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)) {
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Charger charger) {
                if (!world.isClientSide) {
                    charger.invertSignal = !charger.invertSignal;
                    charger.chargeSpeed = 1.0 - charger.chargeSpeed;
                    PacketSender.sendChargerState(charger);
                    Wrench.wrenchUsed(player, pos);
                }
                return true;
            }
            return false;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Charger charger) {
            charger.onNeighborChanged();
        }
        super.neighborChanged(state, world, pos, block, fromPos, moved);
    }
}
