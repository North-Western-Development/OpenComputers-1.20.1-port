package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.traits.Colored;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.Color;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// TODO(port): integration - the Immibis microblocks marker trait (traits.ImmibisMicroblock) was dropped.
public class Cable extends TileEntity implements Environment, NotAnalyzable, Colored {
    public final Node node = li.cil.oc.api.Network.newNode(this, Visibility.None).create();

    public Cable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setColor(Color.rgbValues.get(DyeColor.LIGHT_GRAY));
    }

    @Override
    public Node node() {
        return node;
    }

    public ItemStack createItemStack() {
        final ItemStack stack = li.cil.oc.api.Items.get(Constants.BlockName.Cable).createItemStack(1);
        if (getColor() != Color.rgbValues.get(DyeColor.LIGHT_GRAY)) {
            ItemColorizer.setColor(stack, getColor());
        }
        return stack;
    }

    public void fromItemStack(ItemStack stack) {
        if (ItemColorizer.hasColor(stack)) {
            setColor(ItemColorizer.getColor(stack));
        }
    }

    @Override
    public boolean controlsConnectivity() {
        return true;
    }

    @Override
    public boolean consumesDye() {
        return true;
    }

    @Override
    public void onColorChanged() {
        Colored.super.onColorChanged();
        if (getLevel() != null && isServer()) {
            li.cil.oc.api.Network.joinOrCreateNetwork(this);
        }
    }
}
