package li.cil.oc.common;

import java.io.ByteArrayOutputStream;

public class SimplePacketBuilder extends PacketBuilderBase<ByteArrayOutputStream> {
    public final PacketType packetType;

    public SimplePacketBuilder(PacketType packetType) {
        super(PacketBuilder.newData(false));
        this.packetType = packetType;
        writeByte(packetType.id());
    }

    @Override
    protected byte[] packet() {
        flush();
        return stream.toByteArray();
    }
}
