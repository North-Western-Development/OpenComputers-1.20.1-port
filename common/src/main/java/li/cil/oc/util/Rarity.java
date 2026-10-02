package li.cil.oc.util;

public final class Rarity {
    private Rarity() {
    }

    private static final net.minecraft.world.item.Rarity[] lookup = {
            net.minecraft.world.item.Rarity.COMMON,
            net.minecraft.world.item.Rarity.UNCOMMON,
            net.minecraft.world.item.Rarity.RARE,
            net.minecraft.world.item.Rarity.EPIC};

    public static net.minecraft.world.item.Rarity byTier(int tier) {
        return lookup[Math.min(Math.max(tier, 0), lookup.length - 1)];
    }
}
