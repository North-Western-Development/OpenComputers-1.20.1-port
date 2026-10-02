package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs;
import li.cil.oc.common.tileentity.traits.Tickable;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.server.component.RedstoneVanilla;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Redstone extends TileEntity implements Environment, BundledRedstoneAware, Tickable {
    public final RedstoneVanilla instance;
    public final Component node;
    public final Node dummyNode;

    private static final String RedstoneTag = Settings.namespace + "redstone";

    public Redstone(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        instance = BundledRedstone.isAvailable()
            ? new li.cil.oc.server.component.Redstone.Bundled(this)
            : new li.cil.oc.server.component.Redstone.Vanilla(this);
        instance.wakeNeighborsOnly = false;
        node = (Component) instance.node();
        if (node != null) {
            node.setVisibility(Visibility.Network);
            redstoneAwareState().isOutputEnabled = true;
            dummyNode = li.cil.oc.api.Network.newNode(this, Visibility.None).create();
        } else {
            dummyNode = null;
        }
    }

    @Override
    public Node node() {
        return node;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        instance.loadData(nbt.getCompound(RedstoneTag));
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        ExtendedNBT.setNewCompoundTag(nbt, RedstoneTag, instance::saveData);
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        BundledRedstoneAware.super.onRedstoneInputChanged(args);
        if (node != null && node.network() != null) {
            node.connect(dummyNode);
            dummyNode.sendToNeighbors("redstone.changed", args);
        }
    }
}
