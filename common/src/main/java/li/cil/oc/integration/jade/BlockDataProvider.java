package li.cil.oc.integration.jade;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.Assembler;
import li.cil.oc.common.tileentity.Charger;
import li.cil.oc.common.tileentity.DiskDrive;
import li.cil.oc.common.tileentity.Hologram;
import li.cil.oc.common.tileentity.Keyboard;
import li.cil.oc.common.tileentity.Rack;
import li.cil.oc.common.tileentity.Relay;
import li.cil.oc.common.tileentity.Screen;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server side of the former WAILA {@code BlockDataProvider}: collects node address, energy buffer
 * and component name (plus some block specific info) into the tag sent to the client, where
 * {@link BlockComponentProvider} renders it. Safe to load on a dedicated server.
 */
public final class BlockDataProvider implements IServerDataProvider<BlockAccessor> {
    public static final BlockDataProvider INSTANCE = new BlockDataProvider();

    private BlockDataProvider() {
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.Uid;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        final BlockEntity tileEntity = accessor.getBlockEntity();
        if (tileEntity == null) return;

        if (tileEntity instanceof li.cil.oc.api.network.SidedEnvironment te) {
            final ListTag nodes = new ListTag();
            for (Direction side : Direction.values()) {
                nodes.add(writeNode(tileEntity, te.sidedNode(side), new CompoundTag()));
            }
            tag.put("nodes", nodes);
        } else if (tileEntity instanceof li.cil.oc.api.network.Environment te) {
            writeNode(tileEntity, te.node(), tag);
        }

        if (tileEntity instanceof Relay te) {
            tag.putDouble("signalStrength", te.strength);
            // This might be called before the components have finished loading, thus the addresses may be null.
            final ListTag addresses = new ListTag();
            for (Component node : te.componentNodes) {
                if (node != null && node.address() != null) addresses.add(StringTag.valueOf(node.address()));
            }
            tag.put("addresses", addresses);
        } else if (tileEntity instanceof Assembler te) {
            ignoreSidedness(tileEntity, tag, te.node());
            if (te.isAssembling()) {
                tag.putDouble("progress", te.progress());
                tag.putInt("timeRemaining", te.timeRemaining());
                if (!te.output.isEmpty()) {
                    tag.putString("output", te.output.getDescriptionId());
                }
            }
        } else if (tileEntity instanceof Charger te) {
            tag.putDouble("chargeSpeed", te.chargeSpeed);
        } else if (tileEntity instanceof DiskDrive te) {
            // Override address with file system address.
            tag.remove("address");
            te.filesystemNode().ifPresent(node -> writeNode(tileEntity, node, tag));
        } else if (tileEntity instanceof Hologram te) {
            ignoreSidedness(tileEntity, tag, te.node());
        } else if (tileEntity instanceof Keyboard te) {
            ignoreSidedness(tileEntity, tag, te.node());
        } else if (tileEntity instanceof Screen te) {
            ignoreSidedness(tileEntity, tag, te.node());
        } else if (tileEntity instanceof Rack) {
            tag.remove("nodes");
            // TODO(port): per-server addresses for racks were already disabled in the 1.16.5 version.
        }
    }

    /** Override sided info (show info on all sides). */
    private static void ignoreSidedness(BlockEntity tileEntity, CompoundTag tag, Node node) {
        tag.remove("nodes");
        final CompoundTag nodeTag = writeNode(tileEntity, node, new CompoundTag());
        final ListTag nodes = new ListTag();
        for (Direction ignored : Direction.values()) {
            nodes.add(nodeTag.copy());
        }
        tag.put("nodes", nodes);
    }

    private static CompoundTag writeNode(BlockEntity tileEntity, Node node, CompoundTag tag) {
        if (node != null && node.reachability() != Visibility.None && !(tileEntity instanceof NotAnalyzable)) {
            if (node.address() != null) {
                tag.putString("address", node.address());
            }
            if (node instanceof Connector connector) {
                tag.putInt("buffer", (int) connector.localBuffer());
                tag.putInt("bufferSize", (int) connector.localBufferSize());
            }
            if (node instanceof Component component) {
                tag.putString("componentName", component.name());
            }
        }
        return tag;
    }
}
