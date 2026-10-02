package li.cil.oc.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class CompressedPacketBuilder extends PacketBuilderBase<DeflaterOutputStream> {
    public final PacketType packetType;

    private final ByteArrayOutputStream data;

    private final Deflater deflater;

    private byte[] finished;

    public CompressedPacketBuilder(PacketType packetType) {
        this(packetType, PacketBuilder.newData(true));
    }

    public CompressedPacketBuilder(PacketType packetType, ByteArrayOutputStream data) {
        this(packetType, data, new Deflater(Deflater.BEST_SPEED));
    }

    private CompressedPacketBuilder(PacketType packetType, ByteArrayOutputStream data, Deflater deflater) {
        super(new DeflaterOutputStream(data, deflater));
        this.packetType = packetType;
        this.data = data;
        this.deflater = deflater;
        writeByte(packetType.id());
    }

    @Override
    protected byte[] packet() {
        // May be sent more than once; the deflater can only be finished once.
        if (finished != null) return finished;
        flush();
        try {
            stream.finish();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // Release native zlib resources right away instead of waiting for GC.
        deflater.end();
        finished = data.toByteArray();
        return finished;
    }
}
