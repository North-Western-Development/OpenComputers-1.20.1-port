package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.property.PropertyRunning;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;
import java.util.List;

public class Case extends RedstoneAware implements PowerAcceptor, StateAware, GUI {
    public final int tier;

    public Case(Properties props, int tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing, PropertyRunning.Running);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipBody(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        addLines(tooltip, Tooltip.get(getClass().getSimpleName().toLowerCase(), slots()));
    }

    private String slots() {
        switch (tier) {
            case 0:
                return "2/1/1";
            case 1:
                return "2/2/2";
            case 2:
            case 3:
                return "3/2/3";
            default:
                return "0/0/0";
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double energyThroughput() {
        return Settings.get().caseRate[tier];
    }

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Case te && te.stillValid(player)) {
            ContainerTypes.openCaseGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.tileentity.Case(TileEntityTypes.CASE.get(), pos, state, tier);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (player.isCrouching()) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Case computer
                && !computer.machine().isRunning() && computer.stillValid(player)) {
                computer.machine().start();
            }
            return true;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean removedByPlayer(BlockState state, Level world, BlockPos pos, Player player) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Case c) {
            String name = player.getName().getString();
            if (c.isCreative() && (!player.isCreative() || !c.canInteract(name))) return false;
            return c.canInteract(name) && super.removedByPlayer(state, world, pos, player);
        }
        return super.removedByPlayer(state, world, pos, player);
    }
}
