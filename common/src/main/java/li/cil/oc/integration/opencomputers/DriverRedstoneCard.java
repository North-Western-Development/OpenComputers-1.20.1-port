package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware;
import li.cil.oc.common.tileentity.traits.RedstoneAware;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.integration.util.WirelessRedstone;
import li.cil.oc.server.component.Redstone;
import net.minecraft.world.item.ItemStack;

public final class DriverRedstoneCard implements Item, HostAware {
    public static final DriverRedstoneCard INSTANCE = new DriverRedstoneCard();

    private DriverRedstoneCard() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.RedstoneCardTier1),
                Items.get(Constants.ItemName.RedstoneCardTier2));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        final boolean isAdvanced = tier(stack) == Tier.Two;
        final boolean hasBundled = BundledRedstone.isAvailable() && isAdvanced;
        final boolean hasWireless = WirelessRedstone.isAvailable() && isAdvanced;
        if (host instanceof BundledRedstoneAware && hasBundled) {
            final var redstone = (EnvironmentHost & BundledRedstoneAware) host;
            if (hasWireless) return new Redstone.BundledWireless(redstone);
            else return new Redstone.Bundled(redstone);
        } else if (host instanceof RedstoneAware) {
            final var redstone = (EnvironmentHost & RedstoneAware) host;
            if (hasWireless) return new Redstone.VanillaWireless(redstone);
            else return new Redstone.Vanilla(redstone);
        } else {
            if (hasWireless) return new Redstone.Wireless(host);
            else return null;
        }
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Card;
    }

    @Override
    public int tier(ItemStack stack) {
        if (stack.getItem() instanceof li.cil.oc.common.item.RedstoneCard item) return item.tier;
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverRedstoneCard.INSTANCE.worksWith(stack)) {
                final boolean isAdvanced = DriverRedstoneCard.INSTANCE.tier(stack) == Tier.Two;
                final boolean hasBundled = BundledRedstone.isAvailable() && isAdvanced;
                final boolean hasWireless = WirelessRedstone.isAvailable() && isAdvanced;
                if (hasBundled) {
                    if (hasWireless) return Redstone.BundledWireless.class;
                    else return Redstone.Bundled.class;
                } else {
                    return Redstone.Vanilla.class;
                }
            } else return null;
        }
    }
}
