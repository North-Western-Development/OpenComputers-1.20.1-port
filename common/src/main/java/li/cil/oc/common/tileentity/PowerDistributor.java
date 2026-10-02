package li.cil.oc.common.tileentity;

import li.cil.oc.Settings;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.common.tileentity.traits.PowerBalancer;
import li.cil.oc.common.tileentity.traits.TileEntity;
import li.cil.oc.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class PowerDistributor extends TileEntity implements Environment, PowerBalancer, NotAnalyzable {
    public final Node node = null;

    private final Connector[] nodes = new Connector[6];

    private static final String ConnectorTag = Settings.namespace + "connector";

    public PowerDistributor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.None).
                withConnector(Settings.get().bufferDistributor).
                create();
        }
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public boolean isConnected() {
        for (Connector node : nodes) {
            if (node.address() != null && node.network() != null) return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------- //

    /** Client side only. */
    @Override
    public boolean canConnect(Direction side) {
        return true;
    }

    @Override
    public Connector sidedNode(Direction side) {
        return nodes[side.ordinal()];
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void loadForServer(CompoundTag nbt) {
        super.loadForServer(nbt);
        final CompoundTag[] tags = ExtendedNBT.toTagArray(nbt.getList(ConnectorTag, Tag.TAG_COMPOUND), CompoundTag.class);
        for (int index = 0; index < tags.length && index < nodes.length; index++) {
            nodes[index].loadData(tags[index]);
        }
    }

    @Override
    public void saveForServer(CompoundTag nbt) {
        super.saveForServer(nbt);
        // Side check for Waila (and other mods that may call this client side).
        if (isServer()) {
            final List<CompoundTag> tags = new ArrayList<>();
            for (Connector connector : nodes) {
                final CompoundTag connectorNbt = new CompoundTag();
                connector.saveData(connectorNbt);
                tags.add(connectorNbt);
            }
            ExtendedNBT.setNewTagList(nbt, ConnectorTag, tags);
        }
    }
}
