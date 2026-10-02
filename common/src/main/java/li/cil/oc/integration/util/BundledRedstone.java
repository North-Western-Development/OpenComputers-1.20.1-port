package li.cil.oc.integration.util;

import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class BundledRedstone {
    private BundledRedstone() {
    }

    public static final List<RedstoneProvider> providers = new ArrayList<>();

    public static void addProvider(RedstoneProvider provider) {
        providers.add(provider);
    }

    public static boolean isAvailable() {
        return !providers.isEmpty();
    }

    public static int computeInput(BlockPosition pos, Direction side) {
        if (ExtendedWorld.blockExists(pos.world.get(), pos.offset(side))) {
            int max = 0;
            for (RedstoneProvider provider : providers) {
                max = Math.max(max, provider.computeInput(pos, side));
            }
            return max;
        }
        return 0;
    }

    public static int[] computeBundledInput(BlockPosition pos, Direction side) {
        if (ExtendedWorld.blockExists(pos.world.get(), pos.offset(side))) {
            int[] result = null;
            for (RedstoneProvider provider : providers) {
                final int[] input = provider.computeBundledInput(pos, side);
                if (input == null) continue;
                if (result == null) {
                    result = input.clone();
                } else {
                    // Equivalent of Scala's lazyZip: truncate to the shorter array.
                    final int[] merged = new int[Math.min(result.length, input.length)];
                    for (int i = 0; i < merged.length; i++) {
                        merged[i] = Math.max(result[i], input[i]);
                    }
                    result = merged;
                }
            }
            return result;
        }
        return null;
    }

    public interface RedstoneProvider {
        int computeInput(BlockPosition pos, Direction side);

        int[] computeBundledInput(BlockPosition pos, Direction side);
    }
}
