package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.tileentity.traits.Rotatable;
import li.cil.oc.util.Rarity;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.ArrayUtils;

/**
 * BlockItem used for all OC blocks.
 * <p>
 * Note: the Scala version overrode Forge's getDamage/setDamage to expose the cable color as
 * damage value; cable colors are read via {@link li.cil.oc.util.ItemColorizer} directly now.
 */
public class Item extends BlockItem {
    public Item(Block value, Properties props) {
        super(value, props);
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        if (getBlock() instanceof Microcontroller) {
            MicrocontrollerData data = new MicrocontrollerData(stack);
            return Rarity.byTier(data.tier);
        }
        if (getBlock() instanceof RobotProxy) {
            RobotData data = new RobotData(stack);
            return Rarity.byTier(data.tier);
        }
        return super.getRarity(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemInfo info = Items.get(stack);
        if (info != null && info == Items.get(Constants.BlockName.Print)) {
            PrintData data = new PrintData(stack);
            if (data.label.isPresent()) return Component.literal(data.label.get());
        }
        return super.getName(stack);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext ctx, BlockState newState) {
        // When placing robots in creative mode, we have to copy the stack
        // manually before it's placed to ensure different component addresses
        // in the different robots, to avoid interference of screens e.g.
        ItemInfo info = Items.get(ctx.getItemInHand());
        boolean needsCopying = ctx.getPlayer() != null && ctx.getPlayer().isCreative() && info != null && info == Items.get(Constants.BlockName.Robot);
        BlockPlaceContext ctxToUse = ctx;
        if (needsCopying) {
            ItemStack stackToUse = new RobotData(ctx.getItemInHand()).copyItemStack();
            BlockHitResult hitResult = new BlockHitResult(ctx.getClickLocation(), ctx.getClickedFace(), ctx.getClickedPos(), ctx.isInside());
            ctxToUse = new BlockPlaceContext(ctx.getLevel(), ctx.getPlayer(), ctx.getHand(), stackToUse, hitResult) {
            };
        }
        if (super.placeBlock(ctxToUse, newState)) {
            // If it's a rotatable block try to make it face the player.
            BlockEntity tileEntity = ctx.getLevel().getBlockEntity(ctxToUse.getClickedPos());
            if (tileEntity instanceof li.cil.oc.common.tileentity.Keyboard) {
                // Ignore.
            }
            else if (tileEntity instanceof Rotatable rotatable && ctxToUse.getPlayer() != null) {
                rotatable.setFromEntityPitchAndYaw(ctxToUse.getPlayer());
                Direction[] validFacings = rotatable.validFacings();
                if (!ArrayUtils.contains(validFacings, rotatable.pitch())) {
                    rotatable.setPitch(validFacings.length > 0 ? validFacings[0] : Direction.NORTH);
                }
                if (!(rotatable instanceof li.cil.oc.common.tileentity.RobotProxy)) {
                    rotatable.invertRotation();
                }
            }
            return true;
        }
        return false;
    }
}
