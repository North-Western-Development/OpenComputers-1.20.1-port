package li.cil.oc.common.container;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Slot background icon locations. Lives in common code (not in
 * {@code li.cil.oc.client.Textures}) because container slots are created on the
 * dedicated server too and must not load client classes.
 */
public final class SlotIcons {
    private SlotIcons() {
    }

    private static final Map<String, ResourceLocation> ForSlotType = new HashMap<>();
    private static final Map<Integer, ResourceLocation> ForTier = new HashMap<>();

    static {
        for (String name : Slot.All) {
            ForSlotType.put(name, L(name));
        }
        ForTier.put(Tier.None, L("na"));
        for (int tier = Tier.One; tier <= Tier.Three; tier++) {
            ForTier.put(tier, L("tier" + tier));
        }
    }

    public static ResourceLocation get(String slotType) {
        return ForSlotType.get(slotType);
    }

    public static ResourceLocation get(int tier) {
        return ForTier.get(tier);
    }

    private static ResourceLocation L(String name) {
        return new ResourceLocation(OpenComputers.ID, "textures/icons/" + name + ".png");
    }
}
