package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.event.BlockChangeHandler;
import li.cil.oc.common.event.BlockChangeHandler.ChangeListener;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedWorld;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Mostly stolen from {@link li.cil.oc.common.tileentity.Adapter}
 *
 * @author Sangar, Vexatos
 */
public class UpgradeMF extends AbstractManagedEnvironment implements ChangeListener, DeviceInfo {
    public final EnvironmentHost host;
    public final BlockPosition coord;
    public final Direction dir;

    private Optional<Environment> otherEnv = Optional.empty();
    private Optional<Pair<ManagedEnvironment, DriverBlock>> otherDrv = Optional.empty();
    private Optional<BlockData> blockData = Optional.empty();

    public UpgradeMF(EnvironmentHost host, BlockPosition coord, Direction dir) {
        this.host = host;
        this.coord = coord;
        this.dir = dir;
        setNode(Network.newNode(this, Visibility.None).
            withConnector().
            create());
    }

    @Override
    public Connector node() {
        return (Connector) super.node();
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Bus,
                DeviceAttribute.Description, "Remote Adapter",
                DeviceAttribute.Vendor, Constants.DeviceInfo.Scummtech,
                DeviceAttribute.Product, "ERR NAME NOT FOUND"
            );
        }
        return deviceInfo;
    }

    private void otherNode(BlockEntity tile, Consumer<Node> f) {
        li.cil.oc.server.network.Network.INSTANCE.getNetworkNode(tile, dir).ifPresent(f);
    }

    private double distanceToHost() {
        return coord.toVec3().distanceTo(new Vec3(host.xPosition(), host.yPosition(), host.zPosition()));
    }

    private void updateBoundState() {
        if (node() != null && node().network() != null && coord.world.map(w -> w.dimension() == host.world().dimension()).orElse(false)
            && distanceToHost() <= Settings.get().mfuRange) {
            final BlockEntity tile = ExtendedWorld.getBlockEntity(host.world(), coord);
            if (tile instanceof Environment env) {
                if (otherEnv.isPresent() && otherEnv.get() instanceof BlockEntity environment) {
                    otherNode(environment, node()::disconnect);
                    otherEnv = Optional.empty();
                }
                otherEnv = Optional.of(env);
                // Remove any driver that might be there.
                if (otherDrv.isPresent()) {
                    final ManagedEnvironment environment = otherDrv.get().getLeft();
                    node().disconnect(environment.node());
                    environment.saveData(blockData.get().data);
                    Optional.ofNullable(environment.node()).ifPresent(Node::remove);
                    otherDrv = Optional.empty();
                }
                otherNode(tile, node()::connect);
            }
            else {
                // Remove any environment that might have been there.
                if (otherEnv.isPresent() && otherEnv.get() instanceof BlockEntity environment) {
                    otherNode(environment, node()::disconnect);
                    otherEnv = Optional.empty();
                }
                final Level world = coord.world.get();
                final DriverBlock newDriver = Driver.driverFor(world, coord.toBlockPos(), dir);
                if (newDriver != null) {
                    if (otherDrv.isPresent()) {
                        final ManagedEnvironment oldEnvironment = otherDrv.get().getLeft();
                        final DriverBlock driver = otherDrv.get().getRight();
                        if (!newDriver.equals(driver)) { // Scala != is equals (CompoundBlockDriver is rebuilt per lookup)
                            // This is... odd. Maybe moved by some other mod? First, clean up.
                            otherDrv = Optional.empty();
                            blockData = Optional.empty();
                            node().disconnect(oldEnvironment.node());

                            // Then rebuild - if we have something.
                            final ManagedEnvironment environment = newDriver.createEnvironment(world, coord.toBlockPos(), dir);
                            if (environment != null) {
                                otherDrv = Optional.of(Pair.of(environment, newDriver));
                                blockData = Optional.of(new BlockData(environment.getClass().getName(), new CompoundTag()));
                                node().connect(environment.node());
                            }
                        } // else: the more things change, the more they stay the same.
                    }
                    else {
                        // A challenger appears. Maybe.
                        final ManagedEnvironment environment = newDriver.createEnvironment(world, coord.toBlockPos(), dir);
                        if (environment != null) {
                            otherDrv = Optional.of(Pair.of(environment, newDriver));
                            if (blockData.isPresent() && blockData.get().name.equals(environment.getClass().getName())) {
                                environment.loadData(blockData.get().data);
                            }
                            blockData = Optional.of(new BlockData(environment.getClass().getName(), new CompoundTag()));
                            node().connect(environment.node());
                        }
                    }
                }
                else if (otherDrv.isPresent()) {
                    // We had something there, but it's gone now...
                    final ManagedEnvironment environment = otherDrv.get().getLeft();
                    node().disconnect(environment.node());
                    environment.saveData(blockData.get().data);
                    Optional.ofNullable(environment.node()).ifPresent(Node::remove);
                    otherDrv = Optional.empty();
                }
                // else: Nothing before, nothing now.
            }
        }
    }

    private void disconnect() {
        if (otherEnv.isPresent() && otherEnv.get() instanceof BlockEntity environment) {
            otherNode(environment, node()::disconnect);
            otherEnv = Optional.empty();
        }
        if (otherDrv.isPresent()) {
            final ManagedEnvironment environment = otherDrv.get().getLeft();
            node().disconnect(environment.node());
            environment.saveData(blockData.get().data);
            Optional.ofNullable(environment.node()).ifPresent(Node::remove);
            otherDrv = Optional.empty();
        }
    }

    @Override
    public void onBlockChanged() {
        updateBoundState();
    }

    @Override
    public void update() {
        super.update();
        if (otherDrv.isPresent() && otherDrv.get().getLeft().canUpdate()) {
            otherDrv.get().getLeft().update();
        }
        if (host.world().getGameTime() % Settings.get().tickFrequency == 0) {
            if (!node().tryChangeBuffer(-Settings.get().mfuCost * Settings.get().tickFrequency * distanceToHost())) {
                disconnect();
            }
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            // Not checking for range yet because host may be a moving adapter, who knows?
            BlockChangeHandler.addListener(this, coord);

            updateBoundState();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (otherEnv.isPresent() && otherEnv.get() instanceof BlockEntity env) {
            otherNode(env, otherNode -> {
                if (node == otherNode) otherEnv = Optional.empty();
            });
        }
        if (otherDrv.isPresent() && node == otherDrv.get().getLeft().node()) {
            otherDrv = Optional.empty();
        }
        if (node == this.node()) {
            BlockChangeHandler.removeListener(this);
        }
    }

    @Override
    public void loadData(CompoundTag nbt) {
        super.loadData(nbt);
        final CompoundTag blockNbt = nbt.getCompound(Settings.namespace + "adapter.block");
        if (blockNbt.contains("name") && blockNbt.contains("data")) {
            blockData = Optional.of(new BlockData(blockNbt.getString("name"), blockNbt.getCompound("data")));
        }
    }

    @Override
    public void saveData(CompoundTag nbt) {
        super.saveData(nbt);
        final CompoundTag blockNbt = new CompoundTag();
        blockData.ifPresent(data -> {
            otherDrv.ifPresent(drv -> drv.getLeft().saveData(data.data));
            blockNbt.putString("name", data.name);
            blockNbt.put("data", data.data);
        });
        nbt.put(Settings.namespace + "adapter.block", blockNbt);
    }

    // ----------------------------------------------------------------------- //

    private static final class BlockData {
        final String name;
        final CompoundTag data;

        BlockData(String name, CompoundTag data) {
            this.name = name;
            this.data = data;
        }
    }
}
