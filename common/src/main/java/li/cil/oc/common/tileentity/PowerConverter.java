package li.cil.oc.common.tileentity;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.tileentity.traits.Environment;
import li.cil.oc.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.common.tileentity.traits.PowerAcceptor;
import li.cil.oc.common.tileentity.traits.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;

public class PowerConverter extends TileEntity implements PowerAcceptor, Environment, NotAnalyzable, DeviceInfo {
    public final Connector node = li.cil.oc.api.Network.newNode(this, Visibility.None).
        withConnector(Settings.get().bufferConverter).
        create();

    private Map<String, String> deviceInfo;

    public PowerConverter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Power converter",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Transgizer-PX5",
                DeviceAttribute.Capacity, String.valueOf(energyThroughput())
            );
        }
        return deviceInfo;
    }

    @Override
    public boolean hasConnector(Direction side) {
        return true;
    }

    @Override
    public Optional<Connector> connector(Direction side) {
        return Optional.ofNullable(node);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().powerConverterRate;
    }
}
