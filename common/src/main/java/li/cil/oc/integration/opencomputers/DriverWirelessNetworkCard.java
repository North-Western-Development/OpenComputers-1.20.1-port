package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverWirelessNetworkCard implements Item {
    public static final DriverWirelessNetworkCard INSTANCE = new DriverWirelessNetworkCard();

    private DriverWirelessNetworkCard() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.WirelessNetworkCardTier1),
                Items.get(Constants.ItemName.WirelessNetworkCardTier2));
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        switch (tier(stack)) {
            case Tier.One:
                return new li.cil.oc.server.component.WirelessNetworkCard.Tier1(host);
            case Tier.Two:
                return new li.cil.oc.server.component.WirelessNetworkCard.Tier2(host);
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
        if (stack.getItem() instanceof li.cil.oc.common.item.WirelessNetworkCard item) return item.tier;
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverWirelessNetworkCard.INSTANCE.worksWith(stack)) {
                switch (DriverWirelessNetworkCard.INSTANCE.tier(stack)) {
                    case Tier.One:
                        return li.cil.oc.server.component.WirelessNetworkCard.Tier1.class;
                    case Tier.Two:
                        return li.cil.oc.server.component.WirelessNetworkCard.Tier2.class;
                    default:
                        return null;
                }
            } else return null;
        }
    }
}
