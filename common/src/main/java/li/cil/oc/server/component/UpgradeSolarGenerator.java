package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

public class UpgradeSolarGenerator extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public int ticksUntilCheck = 0;

    public boolean isSunShining = false;

    public UpgradeSolarGenerator(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withConnector().
            create());
    }

    @Override
    public Connector node() {
        return (Connector) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Solar panel",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Enligh10"
            );
        }
        return deviceInfo;
    }

    // ----------------------------------------------------------------------- //

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();

        ticksUntilCheck -= 1;
        if (ticksUntilCheck <= 0) {
            ticksUntilCheck = 100;
            isSunShining = isSunVisible();
        }
        if (isSunShining) {
            node().changeBuffer(Settings.get().solarGeneratorEfficiency);
        }
    }

    private boolean isSunVisible() {
        final BlockPos blockPos = BlockPosition.apply(host).offset(Direction.UP).toBlockPos();
        final Level world = host.world();
        return world.isDay() &&
            (world.dimension() != Level.NETHER) &&
            world.canSeeSkyFromBelowWater(blockPos) &&
            (world.getBiome(blockPos).value().getPrecipitationAt(blockPos) == Biome.Precipitation.NONE || (!world.isRaining() && !world.isThundering()));
    }
}
