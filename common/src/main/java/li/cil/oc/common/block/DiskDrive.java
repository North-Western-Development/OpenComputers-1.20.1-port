package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.tileentity.TileEntityTypes;
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
import java.util.Optional;

public class DiskDrive extends SimpleBlock implements GUI {
    public DiskDrive(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, world, tooltip, flag);
        // TODO(port): integration - ComputerCraft tooltip ("DiskDrive.CC").
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.DiskDrive te) {
            ContainerTypes.openDiskDriveGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.DISK_DRIVE.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.DiskDrive drive && !drive.getItem(0).isEmpty()) {
            return 15;
        }
        return 0;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        // Behavior: sneaking -> Insert[+Eject], not sneaking -> GUI.
        if (player.isCrouching()) {
            if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.DiskDrive drive) {
                // Note: the Scala original compared against null, so this was always true.
                boolean isDiskInDrive = drive.getItem(0) != null;
                boolean isHoldingDisk = drive.canPlaceItem(0, heldItem);
                if (isDiskInDrive) {
                    if (!world.isClientSide) {
                        drive.dropSlot(0, 1, Optional.of(drive.facing()));
                    }
                }
                if (isHoldingDisk) {
                    // Insert the disk.
                    drive.setItem(0, heldItem.split(1));
                }
                return isDiskInDrive || isHoldingDisk;
            }
            return false;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }
}
