package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.event.ChunkloaderUpgradeHandler;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chunk loading itself (vanilla chunk tickets owned by this upgrade's address plus
 * OC-side bookkeeping) lives in {@link ChunkloaderUpgradeHandler}; this class only keeps
 * track of the current ticket (the center chunk) in {@link #ticket}.
 */
public class UpgradeChunkloader extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public Optional<ChunkPos> ticket = Optional.empty();

    public UpgradeChunkloader(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).
            withComponent("chunkloader").
            withConnector().
            create());
    }

    @Override
    public ComponentConnector node() {
        return (ComponentConnector) super.node();
    }

    private Map<String, String> deviceInfo;

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "World stabilizer",
                DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
                DeviceAttribute.Product, "Realizer9001-CL"
            );
        }
        return deviceInfo;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (host.world().getGameTime() % Settings.get().tickFrequency == 0 && ticket.isPresent()) {
            if (!node().tryChangeBuffer(-Settings.get().chunkloaderCost * Settings.get().tickFrequency)) {
                if (host.world() instanceof ServerLevel world) {
                    ticket.ifPresent(pos -> ChunkloaderUpgradeHandler.releaseTicket(world, node().address(), pos));
                }
                ticket = Optional.empty();
            }
            else if (host instanceof Entity) // Robot move events are not fired for entities (drones)
                ChunkloaderUpgradeHandler.updateLoadedChunk(this);
        }
    }

    @Callback(doc = "function():boolean -- Gets whether the chunkloader is currently active.")
    public Object[] isActive(Context context, Arguments args) {
        return ResultWrapper.result(ticket.isPresent());
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Enables or disables the chunkloader, returns true if active changed")
    public Object[] setActive(Context context, Arguments args) throws Exception {
        return ResultWrapper.result(setActive(args.checkBoolean(0), true));
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node()) {
            final Optional<ChunkPos> restoredTicket = ChunkloaderUpgradeHandler.claimTicket(node.address());
            if (restoredTicket.isPresent()) {
                if (!isDimensionAllowed()) {
                    if (host.world() instanceof ServerLevel world) {
                        ChunkloaderUpgradeHandler.releaseTicket(world, node.address(), restoredTicket.get());
                    }
                    OpenComputers.log.info("Releasing chunk loader ticket at (" + host.xPosition() + ", " + host.yPosition() + ", " + host.zPosition() + ") in blacklisted dimension " + host.world().dimension() + ".");
                }
                else {
                    OpenComputers.log.info("Reclaiming chunk loader ticket at (" + host.xPosition() + ", " + host.yPosition() + ", " + host.zPosition() + ") in dimension " + host.world().dimension() + ".");
                    ticket = restoredTicket;
                    ChunkloaderUpgradeHandler.updateLoadedChunk(this);
                }
            }
            else if (host instanceof Context context && context.isRunning()) {
                try {
                    requestTicket(false);
                } catch (Exception ignored) {
                    // Cannot happen, we don't throw if blocked.
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node()) {
            // Since 1.17 the server unloads all chunks before the final save when it shuts down,
            // which disconnects us; keep the persisted ticket then so it is restored on the next
            // start (like Forge's persisted tickets were).
            if (host.world() instanceof ServerLevel world && world.getServer().isRunning()) {
                ticket.ifPresent(pos -> ChunkloaderUpgradeHandler.releaseTicket(world, node.address(), pos));
            }
            ticket = Optional.empty();
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        try {
            if ("computer.stopped".equals(message.name())) {
                setActive(false, false);
            }
            else if ("computer.started".equals(message.name())) {
                setActive(true, false);
            }
        } catch (Exception ignored) {
            // Cannot happen, we don't throw if blocked.
        }
    }

    private boolean setActive(boolean enabled, boolean throwIfBlocked) throws Exception {
        if (enabled && ticket.isEmpty()) {
            requestTicket(throwIfBlocked);
            return ticket.isPresent();
        }
        else if (!enabled && ticket.isPresent()) {
            if (host.world() instanceof ServerLevel world) {
                ticket.ifPresent(pos -> ChunkloaderUpgradeHandler.releaseTicket(world, node().address(), pos));
            }
            ticket = Optional.empty();
            return true;
        }
        else {
            return false;
        }
    }

    @Deprecated
    private boolean isDimensionAllowed() {
        final ResourceKey<Level> dimension = host.world().dimension();
        final int id;
        if (dimension == Level.OVERWORLD) id = 0;
        else if (dimension == Level.NETHER) id = -1;
        else if (dimension == Level.END) id = 1;
        else {
            // Numeric dimension ids do not exist for modded dimensions anymore: they are allowed
            // unless there is a whitelist (which can only name the vanilla dimensions).
            return Settings.get().chunkloadDimensionWhitelist.isEmpty();
        }
        final List<Integer> whitelist = Settings.get().chunkloadDimensionWhitelist;
        final List<Integer> blacklist = Settings.get().chunkloadDimensionBlacklist;
        if (!whitelist.isEmpty()) {
            if (!whitelist.contains(id))
                return false;
        }
        if (!blacklist.isEmpty()) {
            if (blacklist.contains(id)) {
                return false;
            }
        }
        return true;
    }

    private void requestTicket(boolean throwIfBlocked) throws Exception {
        if (!isDimensionAllowed()) {
            if (throwIfBlocked) {
                throw new Exception("this dimension is blacklisted");
            }
        }
        else {
            // This ticket is a lie, but ChunkloaderUpgradeHandler won't crash or load it.
            ticket = Optional.of(new ChunkPos(0, 0));
            ChunkloaderUpgradeHandler.updateLoadedChunk(this);
        }
    }
}
