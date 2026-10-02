package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverDataCard implements Item {
    public static final DriverDataCard INSTANCE = new DriverDataCard();

    private DriverDataCard() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.DataCardTier1),
                Items.get(Constants.ItemName.DataCardTier2),
                Items.get(Constants.ItemName.DataCardTier3));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        switch (tier(stack)) {
            case Tier.One:
                return new li.cil.oc.server.component.DataCard.Tier1();
            case Tier.Two:
                return new li.cil.oc.server.component.DataCard.Tier2();
            case Tier.Three:
                return new li.cil.oc.server.component.DataCard.Tier3();
            default:
                return null;
        }
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.DataCard item) return item.tier;
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverDataCard.INSTANCE.worksWith(stack)) {
                switch (DriverDataCard.INSTANCE.tier(stack)) {
                    case Tier.One:
                        return li.cil.oc.server.component.DataCard.Tier1.class;
                    case Tier.Two:
                        return li.cil.oc.server.component.DataCard.Tier2.class;
                    case Tier.Three:
                        return li.cil.oc.server.component.DataCard.Tier3.class;
                    default:
                        return null;
                }
            } else return null;
        }
    }
}
