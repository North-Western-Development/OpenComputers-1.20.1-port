package li.cil.oc.common.block;

import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.GUI;
import li.cil.oc.common.container.ContainerTypes;
import li.cil.oc.common.item.data.RaidData;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class Raid extends SimpleBlock implements GUI {
    public Raid(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    @Override
    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        super.tooltipTail(stack, world, tooltip, advanced);
        if (li.cil.oc.util.Tooltip.showExtended()) {
            RaidData data = new RaidData(stack);
            for (ItemStack disk : data.disks) {
                if (!disk.isEmpty()) {
                    tooltip.add(Component.literal("- " + disk.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Raid te) {
            ContainerTypes.openRaidGui(player, te);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.RAID.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Raid raid) {
            for (boolean ok : raid.presence) {
                if (!ok) return 0;
            }
            return 15;
        }
        return 0;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Raid tileEntity) {
            RaidData data = new RaidData(stack);
            for (int i = 0; i < Math.min(data.disks.length, tileEntity.getContainerSize()); i++) {
                tileEntity.setItem(i, data.disks[i]);
            }
            data.label.ifPresent(tileEntity.label::setLabel);
            if (!data.filesystem.isEmpty()) {
                tileEntity.tryCreateRaid(data.filesystem.getCompound("node").getString("address"));
                tileEntity.filesystem.ifPresent(fs -> fs.loadData(data.filesystem));
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof li.cil.oc.common.tileentity.Raid tileEntity) {
            builder = builder.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f -> {
                ItemStack stack = createItemStack();
                boolean hasItems = false;
                for (ItemStack item : tileEntity.items()) {
                    if (!item.isEmpty()) {
                        hasItems = true;
                        break;
                    }
                }
                if (hasItems) {
                    RaidData data = new RaidData();
                    data.disks = tileEntity.items().clone();
                    tileEntity.filesystem.ifPresent(fs -> fs.saveData(data.filesystem));
                    data.label = Optional.ofNullable(tileEntity.label.getLabel());
                    data.saveData(stack);
                }
                f.accept(stack);
            });
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && player.isCreative() && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Raid tileEntity) {
            for (ItemStack item : tileEntity.items()) {
                if (!item.isEmpty()) {
                    Block.dropResources(state, world, pos, tileEntity, player, player.getMainHandItem());
                    break;
                }
            }
        }
        super.playerWillDestroy(world, pos, state, player);
    }
}
