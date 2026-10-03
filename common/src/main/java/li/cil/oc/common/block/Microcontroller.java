package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.block.traits.PowerAcceptor;
import li.cil.oc.common.block.traits.StateAware;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.tileentity.TileEntityTypes;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
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

public class Microcontroller extends RedstoneAware implements PowerAcceptor, StateAware {
    public Microcontroller(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Microcontroller mcu) {
            return mcu.info.copyItemStack();
        }
        return ItemStack.EMPTY;
    }

    // ----------------------------------------------------------------------- //

    @Override
    protected void tooltipTail(ItemStack stack, @Nullable BlockGetter world, List<Component> tooltip, TooltipFlag advanced) {
        super.tooltipTail(stack, world, tooltip, advanced);
        if (li.cil.oc.util.Tooltip.showExtended()) {
            MicrocontrollerData info = new MicrocontrollerData(stack);
            for (ItemStack component : info.components) {
                if (!component.isEmpty()) {
                    tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }

    // ----------------------------------------------------------------------- //

    @Override
    public double energyThroughput() {
        return Settings.get().caseRate[Tier.One];
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileEntityTypes.MICROCONTROLLER.get().create(pos, state);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (Wrench.holdsApplicableWrench(player, pos)) return false;
        if (!player.isCrouching()) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Microcontroller mcu) {
                if (mcu.machine().isRunning()) mcu.machine().stop();
                else mcu.machine().start();
            }
            return true;
        }
        else if (Items.get(heldItem) != null && Items.get(heldItem) == Items.get(Constants.ItemName.EEPROM)) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Microcontroller mcu) {
                ItemStack newEeprom = player.getInventory().removeItem(player.getInventory().selected, 1);
                ItemStack oldEeprom = mcu.changeEEPROM(newEeprom);
                if (!oldEeprom.isEmpty()) {
                    InventoryUtils.addToPlayerInventory(oldEeprom, player);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Microcontroller tileEntity) {
            tileEntity.info.loadData(stack);
            tileEntity.snooperNode.changeBuffer(tileEntity.info.storedEnergy - tileEntity.snooperNode.localBuffer());
            // The block entity joined its network when it was added to the level (before this),
            // with the then still empty component list; re-create it and connect the components.
            tileEntity.resetComponents();
            if (tileEntity.machine().node().network() != null) tileEntity.connectComponents();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof li.cil.oc.common.tileentity.Microcontroller tileEntity) {
            builder = builder.withDynamicDrop(LootFunctions.DYN_ITEM_DATA, f -> {
                tileEntity.saveComponents();
                tileEntity.info.storedEnergy = (int) tileEntity.snooperNode.localBuffer();
                f.accept(tileEntity.info.createItemStack());
            });
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide && player.isCreative() && world.getBlockEntity(pos) instanceof li.cil.oc.common.tileentity.Microcontroller tileEntity) {
            Block.dropResources(state, world, pos, tileEntity, player, player.getMainHandItem());
        }
        super.playerWillDestroy(world, pos, state, player);
    }
}
