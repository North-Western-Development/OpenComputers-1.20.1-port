package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.world.item.ItemStack;

public final class DriverGraphicsCard implements Item, HostAware {
    public static final DriverGraphicsCard INSTANCE = new DriverGraphicsCard();

    private DriverGraphicsCard() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.GraphicsCardTier1),
                Items.get(Constants.ItemName.GraphicsCardTier2),
                Items.get(Constants.ItemName.GraphicsCardTier3));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return Item.super.worksWith(stack, host);
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && host.world().isClientSide) return null;
        switch (tier(stack)) {
            case Tier.One:
                return new li.cil.oc.server.component.GraphicsCard(Tier.One);
            case Tier.Two:
                return new li.cil.oc.server.component.GraphicsCard(Tier.Two);
            case Tier.Three:
                return new li.cil.oc.server.component.GraphicsCard(Tier.Three);
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
        if (stack.getItem() instanceof li.cil.oc.common.item.GraphicsCard gpu) return gpu.gpuTier();
        return Tier.One;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverGraphicsCard.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.GraphicsCard.class;
            else return null;
        }
    }
}
