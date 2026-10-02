package li.cil.oc.common.item.traits;

import li.cil.oc.Settings;
import li.cil.oc.util.PackedColor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Implementers override {@code SimpleItem.tooltipData()} and return {@link #gpuTooltipData()}.
 */
public interface GPULike {
    int gpuTier();

    default List<Object> gpuTooltipData() {
        final Pair<Integer, Integer> res = Settings.screenResolutionsByTier[gpuTier()];
        final int depth = PackedColor.Depth.bits(Settings.screenDepthsByTier[gpuTier()]);
        final String costs;
        switch (gpuTier()) {
            case 0:
                costs = "1/1/4/2/2";
                break;
            case 1:
                costs = "2/4/8/4/4";
                break;
            case 2:
                costs = "4/8/16/8/8";
                break;
            default:
                throw new IllegalStateException("Invalid GPU tier " + gpuTier());
        }
        return Arrays.asList(res.getLeft(), res.getRight(), depth, costs);
    }
}
