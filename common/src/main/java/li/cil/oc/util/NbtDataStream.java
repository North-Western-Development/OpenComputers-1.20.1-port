package li.cil.oc.util;

import net.minecraft.nbt.CompoundTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class NbtDataStream {
    private NbtDataStream() {
    }

    public static boolean getShortArray(CompoundTag nbt, String key, short[][] array2d, int w, int h) {
        if (!nbt.contains(key)) {
            return false;
        }

        final ByteArrayInputStream rawByteReader = new ByteArrayInputStream(nbt.getByteArray(key));
        final DataInputStream memReader = new DataInputStream(rawByteReader);
        try {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (2 > memReader.available()) {
                        return true; // not great, but get out now
                    }
                    array2d[y][x] = memReader.readShort();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return true;
    }

    public static boolean getIntArrayLegacy(CompoundTag nbt, String key, short[][] array2d, int w, int h) {
        if (!nbt.contains(key)) {
            return false;
        }
        // legacy format
        final int[] c = nbt.getIntArray(key);
        for (int y = 0; y < h; y++) {
            final short[] rowColor = array2d[y];
            for (int x = 0; x < w; x++) {
                final int index = x + y * w;
                if (index >= c.length) {
                    return true; // not great, but, the read at least started
                }
                rowColor[x] = (short) c[index];
            }
        }
        return true;
    }

    public static void setShortArray(CompoundTag nbt, String key, short[] array) {
        final ByteArrayOutputStream rawByteWriter = new ByteArrayOutputStream();
        final DataOutputStream memWriter = new DataOutputStream(rawByteWriter);
        try {
            for (short value : array) {
                memWriter.writeShort(value);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        nbt.putByteArray(key, rawByteWriter.toByteArray());
    }

    public static boolean getOptBoolean(CompoundTag nbt, String key, boolean df) {
        return nbt.contains(key) ? nbt.getBoolean(key) : df;
    }

    public static String getOptString(CompoundTag nbt, String key, String df) {
        return nbt.contains(key) ? nbt.getString(key) : df;
    }

    public static CompoundTag getOptNbt(CompoundTag nbt, String key) {
        return nbt.contains(key) ? nbt.getCompound(key) : new CompoundTag();
    }

    public static int getOptInt(CompoundTag nbt, String key, int df) {
        return nbt.contains(key) ? nbt.getInt(key) : df;
    }
}
