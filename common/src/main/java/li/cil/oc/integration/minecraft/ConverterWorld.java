package li.cil.oc.integration.minecraft;

import com.google.common.hash.Hashing;
import li.cil.oc.api.driver.Converter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public final class ConverterWorld implements Converter {
    public static final ConverterWorld INSTANCE = new ConverterWorld();

    private ConverterWorld() {
    }

    @Override
    @SuppressWarnings("deprecation")
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ServerLevel world) {
            output.put("id", UUID.nameUUIDFromBytes(Hashing.md5().newHasher().
                    putLong(world.getSeed()).
                    putString(world.dimension().location().toString(), StandardCharsets.UTF_8).
                    hash().asBytes()).toString());
        }

        if (value instanceof Level world) {
            output.put("name", world.dimension().location().toString());
        }
    }
}
