package li.cil.oc.server.network;

import li.cil.oc.common.item.data.NodeData;
import net.minecraft.nbt.CompoundTag;

/**
 * Port note: Scala linearization is ComponentConnector, Connector, Component, Node,
 * so persistence runs Node, Component, then Connector logic.
 */
public interface ComponentConnector extends li.cil.oc.api.network.ComponentConnector, Component, Connector {
    @Override
    default void loadData(CompoundTag nbt) {
        Component.super.loadData(nbt);
        setLocalBuffer(nbt.getDouble(NodeData.BufferTag));
    }

    @Override
    default void saveData(CompoundTag nbt) {
        Component.super.saveData(nbt);
        nbt.putDouble(NodeData.BufferTag, Math.min(localBuffer(), localBufferSize()));
    }

    @Override
    default void onDisconnect(li.cil.oc.api.network.Node node) {
        Connector.super.onDisconnect(node);
    }
}
