package li.cil.oc.integration.opencomputers;

import dev.architectury.utils.GameInstance;
import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * @author Vexatos
 */
public final class DriverUpgradeMF implements Item, HostAware {
    public static final DriverUpgradeMF INSTANCE = new DriverUpgradeMF();

    private DriverUpgradeMF() {
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                Items.get(Constants.ItemName.MFU));
    }

    @Override
    public boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && isAdapter(host);
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Upgrade;
    }

    @Override
    public int tier(ItemStack stack) {
        return Tier.Three;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.world() != null && !host.world().isClientSide) {
            if (stack.hasTag()) {
                final int[] coord = stack.getTag().getIntArray(Settings.namespace + "coord");
                if (coord.length == 4) {
                    final ResourceLocation dimension = new ResourceLocation(stack.getTag().getString(Settings.namespace + "dimension"));
                    final MinecraftServer server = GameInstance.getServer();
                    final ServerLevel world = server != null ? server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension)) : null;
                    if (world != null) {
                        return new li.cil.oc.server.component.UpgradeMF(host, BlockPosition.apply(coord[0], coord[1], coord[2], world), Direction.from3DDataValue(coord[3]));
                    }
                    // Invalid dimension ID
                }
                // Invalid tag
            }
        }
        return null;
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (DriverUpgradeMF.INSTANCE.worksWith(stack))
                return li.cil.oc.server.component.UpgradeMF.class;
            else return null;
        }
    }
}
